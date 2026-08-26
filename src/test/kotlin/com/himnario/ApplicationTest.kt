package com.himnario

import com.himnario.common.health.HealthCheck
import com.himnario.config.AppConfig
import com.himnario.config.DatabaseSettings
import com.himnario.support.FakeHymnRepository
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun `health responds 200`() = withTestApplication {
        val response = client.get("/api/v1/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "Himnario API is healthy")
    }

    @Test
    fun `hymn list responds with bounded pagination`() = withTestApplication {
        val response = client.get("/api/v1/hymns")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "\"totalItems\":0")

        val oversized = client.get("/api/v1/hymns?size=101")
        assertEquals(HttpStatusCode.UnprocessableEntity, oversized.status)
        assertContains(oversized.bodyAsText(), "INVALID_PAGE_SIZE")
    }

    @Test
    fun `valid hymn can be created and retrieved`() = withTestApplication {
        val created = client.post("/api/v1/hymns") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"title":"Santo Espíritu","bpm":72,"tempo":"SLOW","status":"ACTIVE"}""",
            )
        }

        assertEquals(HttpStatusCode.Created, created.status)
        val createdJson = Json.parseToJsonElement(created.bodyAsText()).jsonObject
        val data = createdJson.getValue("data").jsonObject
        assertEquals("santo-espiritu", data.getValue("slug").jsonPrimitive.content)

        val id = data.getValue("id").jsonPrimitive.content
        val retrieved = client.get("/api/v1/hymns/$id")
        assertEquals(HttpStatusCode.OK, retrieved.status)
        assertContains(retrieved.bodyAsText(), "Santo Espíritu")
    }

    @Test
    fun `duplicate titles receive deterministic slug suffixes`() = withTestApplication {
        repeat(2) {
            val response = client.post("/api/v1/hymns") {
                contentType(ContentType.Application.Json)
                setBody("""{"title":"Santo Espíritu"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            if (it == 1) assertContains(response.bodyAsText(), "santo-espiritu-2")
        }
    }

    @Test
    fun `empty title returns 422`() = withTestApplication {
        val response = client.post("/api/v1/hymns") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"   "}""")
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertContains(response.bodyAsText(), "INVALID_TITLE")
    }

    @Test
    fun `non-positive bpm returns 422`() = withTestApplication {
        val response = client.post("/api/v1/hymns") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Test","bpm":0}""")
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertContains(response.bodyAsText(), "INVALID_BPM")
    }

    @Test
    fun `unknown hymn returns 404`() = withTestApplication {
        val response = client.get("/api/v1/hymns/30c7c499-f4c5-4bef-a3ca-3563d174d916")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertContains(response.bodyAsText(), "HYMN_NOT_FOUND")
    }

    @Test
    fun `hymn can be updated and archived`() = withTestApplication {
        val created = client.post("/api/v1/hymns") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Original"}""")
        }
        val id = Json.parseToJsonElement(created.bodyAsText())
            .jsonObject.getValue("data").jsonObject.getValue("id").jsonPrimitive.content

        val updated = client.put("/api/v1/hymns/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Actualizado","bpm":90}""")
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        assertContains(updated.bodyAsText(), "actualizado")

        val archived = client.patch("/api/v1/hymns/$id/archive")
        assertEquals(HttpStatusCode.OK, archived.status)
        assertContains(archived.bodyAsText(), "ARCHIVED")
    }

    private fun withTestApplication(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureApplication(
                    appConfig = TEST_CONFIG,
                    hymnRepository = FakeHymnRepository(),
                    databaseHealthCheck = HealthCheck { true },
                )
            }
            block()
        }

    private companion object {
        val TEST_CONFIG = AppConfig(
            database = DatabaseSettings(
                host = "localhost",
                port = 5434,
                name = "test",
                user = "test",
                password = "test",
                maxPoolSize = 1,
            ),
            corsAllowedHosts = listOf("localhost:8081"),
        )
    }
}

package com.himnario.features.hymns

import com.himnario.common.dto.PaginationResponse
import com.himnario.common.exceptions.BadRequestException
import com.himnario.common.response.ApiSuccessResponse
import com.himnario.features.hymns.dto.CreateHymnRequest
import com.himnario.features.hymns.dto.HymnListResponse
import com.himnario.features.hymns.dto.UpdateHymnRequest
import com.himnario.features.hymns.dto.toResponse
import com.himnario.features.hymns.model.HymnFilters
import com.himnario.features.hymns.model.HymnStatus
import com.himnario.features.hymns.model.HymnTempo
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import java.util.UUID

class HymnController(private val service: HymnService) {
    suspend fun list(call: ApplicationCall) {
        val page = call.queryInt("page", DEFAULT_PAGE)
        val size = call.queryInt("size", DEFAULT_PAGE_SIZE)
        val filters = HymnFilters(
            page = page,
            size = size,
            query = call.request.queryParameters["q"]?.trim()?.takeIf(String::isNotEmpty),
            status = call.request.queryParameters["status"]?.let { value ->
                parseEnum<HymnStatus>("status", value)
            },
            tempo = call.request.queryParameters["tempo"]?.let { value ->
                parseEnum<HymnTempo>("tempo", value)
            },
            musicalKey = call.request.queryParameters["key"]?.trim()?.takeIf(String::isNotEmpty),
        )
        val result = service.list(filters)
        val totalPages = if (result.totalItems == 0L) {
            0L
        } else {
            (result.totalItems + size - 1) / size
        }

        call.respond(
            ApiSuccessResponse(
                data = HymnListResponse(
                    items = result.items.map { it.toResponse() },
                    pagination = PaginationResponse(
                        page = page,
                        size = size,
                        totalItems = result.totalItems,
                        totalPages = totalPages,
                    ),
                ),
            ),
        )
    }

    suspend fun get(call: ApplicationCall) {
        call.respond(ApiSuccessResponse(data = service.get(call.hymnId()).toResponse()))
    }

    suspend fun create(call: ApplicationCall) {
        val request = call.receive<CreateHymnRequest>()
        val created = service.create(request)
        call.respond(HttpStatusCode.Created, ApiSuccessResponse(data = created.toResponse()))
    }

    suspend fun update(call: ApplicationCall) {
        val request = call.receive<UpdateHymnRequest>()
        val updated = service.update(call.hymnId(), request)
        call.respond(ApiSuccessResponse(data = updated.toResponse()))
    }

    suspend fun archive(call: ApplicationCall) {
        val archived = service.archive(call.hymnId())
        call.respond(ApiSuccessResponse(data = archived.toResponse()))
    }

    private fun ApplicationCall.hymnId(): UUID {
        val rawId = parameters["id"]
            ?: throw BadRequestException("INVALID_HYMN_ID", "Hymn id is required")
        return runCatching { UUID.fromString(rawId) }.getOrElse {
            throw BadRequestException("INVALID_HYMN_ID", "Hymn id must be a valid UUID")
        }
    }

    private fun ApplicationCall.queryInt(name: String, default: Int): Int {
        val rawValue = request.queryParameters[name] ?: return default
        return rawValue.toIntOrNull()
            ?: throw BadRequestException("INVALID_QUERY_PARAMETER", "$name must be an integer")
    }

    private inline fun <reified T : Enum<T>> parseEnum(name: String, value: String): T =
        enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw BadRequestException(
                "INVALID_QUERY_PARAMETER",
                "$name has an unsupported value",
            )

    private companion object {
        const val DEFAULT_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 20
    }
}


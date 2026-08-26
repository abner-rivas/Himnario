package com.himnario.common.health

import com.himnario.common.response.ApiError
import com.himnario.common.response.ApiErrorResponse
import com.himnario.common.response.HealthResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

class HealthController(private val databaseHealthCheck: HealthCheck) {
    suspend fun live(call: ApplicationCall) {
        call.respond(HealthResponse(message = "Himnario API is healthy"))
    }

    suspend fun ready(call: ApplicationCall) {
        if (databaseHealthCheck.isHealthy()) {
            call.respond(HealthResponse(message = "Himnario API is ready"))
            return
        }

        call.respond(
            HttpStatusCode.ServiceUnavailable,
            ApiErrorResponse(
                error = ApiError(
                    code = "DATABASE_UNAVAILABLE",
                    message = "Database connection is unavailable",
                ),
            ),
        )
    }
}


package com.himnario.plugins

import com.himnario.common.exceptions.BadRequestException
import com.himnario.common.exceptions.ResourceConflictException
import com.himnario.common.exceptions.ResourceNotFoundException
import com.himnario.common.exceptions.ValidationException
import com.himnario.common.response.ApiError
import com.himnario.common.response.ApiErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respondError(HttpStatusCode.BadRequest, cause.code, cause.message)
        }
        exception<ResourceNotFoundException> { call, cause ->
            call.respondError(HttpStatusCode.NotFound, cause.code, cause.message)
        }
        exception<ResourceConflictException> { call, cause ->
            call.respondError(HttpStatusCode.Conflict, cause.code, cause.message)
        }
        exception<ValidationException> { call, cause ->
            call.respondError(HttpStatusCode.UnprocessableEntity, cause.code, cause.message)
        }
        exception<ContentTransformationException> { call, _ ->
            call.respondError(HttpStatusCode.BadRequest, "INVALID_REQUEST", "Request body is invalid")
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled request failure", cause)
            call.respondError(
                HttpStatusCode.InternalServerError,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondError(status, "ROUTE_NOT_FOUND", "Route not found")
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String,
) {
    respond(status, ApiErrorResponse(error = ApiError(code = code, message = message)))
}

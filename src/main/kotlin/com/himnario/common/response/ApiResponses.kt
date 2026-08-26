package com.himnario.common.response

import kotlinx.serialization.Serializable

@Serializable
data class ApiSuccessResponse<T>(
    val success: Boolean = true,
    val data: T,
)

@Serializable
data class HealthResponse(
    val success: Boolean = true,
    val message: String,
)

@Serializable
data class ApiErrorResponse(
    val success: Boolean = false,
    val error: ApiError,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)


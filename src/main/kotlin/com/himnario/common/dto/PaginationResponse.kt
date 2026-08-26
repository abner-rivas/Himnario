package com.himnario.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginationResponse(
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Long,
)


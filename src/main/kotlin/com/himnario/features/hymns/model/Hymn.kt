package com.himnario.features.hymns.model

import java.time.OffsetDateTime
import java.util.UUID

data class Hymn(
    val id: UUID,
    val title: String,
    val slug: String,
    val description: String?,
    val lyrics: String?,
    val musicalKey: String?,
    val musicalMode: HymnMusicalMode?,
    val bpm: Int?,
    val tempo: HymnTempo?,
    val status: HymnStatus,
    val version: Int,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val deletedAt: OffsetDateTime?,
)

data class HymnFilters(
    val page: Int,
    val size: Int,
    val query: String?,
    val status: HymnStatus?,
    val tempo: HymnTempo?,
    val musicalKey: String?,
)

data class HymnPage(
    val items: List<Hymn>,
    val totalItems: Long,
)


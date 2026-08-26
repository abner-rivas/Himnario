package com.himnario.features.hymns.dto

import com.himnario.common.dto.PaginationResponse
import com.himnario.features.hymns.model.Hymn
import com.himnario.features.hymns.model.HymnMusicalMode
import com.himnario.features.hymns.model.HymnStatus
import com.himnario.features.hymns.model.HymnTempo
import kotlinx.serialization.Serializable

@Serializable
data class HymnResponse(
    val id: String,
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
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class HymnListResponse(
    val items: List<HymnResponse>,
    val pagination: PaginationResponse,
)

fun Hymn.toResponse(): HymnResponse = HymnResponse(
    id = id.toString(),
    title = title,
    slug = slug,
    description = description,
    lyrics = lyrics,
    musicalKey = musicalKey,
    musicalMode = musicalMode,
    bpm = bpm,
    tempo = tempo,
    status = status,
    version = version,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)


package com.himnario.features.hymns.dto

import com.himnario.features.hymns.model.HymnMusicalMode
import com.himnario.features.hymns.model.HymnStatus
import com.himnario.features.hymns.model.HymnTempo
import kotlinx.serialization.Serializable

@Serializable
data class CreateHymnRequest(
    val title: String,
    val description: String? = null,
    val lyrics: String? = null,
    val musicalKey: String? = null,
    val musicalMode: HymnMusicalMode? = null,
    val bpm: Int? = null,
    val tempo: HymnTempo? = null,
    val status: HymnStatus = HymnStatus.DRAFT,
)

@Serializable
data class UpdateHymnRequest(
    val title: String? = null,
    val description: String? = null,
    val lyrics: String? = null,
    val musicalKey: String? = null,
    val musicalMode: HymnMusicalMode? = null,
    val bpm: Int? = null,
    val tempo: HymnTempo? = null,
    val status: HymnStatus? = null,
)


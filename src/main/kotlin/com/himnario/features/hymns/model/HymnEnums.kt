package com.himnario.features.hymns.model

import kotlinx.serialization.Serializable

@Serializable
enum class HymnTempo {
    SLOW,
    MEDIUM,
    FAST,
}

@Serializable
enum class HymnStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED,
}

@Serializable
enum class HymnMusicalMode {
    MAJOR,
    MINOR,
}


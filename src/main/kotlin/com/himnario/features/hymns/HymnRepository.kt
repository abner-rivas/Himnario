package com.himnario.features.hymns

import com.himnario.features.hymns.model.Hymn
import com.himnario.features.hymns.model.HymnFilters
import com.himnario.features.hymns.model.HymnPage
import java.util.UUID

interface HymnRepository {
    suspend fun findAll(filters: HymnFilters): HymnPage

    suspend fun findById(id: UUID): Hymn?

    suspend fun slugExists(slug: String, excludingId: UUID? = null): Boolean

    @Throws(SlugAlreadyExistsException::class)
    suspend fun create(hymn: Hymn): Hymn

    @Throws(SlugAlreadyExistsException::class)
    suspend fun update(hymn: Hymn): Hymn?
}

class SlugAlreadyExistsException : RuntimeException()


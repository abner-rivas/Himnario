package com.himnario.support

import com.himnario.features.hymns.HymnRepository
import com.himnario.features.hymns.SlugAlreadyExistsException
import com.himnario.features.hymns.model.Hymn
import com.himnario.features.hymns.model.HymnFilters
import com.himnario.features.hymns.model.HymnPage
import java.util.Locale
import java.util.UUID

class FakeHymnRepository : HymnRepository {
    private val hymns = linkedMapOf<UUID, Hymn>()

    override suspend fun findAll(filters: HymnFilters): HymnPage {
        val matching = hymns.values
            .asSequence()
            .filter { it.deletedAt == null }
            .filter { hymn ->
                filters.query?.lowercase(Locale.ROOT)?.let { query ->
                    query in hymn.title.lowercase(Locale.ROOT) || query in hymn.slug
                } ?: true
            }
            .filter { filters.status == null || it.status == filters.status }
            .filter { filters.tempo == null || it.tempo == filters.tempo }
            .filter {
                filters.musicalKey == null || it.musicalKey.equals(filters.musicalKey, ignoreCase = true)
            }
            .sortedWith(compareBy(Hymn::title, Hymn::id))
            .toList()
        val offset = (filters.page - 1) * filters.size
        return HymnPage(
            items = matching.drop(offset).take(filters.size),
            totalItems = matching.size.toLong(),
        )
    }

    override suspend fun findById(id: UUID): Hymn? = hymns[id]?.takeIf { it.deletedAt == null }

    override suspend fun slugExists(slug: String, excludingId: UUID?): Boolean = hymns.values.any {
        it.slug == slug && it.id != excludingId && it.deletedAt == null
    }

    override suspend fun create(hymn: Hymn): Hymn {
        if (slugExists(hymn.slug)) throw SlugAlreadyExistsException()
        hymns[hymn.id] = hymn
        return hymn
    }

    override suspend fun update(hymn: Hymn): Hymn? {
        if (hymns[hymn.id] == null) return null
        if (slugExists(hymn.slug, excludingId = hymn.id)) throw SlugAlreadyExistsException()
        hymns[hymn.id] = hymn
        return hymn
    }
}


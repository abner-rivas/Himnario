package com.himnario.features.hymns

import com.himnario.features.hymns.model.Hymn
import com.himnario.features.hymns.model.HymnFilters
import com.himnario.features.hymns.model.HymnPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.SQLException
import java.util.Locale
import java.util.UUID

class ExposedHymnRepository(private val database: Database) : HymnRepository {
    override suspend fun findAll(filters: HymnFilters): HymnPage = databaseQuery {
        val totalItems = filteredQuery(filters).count()
        val items = filteredQuery(filters)
            .orderBy(HymnsTable.title to SortOrder.ASC, HymnsTable.id to SortOrder.ASC)
            .limit(filters.size)
            .offset(((filters.page - 1) * filters.size).toLong())
            .map { row -> row.toHymn() }

        HymnPage(items = items, totalItems = totalItems)
    }

    override suspend fun findById(id: UUID): Hymn? = databaseQuery {
        HymnsTable
            .selectAll()
            .where { HymnsTable.id eq id }
            .andWhere { HymnsTable.deletedAt.isNull() }
            .firstOrNull()
            ?.toHymn()
    }

    override suspend fun slugExists(slug: String, excludingId: UUID?): Boolean = databaseQuery {
        val query = HymnsTable
            .selectAll()
            .where { HymnsTable.slug eq slug }
            .andWhere { HymnsTable.deletedAt.isNull() }

        if (excludingId != null) {
            query.andWhere { HymnsTable.id neq excludingId }
        }
        !query.empty()
    }

    override suspend fun create(hymn: Hymn): Hymn = translateSlugConflict {
        databaseQuery {
            HymnsTable.insert { statement ->
                statement[id] = hymn.id
                statement[title] = hymn.title
                statement[slug] = hymn.slug
                statement[description] = hymn.description
                statement[lyrics] = hymn.lyrics
                statement[musicalKey] = hymn.musicalKey
                statement[musicalMode] = hymn.musicalMode
                statement[bpm] = hymn.bpm
                statement[tempo] = hymn.tempo
                statement[status] = hymn.status
                statement[version] = hymn.version
                statement[createdAt] = hymn.createdAt
                statement[updatedAt] = hymn.updatedAt
                statement[deletedAt] = hymn.deletedAt
            }
            hymn
        }
    }

    override suspend fun update(hymn: Hymn): Hymn? = translateSlugConflict {
        databaseQuery {
            val updatedRows = HymnsTable.update(
                where = {
                    (HymnsTable.id eq hymn.id) and HymnsTable.deletedAt.isNull()
                },
            ) { statement ->
                statement[title] = hymn.title
                statement[slug] = hymn.slug
                statement[description] = hymn.description
                statement[lyrics] = hymn.lyrics
                statement[musicalKey] = hymn.musicalKey
                statement[musicalMode] = hymn.musicalMode
                statement[bpm] = hymn.bpm
                statement[tempo] = hymn.tempo
                statement[status] = hymn.status
                statement[version] = hymn.version
                statement[updatedAt] = hymn.updatedAt
            }
            hymn.takeIf { updatedRows == 1 }
        }
    }

    private fun filteredQuery(filters: HymnFilters): Query {
        val query = HymnsTable
            .selectAll()
            .where { HymnsTable.deletedAt.isNull() }

        filters.query?.let { searchTerm ->
            val pattern = "%${searchTerm.lowercase(Locale.ROOT)}%"
            query.andWhere {
                (HymnsTable.title.lowerCase() like pattern) or
                    (HymnsTable.slug.lowerCase() like pattern)
            }
        }
        filters.status?.let { value -> query.andWhere { HymnsTable.status eq value } }
        filters.tempo?.let { value -> query.andWhere { HymnsTable.tempo eq value } }
        filters.musicalKey?.let { value ->
            query.andWhere { HymnsTable.musicalKey.lowerCase() eq value.lowercase(Locale.ROOT) }
        }
        return query
    }

    private suspend fun <T> databaseQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }

    private suspend fun <T> translateSlugConflict(block: suspend () -> T): T = try {
        block()
    } catch (cause: Exception) {
        if (cause.hasSqlState(POSTGRES_UNIQUE_VIOLATION)) {
            throw SlugAlreadyExistsException()
        }
        throw cause
    }

    private fun ResultRow.toHymn(): Hymn = Hymn(
        id = this[HymnsTable.id],
        title = this[HymnsTable.title],
        slug = this[HymnsTable.slug],
        description = this[HymnsTable.description],
        lyrics = this[HymnsTable.lyrics],
        musicalKey = this[HymnsTable.musicalKey],
        musicalMode = this[HymnsTable.musicalMode],
        bpm = this[HymnsTable.bpm],
        tempo = this[HymnsTable.tempo],
        status = this[HymnsTable.status],
        version = this[HymnsTable.version],
        createdAt = this[HymnsTable.createdAt],
        updatedAt = this[HymnsTable.updatedAt],
        deletedAt = this[HymnsTable.deletedAt],
    )

    private fun Throwable.hasSqlState(sqlState: String): Boolean =
        generateSequence(this) { it.cause }
            .filterIsInstance<SQLException>()
            .any { it.sqlState == sqlState }

    private companion object {
        const val POSTGRES_UNIQUE_VIOLATION = "23505"
    }
}

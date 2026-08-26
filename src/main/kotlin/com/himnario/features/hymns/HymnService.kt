package com.himnario.features.hymns

import com.himnario.common.exceptions.ResourceConflictException
import com.himnario.common.exceptions.ResourceNotFoundException
import com.himnario.common.exceptions.ValidationException
import com.himnario.features.hymns.dto.CreateHymnRequest
import com.himnario.features.hymns.dto.UpdateHymnRequest
import com.himnario.features.hymns.model.Hymn
import com.himnario.features.hymns.model.HymnFilters
import com.himnario.features.hymns.model.HymnPage
import com.himnario.features.hymns.model.HymnStatus
import java.text.Normalizer
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

class HymnService(
    private val repository: HymnRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun list(filters: HymnFilters): HymnPage {
        if (filters.page < 1) {
            throw ValidationException("INVALID_PAGE", "Page must be greater than or equal to 1")
        }
        if (filters.size !in 1..MAX_PAGE_SIZE) {
            throw ValidationException(
                "INVALID_PAGE_SIZE",
                "Size must be between 1 and $MAX_PAGE_SIZE",
            )
        }
        return repository.findAll(filters)
    }

    suspend fun get(id: UUID): Hymn = repository.findById(id) ?: hymnNotFound()

    suspend fun create(request: CreateHymnRequest): Hymn {
        val title = validateTitle(request.title)
        validateBpm(request.bpm)
        val description = normalizeOptional(request.description, MAX_DESCRIPTION_LENGTH, "description")
        val lyrics = normalizeOptional(request.lyrics, MAX_LYRICS_LENGTH, "lyrics")
        val musicalKey = normalizeOptional(request.musicalKey, MAX_MUSICAL_KEY_LENGTH, "musicalKey")
        val baseSlug = slugify(title)

        for (sequence in 1..MAX_SLUG_ATTEMPTS) {
            val slug = slugCandidate(baseSlug, sequence)
            if (repository.slugExists(slug)) continue

            val now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
            val hymn = Hymn(
                id = UUID.randomUUID(),
                title = title,
                slug = slug,
                description = description,
                lyrics = lyrics,
                musicalKey = musicalKey,
                musicalMode = request.musicalMode,
                bpm = request.bpm,
                tempo = request.tempo,
                status = request.status,
                version = 1,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )

            try {
                return repository.create(hymn)
            } catch (_: SlugAlreadyExistsException) {
                // A concurrent insert won the unique slug; try the next suffix.
            }
        }
        throw slugConflict()
    }

    suspend fun update(id: UUID, request: UpdateHymnRequest): Hymn {
        val current = repository.findById(id) ?: hymnNotFound()
        val title = request.title?.let(::validateTitle) ?: current.title
        validateBpm(request.bpm)

        val baseSlug = slugify(title)
        for (sequence in 1..MAX_SLUG_ATTEMPTS) {
            val slug = slugCandidate(baseSlug, sequence)
            if (repository.slugExists(slug, excludingId = id)) continue

            val updated = current.copy(
                title = title,
                slug = slug,
                description = request.description?.let {
                    normalizeOptional(it, MAX_DESCRIPTION_LENGTH, "description")
                } ?: current.description,
                lyrics = request.lyrics?.let {
                    normalizeOptional(it, MAX_LYRICS_LENGTH, "lyrics")
                } ?: current.lyrics,
                musicalKey = request.musicalKey?.let {
                    normalizeOptional(it, MAX_MUSICAL_KEY_LENGTH, "musicalKey")
                } ?: current.musicalKey,
                musicalMode = request.musicalMode ?: current.musicalMode,
                bpm = request.bpm ?: current.bpm,
                tempo = request.tempo ?: current.tempo,
                status = request.status ?: current.status,
                version = current.version + 1,
                updatedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
            )

            try {
                return repository.update(updated) ?: hymnNotFound()
            } catch (_: SlugAlreadyExistsException) {
                // A concurrent update won the unique slug; try the next suffix.
            }
        }
        throw slugConflict()
    }

    suspend fun archive(id: UUID): Hymn {
        val current = repository.findById(id) ?: hymnNotFound()
        if (current.status == HymnStatus.ARCHIVED) return current

        val archived = current.copy(
            status = HymnStatus.ARCHIVED,
            version = current.version + 1,
            updatedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
        )
        return repository.update(archived) ?: hymnNotFound()
    }

    private fun validateTitle(rawTitle: String): String {
        val title = rawTitle.trim()
        if (title.isEmpty()) {
            throw ValidationException("INVALID_TITLE", "Title must not be empty")
        }
        if (title.length > MAX_TITLE_LENGTH) {
            throw ValidationException(
                "INVALID_TITLE",
                "Title must not exceed $MAX_TITLE_LENGTH characters",
            )
        }
        return title
    }

    private fun validateBpm(bpm: Int?) {
        if (bpm != null && bpm <= 0) {
            throw ValidationException("INVALID_BPM", "BPM must be greater than 0")
        }
    }

    private fun normalizeOptional(value: String?, maxLength: Int, field: String): String? {
        val normalized = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (normalized.length > maxLength) {
            throw ValidationException(
                "INVALID_${field.uppercase(Locale.ROOT)}",
                "$field must not exceed $maxLength characters",
            )
        }
        return normalized
    }

    private fun slugify(title: String): String {
        val ascii = Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)
        return ascii
            .replace(NON_ALPHANUMERIC, "-")
            .trim('-')
            .ifEmpty { "hymn" }
            .take(MAX_SLUG_LENGTH)
            .trimEnd('-')
    }

    private fun slugCandidate(baseSlug: String, sequence: Int): String {
        if (sequence == 1) return baseSlug
        val suffix = "-$sequence"
        return baseSlug.take(MAX_SLUG_LENGTH - suffix.length).trimEnd('-') + suffix
    }

    private fun hymnNotFound(): Nothing = throw ResourceNotFoundException(
        code = "HYMN_NOT_FOUND",
        message = "Hymn not found",
    )

    private fun slugConflict(): ResourceConflictException = ResourceConflictException(
        code = "HYMN_SLUG_CONFLICT",
        message = "Unable to generate a unique hymn slug",
    )

    companion object {
        const val MAX_PAGE_SIZE = 100
        private const val MAX_TITLE_LENGTH = 255
        private const val MAX_SLUG_LENGTH = 255
        private const val MAX_DESCRIPTION_LENGTH = 5_000
        private const val MAX_LYRICS_LENGTH = 100_000
        private const val MAX_MUSICAL_KEY_LENGTH = 16
        private const val MAX_SLUG_ATTEMPTS = 1_000
        private val COMBINING_MARKS = Regex("\\p{M}+")
        private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
    }
}


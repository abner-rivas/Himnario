package com.himnario.features.hymns

import com.himnario.features.hymns.model.HymnMusicalMode
import com.himnario.features.hymns.model.HymnStatus
import com.himnario.features.hymns.model.HymnTempo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object HymnsTable : Table("hymns") {
    val id = javaUUID("id")
    val title = varchar("title", 255)
    val slug = varchar("slug", 255).uniqueIndex()
    val description = text("description").nullable()
    val lyrics = text("lyrics").nullable()
    val musicalKey = varchar("musical_key", 16).nullable()
    val musicalMode = enumerationByName<HymnMusicalMode>("musical_mode", 16).nullable()
    val bpm = integer("bpm").nullable()
    val tempo = enumerationByName<HymnTempo>("tempo", 20).nullable()
    val status = enumerationByName<HymnStatus>("status", 20)
    val version = integer("version")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

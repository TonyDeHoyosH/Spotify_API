package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Tracks : Table("tracks") {
    val id = uuid("id").autoGenerate()
    val title = varchar("title", 150)
    val durationSeconds = integer("duration_seconds")
    val albumId = uuid("album_id").references(Albumes.id)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}
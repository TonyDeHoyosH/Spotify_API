package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Albumes : Table("albumes") {
    val id = uuid("id").autoGenerate()
    val title = varchar("title", 150)
    val releaseYear = integer("release_year")
    val artistId = uuid("artist_id").references(Artistas.id)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}
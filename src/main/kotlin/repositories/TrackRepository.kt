package com.example.repositories

import com.example.database.DatabaseFactory.dbQuery
import com.example.database.tables.Tracks
import com.example.models.TrackRequest
import com.example.models.TrackResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class TrackRepository {

    suspend fun create(request: TrackRequest): TrackResponse = dbQuery {
        val insertStatement = Tracks.insert {
            it[title] = request.title
            it[durationSeconds] = request.duration
            it[albumId] = UUID.fromString(request.albumId)
        }

        insertStatement.resultedValues?.first()?.let { rowToTrackResponse(it) }
            ?: throw Exception("Error al crear track")
    }

    suspend fun findAll(page: Int = 0, size: Int = 10): List<TrackResponse> = dbQuery {
        Tracks.selectAll()
            .limit(size, offset = (page * size).toLong())
            .orderBy(Tracks.createdAt to SortOrder.DESC)
            .map { rowToTrackResponse(it) }
    }

    suspend fun findById(id: String): TrackResponse? = dbQuery {
        Tracks.select { Tracks.id eq UUID.fromString(id) }
            .mapNotNull { rowToTrackResponse(it) }
            .singleOrNull()
    }

    suspend fun findByAlbumId(albumId: String, page: Int = 0, size: Int = 10): List<TrackResponse> = dbQuery {
        Tracks.select { Tracks.albumId eq UUID.fromString(albumId) }
            .limit(size, offset = (page * size).toLong())
            .orderBy(Tracks.title to SortOrder.ASC)
            .map { rowToTrackResponse(it) }
    }

    suspend fun update(id: String, request: TrackRequest): Boolean = dbQuery {
        Tracks.update({ Tracks.id eq UUID.fromString(id) }) {
            it[title] = request.title
            it[durationSeconds] = request.duration
            it[albumId] = UUID.fromString(request.albumId)
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        Tracks.deleteWhere { Tracks.id eq UUID.fromString(id) } > 0
    }

    private fun rowToTrackResponse(row: ResultRow): TrackResponse {
        return TrackResponse(
            id = row[Tracks.id].toString(),
            title = row[Tracks.title],
            duration = row[Tracks.durationSeconds],
            albumId = row[Tracks.albumId].toString(),
            createdAt = row[Tracks.createdAt].toString(),
            updatedAt = row[Tracks.updatedAt].toString()
        )
    }
}
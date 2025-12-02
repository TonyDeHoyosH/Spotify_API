package com.example.repositories

import com.example.database.DatabaseFactory.dbQuery
import com.example.database.tables.Albumes
import com.example.database.tables.Tracks
import com.example.models.AlbumRequest
import com.example.models.AlbumResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class AlbumRepository {

    suspend fun create(request: AlbumRequest): AlbumResponse = dbQuery {
        val insertStatement = Albumes.insert {
            it[title] = request.title
            it[releaseYear] = request.releaseYear
            it[artistId] = UUID.fromString(request.artistId)
        }

        insertStatement.resultedValues?.first()?.let { rowToAlbumResponse(it) }
            ?: throw Exception("Error al crear álbum")
    }

    suspend fun findAll(page: Int = 0, size: Int = 10): List<AlbumResponse> = dbQuery {
        Albumes.selectAll()
            .limit(size, offset = (page * size).toLong())
            .orderBy(Albumes.releaseYear to SortOrder.DESC)
            .map { rowToAlbumResponse(it) }
    }

    suspend fun findById(id: String): AlbumResponse? = dbQuery {
        Albumes.select { Albumes.id eq UUID.fromString(id) }
            .mapNotNull { rowToAlbumResponse(it) }
            .singleOrNull()
    }

    suspend fun findByArtistId(artistId: String, page: Int = 0, size: Int = 10): List<AlbumResponse> = dbQuery {
        Albumes.select { Albumes.artistId eq UUID.fromString(artistId) }
            .limit(size, offset = (page * size).toLong())
            .orderBy(Albumes.releaseYear to SortOrder.ASC)
            .map { rowToAlbumResponse(it) }
    }

    suspend fun update(id: String, request: AlbumRequest): Boolean = dbQuery {
        Albumes.update({ Albumes.id eq UUID.fromString(id) }) {
            it[title] = request.title
            it[releaseYear] = request.releaseYear
            it[artistId] = UUID.fromString(request.artistId)
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        Albumes.deleteWhere { Albumes.id eq UUID.fromString(id) } > 0
    }

    suspend fun canDelete(id: String): Pair<Boolean, Int> = dbQuery {
        val count = Tracks.select { Tracks.albumId eq UUID.fromString(id) }.count()
        Pair(count == 0L, count.toInt())
    }

    private fun rowToAlbumResponse(row: ResultRow): AlbumResponse {
        return AlbumResponse(
            id = row[Albumes.id].toString(),
            title = row[Albumes.title],
            releaseYear = row[Albumes.releaseYear],
            artistId = row[Albumes.artistId].toString(),
            createdAt = row[Albumes.createdAt].toString(),
            updatedAt = row[Albumes.updatedAt].toString()
        )
    }
}
package com.example.repositories

import com.example.database.DatabaseFactory.dbQuery
import com.example.database.tables.Albumes
import com.example.database.tables.Artistas
import com.example.database.tables.Tracks
import com.example.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ArtistaRepository {

    suspend fun create(request: ArtistaRequest): ArtistaResponse = dbQuery {
        val insertStatement = Artistas.insert {
            it[name] = request.name
            it[genre] = request.genre
        }

        insertStatement.resultedValues?.first()?.let { rowToArtistaResponse(it) }
            ?: throw Exception("Error al crear artista")
    }

    suspend fun findAll(page: Int = 0, size: Int = 10, searchName: String? = null): List<ArtistaResponse> = dbQuery {
        val query = if (searchName != null) {
            Artistas.select { Artistas.name.lowerCase() like "%${searchName.lowercase()}%" }
        } else {
            Artistas.selectAll()
        }

        query
            .limit(size, offset = (page * size).toLong())
            .orderBy(Artistas.createdAt to SortOrder.DESC)
            .map { rowToArtistaResponse(it) }
    }

    suspend fun findById(id: String): ArtistaResponse? = dbQuery {
        Artistas.select { Artistas.id eq UUID.fromString(id) }
            .mapNotNull { rowToArtistaResponse(it) }
            .singleOrNull()
    }

    suspend fun findByIdWithAlbumes(id: String): ArtistaWithAlbumes? = dbQuery {
        val artistaRow = Artistas.select { Artistas.id eq UUID.fromString(id) }
            .singleOrNull() ?: return@dbQuery null

        val albumes = (Albumes innerJoin Tracks)
            .slice(
                Albumes.id,
                Albumes.title,
                Albumes.releaseYear,
                Tracks.id,
                Tracks.title,
                Tracks.durationSeconds
            )
            .select { Albumes.artistId eq UUID.fromString(id) }
            .groupBy { it[Albumes.id] }
            .map { (albumId, rows) ->
                val firstRow = rows.first()
                AlbumWithTracks(
                    id = albumId.toString(),
                    title = firstRow[Albumes.title],
                    releaseYear = firstRow[Albumes.releaseYear],
                    tracks = rows.map { row ->
                        TrackResponse(
                            id = row[Tracks.id].toString(),
                            title = row[Tracks.title],
                            duration = row[Tracks.durationSeconds],
                            albumId = albumId.toString()
                        )
                    }
                )
            }

        ArtistaWithAlbumes(
            id = artistaRow[Artistas.id].toString(),
            name = artistaRow[Artistas.name],
            genre = artistaRow[Artistas.genre],
            createdAt = artistaRow[Artistas.createdAt].toString(),
            updatedAt = artistaRow[Artistas.updatedAt].toString(),
            albumes = albumes
        )
    }

    suspend fun update(id: String, request: ArtistaRequest): Boolean = dbQuery {
        Artistas.update({ Artistas.id eq UUID.fromString(id) }) {
            it[name] = request.name
            it[genre] = request.genre
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        Artistas.deleteWhere { Artistas.id eq UUID.fromString(id) } > 0
    }

    suspend fun canDelete(id: String): Pair<Boolean, Int> = dbQuery {
        val count = Albumes.select { Albumes.artistId eq UUID.fromString(id) }.count()
        Pair(count == 0L, count.toInt())
    }

    private fun rowToArtistaResponse(row: ResultRow): ArtistaResponse {
        return ArtistaResponse(
            id = row[Artistas.id].toString(),
            name = row[Artistas.name],
            genre = row[Artistas.genre],
            createdAt = row[Artistas.createdAt].toString(),
            updatedAt = row[Artistas.updatedAt].toString()
        )
    }
}
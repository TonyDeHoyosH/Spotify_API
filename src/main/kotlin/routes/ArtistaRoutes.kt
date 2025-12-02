package com.example.routes

import com.example.models.ArtistaRequest
import com.example.models.DeleteValidationResponse
import com.example.models.ErrorResponse
import com.example.models.SuccessResponse
import com.example.repositories.ArtistaRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.artistaRoutes(repository: ArtistaRepository) {
    route("/artistas") {

        // POST /api/artistas - Crear artista (protegido con JWT)
        authenticate("auth-jwt") {
            post {
                try {
                    val request = call.receive<ArtistaRequest>()

                    // Validaciones
                    if (request.name.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("El nombre del artista es requerido"))
                        return@post
                    }

                    val artista = repository.create(request)
                    call.respond(HttpStatusCode.Created, artista)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al crear artista: ${e.message}"))
                }
            }
        }

        // GET /api/artistas - Listar todos con paginación y búsqueda
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
                val search = call.request.queryParameters["name"] // Búsqueda por nombre

                if (page < 0 || size < 1 || size > 100) {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("Parámetros de paginación inválidos"))
                    return@get
                }

                val artistas = repository.findAll(page, size, search)

                call.respond(HttpStatusCode.OK, artistas)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("Error al obtener artistas: ${e.message}"))
            }
        }

        // GET /api/artistas/{id} - Obtener artista con relaciones (álbumes y tracks)
        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("ID requerido"))
                    return@get
                }

                // Validar UUID
                try {
                    UUID.fromString(id)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("ID inválido"))
                    return@get
                }

                val artista = repository.findByIdWithAlbumes(id)

                if (artista != null) {
                    call.respond(HttpStatusCode.OK, artista)
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ErrorResponse("Artista no encontrado"))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("Error al obtener artista: ${e.message}"))
            }
        }

        // GET /api/artistas/{id}/albumes - Obtener álbumes de un artista
        get("/{id}/albumes") {
            try {
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("ID requerido"))
                    return@get
                }

                try {
                    UUID.fromString(id)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("ID inválido"))
                    return@get
                }

                val artista = repository.findByIdWithAlbumes(id)
                val albumes = artista?.albumes ?: emptyList()
                call.respond(HttpStatusCode.OK, albumes)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("Error al obtener álbumes: ${e.message}"))
            }
        }

        // PUT /api/artistas/{id} - Actualizar artista (protegido con JWT)
        authenticate("auth-jwt") {
            put("/{id}") {
                try {
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("ID requerido"))
                        return@put
                    }

                    try {
                        UUID.fromString(id)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("ID inválido"))
                        return@put
                    }

                    val request = call.receive<ArtistaRequest>()

                    // Validaciones
                    if (request.name.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("El nombre no puede estar vacío"))
                        return@put
                    }

                    val updated = repository.update(id, request)

                    if (updated) {
                        val artista = repository.findById(id)
                        call.respond(HttpStatusCode.OK, artista!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound,
                            ErrorResponse("Artista no encontrado"))
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al actualizar artista: ${e.message}"))
                }
            }
        }

        // DELETE /api/artistas/{id} - Verificación y borrado (protegido con JWT)
        authenticate("auth-jwt") {
            // GET /api/artistas/{id}/can-delete - Verificar si se puede borrar
            get("/{id}/can-delete") {
                try {
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("ID requerido"))
                        return@get
                    }

                    try {
                        UUID.fromString(id)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("ID inválido"))
                        return@get
                    }

                    val (canDelete, albumCount) = repository.canDelete(id)

                    call.respond(HttpStatusCode.OK, DeleteValidationResponse(
                        canDelete = canDelete,
                        reason = if (!canDelete) "El artista tiene álbumes relacionados" else null,
                        relatedCount = albumCount
                    ))

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al verificar artista: ${e.message}"))
                }
            }

            delete("/{id}") {
                try {
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("ID requerido"))
                        return@delete
                    }

                    try {
                        UUID.fromString(id)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("ID inválido"))
                        return@delete
                    }

                    // Verificar primero si se puede borrar
                    val (canDelete, albumCount) = repository.canDelete(id)
                    if (!canDelete) {
                        call.respond(HttpStatusCode.Conflict,
                            ErrorResponse("No se puede borrar el artista porque tiene $albumCount álbum(es) relacionado(s)"))
                        return@delete
                    }

                    val deleted = repository.delete(id)

                    if (deleted) {
                        call.respond(HttpStatusCode.OK,
                            SuccessResponse("Artista eliminado correctamente"))
                    } else {
                        call.respond(HttpStatusCode.NotFound,
                            ErrorResponse("Artista no encontrado"))
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al eliminar artista: ${e.message}"))
                }
            }
        }
    }
}
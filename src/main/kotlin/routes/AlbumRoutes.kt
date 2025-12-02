package com.example.routes

import com.example.models.AlbumRequest
import com.example.models.DeleteValidationResponse
import com.example.models.ErrorResponse
import com.example.models.SuccessResponse
import com.example.repositories.AlbumRepository
import com.example.repositories.TrackRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.albumRoutes(repository: AlbumRepository, trackRepository: TrackRepository) {
    route("/albumes") {

        // POST /api/albumes - Crear álbum (protegido con JWT)
        authenticate("auth-jwt") {
            post {
                try {
                    val request = call.receive<AlbumRequest>()

                    // Validaciones
                    if (request.title.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("El título del álbum es requerido"))
                        return@post
                    }

                    if (request.releaseYear < 1900 || request.releaseYear > 2025) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("Año de lanzamiento inválido"))
                        return@post
                    }

                    val album = repository.create(request)
                    call.respond(HttpStatusCode.Created, album)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al crear álbum: ${e.message}"))
                }
            }
        }

        // GET /api/albumes - Listar todos con paginación
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10

                if (page < 0 || size < 1 || size > 100) {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("Parámetros de paginación inválidos"))
                    return@get
                }

                val albumes = repository.findAll(page, size)
                call.respond(HttpStatusCode.OK, albumes)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("Error al obtener álbumes: ${e.message}"))
            }
        }

        // GET /api/albumes/{id} - Obtener álbum por ID
        get("/{id}") {
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

                val album = repository.findById(id)

                if (album != null) {
                    call.respond(HttpStatusCode.OK, album)
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ErrorResponse("Álbum no encontrado"))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("Error al obtener álbum: ${e.message}"))
            }
        }

        // GET /api/albumes/{id}/tracks - Obtener tracks de un álbum
        get("/{id}/tracks") {
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

                // Obtener tracks del álbum
                val tracks = trackRepository.findByAlbumId(id)
                call.respond(HttpStatusCode.OK, tracks)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("Error al obtener tracks: ${e.message}"))
            }
        }

        // PUT /api/albumes/{id} - Actualizar álbum (protegido con JWT)
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

                    val request = call.receive<AlbumRequest>()

                    // Validaciones
                    if (request.title.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("El título no puede estar vacío"))
                        return@put
                    }

                    if (request.releaseYear < 1900 || request.releaseYear > 2025) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("Año de lanzamiento inválido"))
                        return@put
                    }

                    val updated = repository.update(id, request)

                    if (updated) {
                        val album = repository.findById(id)
                        call.respond(HttpStatusCode.OK, album!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound,
                            ErrorResponse("Álbum no encontrado"))
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al actualizar álbum: ${e.message}"))
                }
            }
        }

        // DELETE /api/albumes/{id} - Verificación y borrado (protegido con JWT)
        authenticate("auth-jwt") {
            // GET /api/albumes/{id}/can-delete - Verificar si se puede borrar
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

                    val (canDelete, trackCount) = repository.canDelete(id)

                    call.respond(HttpStatusCode.OK, DeleteValidationResponse(
                        canDelete = canDelete,
                        reason = if (!canDelete) "El álbum tiene tracks relacionados" else null,
                        relatedCount = trackCount
                    ))

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al verificar álbum: ${e.message}"))
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
                    val (canDelete, trackCount) = repository.canDelete(id)
                    if (!canDelete) {
                        call.respond(HttpStatusCode.Conflict,
                            ErrorResponse("No se puede borrar el álbum porque tiene $trackCount track(s) relacionado(s)"))
                        return@delete
                    }

                    val deleted = repository.delete(id)

                    if (deleted) {
                        call.respond(HttpStatusCode.OK,
                            SuccessResponse("Álbum eliminado correctamente"))
                    } else {
                        call.respond(HttpStatusCode.NotFound,
                            ErrorResponse("Álbum no encontrado"))
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al eliminar álbum: ${e.message}"))
                }
            }
        }
    }
}
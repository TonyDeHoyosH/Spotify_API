package com.example.routes

import com.example.models.ErrorResponse
import com.example.models.SuccessResponse
import com.example.models.TrackRequest
import com.example.repositories.TrackRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.trackRoutes(repository: TrackRepository) {
    route("/tracks") {

        // POST /api/tracks - Crear track (protegido con JWT)
        authenticate("auth-jwt") {
            post {
                try {
                    val request = call.receive<TrackRequest>()

                    // Validaciones
                    if (request.title.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("El título del track es requerido"))
                        return@post
                    }

                    if (request.duration <= 0) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("La duración debe ser mayor a 0"))
                        return@post
                    }

                    val track = repository.create(request)
                    call.respond(HttpStatusCode.Created, track)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al crear track: ${e.message}"))
                }
            }
        }

        // GET /api/tracks - Listar todos con paginación
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10

                if (page < 0 || size < 1 || size > 100) {
                    call.respond(HttpStatusCode.BadRequest,
                        ErrorResponse("Parámetros de paginación inválidos"))
                    return@get
                }

                val tracks = repository.findAll(page, size)
                call.respond(HttpStatusCode.OK, tracks)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("Error al obtener tracks: ${e.message}"))
            }
        }

        // GET /api/tracks/{id} - Obtener track por ID
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

                val track = repository.findById(id)

                if (track != null) {
                    call.respond(HttpStatusCode.OK, track)
                } else {
                    call.respond(HttpStatusCode.NotFound,
                        ErrorResponse("Track no encontrado"))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("Error al obtener track: ${e.message}"))
            }
        }

        // PUT /api/tracks/{id} - Actualizar track (protegido con JWT)
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

                    val request = call.receive<TrackRequest>()

                    // Validaciones
                    if (request.title.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("El título no puede estar vacío"))
                        return@put
                    }

                    if (request.duration <= 0) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse("La duración debe ser mayor a 0"))
                        return@put
                    }

                    val updated = repository.update(id, request)

                    if (updated) {
                        val track = repository.findById(id)
                        call.respond(HttpStatusCode.OK, track!!)
                    } else {
                        call.respond(HttpStatusCode.NotFound,
                            ErrorResponse("Track no encontrado"))
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al actualizar track: ${e.message}"))
                }
            }
        }

        // DELETE /api/tracks/{id} - Borrar track (protegido con JWT)
        authenticate("auth-jwt") {
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

                    val deleted = repository.delete(id)

                    if (deleted) {
                        call.respond(HttpStatusCode.OK,
                            SuccessResponse("Track eliminado correctamente"))
                    } else {
                        call.respond(HttpStatusCode.NotFound,
                            ErrorResponse("Track no encontrado"))
                    }

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al eliminar track: ${e.message}"))
                }
            }
        }
    }
}
package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class ArtistaRequest(
    val name: String,
    val genre: String? = null
)

@Serializable
data class ArtistaResponse(
    val id: String,
    val name: String,
    val genre: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class ArtistaWithAlbumes(
    val id: String,
    val name: String,
    val genre: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val albumes: List<AlbumWithTracks> = emptyList()
)
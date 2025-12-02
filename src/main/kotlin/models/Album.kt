package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class AlbumRequest(
    val title: String,
    val releaseYear: Int,
    val artistId: String
)

@Serializable
data class AlbumResponse(
    val id: String,
    val title: String,
    val releaseYear: Int,
    val artistId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class AlbumWithTracks(
    val id: String,
    val title: String,
    val releaseYear: Int,
    val tracks: List<TrackResponse> = emptyList()
)
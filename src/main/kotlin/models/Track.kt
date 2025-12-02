package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class TrackRequest(
    val title: String,
    val duration: Int, // El test usa "duration" no "durationSeconds"
    val albumId: String
)

@Serializable
data class TrackResponse(
    val id: String,
    val title: String,
    val duration: Int,
    val albumId: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Serializable
data class ErrorResponse(
    val error: String,
    val details: String? = null
)

@Serializable
data class DeleteValidationResponse(
    val canDelete: Boolean,
    val reason: String? = null,
    val relatedCount: Int = 0
)

@Serializable
data class SuccessResponse(
    val message: String
)
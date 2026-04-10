package com.example.rag.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val meta: PaginationMeta? = null
) {
    companion object {
        fun <T> ok(data: T, meta: PaginationMeta? = null) =
            ApiResponse(success = true, data = data, meta = meta)

        fun <T> error(message: String) =
            ApiResponse<T>(success = false, error = message)
    }
}

@Serializable
data class PaginationMeta(
    val limit: Int,
    val offset: Int,
    val total: Int
)

package com.example.rag.ui.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Conversation(
    val id: String,
    val userId: String,
    val title: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PaginationMeta(val limit: Int, val offset: Int, val total: Int)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val meta: PaginationMeta? = null
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

object ApiConstants {
    const val BASE_URL = "http://localhost:8081/api"
    const val CONVERSATIONS_ENDPOINT = "$BASE_URL/conversations"
    const val LOGIN_ENDPOINT = "$BASE_URL/login"
}

val apiClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

suspend fun performLoginSuspending(email: String, password: String): String {
    val response = apiClient.post(ApiConstants.LOGIN_ENDPOINT) {
        contentType(ContentType.Application.Json)
        setBody(LoginRequest(email, password))
    }
    
    if (response.status == HttpStatusCode.OK) {
        val loginResponse: LoginResponse = response.body()
        return loginResponse.token
    } else {
        throw Exception("Login failed: ${response.status}")
    }
}

suspend fun fetchConversations(token: String, limit: Int = 20, offset: Int = 0): ApiResponse<List<Conversation>> {
    val response = apiClient.get("${ApiConstants.CONVERSATIONS_ENDPOINT}?limit=$limit&offset=$offset") {
        header(HttpHeaders.Authorization, "Bearer $token")
    }
    return response.body()
}

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
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

object ApiConstants {
    const val BASE_URL = "http://localhost:8081/api"
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

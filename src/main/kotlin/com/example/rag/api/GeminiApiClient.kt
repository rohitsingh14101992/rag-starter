package com.example.rag.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class GeminiApiClient(private val apiKey: String, private val modelName: String = "gemini-1.5-flash") {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
            })
        }
    }

    suspend fun generateContent(prompt: String): String {
        val requestBody = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            )
        )

        val response: HttpResponse = client.post("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent") {
            contentType(ContentType.Application.Json)
            parameter("key", apiKey)
            setBody(requestBody)
        }

        if (response.status.isSuccess()) {
            val geminiResponse = response.body<GeminiResponse>()
            return geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response generated."
        } else {
            return "Failed to fetch from Gemini API. Status: ${response.status}, Body: ${response.bodyAsText()}"
        }
    }
}

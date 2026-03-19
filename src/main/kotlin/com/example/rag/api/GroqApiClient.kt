package com.example.rag.api

import com.example.rag.core.LlmClient

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- Chat Completions API models (for llama, mixtral, etc.) ---
@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1024
)

@Serializable
data class GroqMessage(val role: String, val content: String)

@Serializable
data class GroqResponse(val choices: List<GroqChoice>? = null)

@Serializable
data class GroqChoice(val message: GroqMessage? = null)

// --- Responses API models (for openai/* models on Groq) ---
@Serializable
data class GroqResponsRequest(
    val model: String,
    val input: String
)

@Serializable
data class GroqResponsResponse(
    val output: List<GroqOutputItem>? = null
)

@Serializable
data class GroqOutputItem(
    val content: List<GroqOutputContent>? = null
)

@Serializable
data class GroqOutputContent(
    val text: String? = null
)

class GroqApiClient(
    private val apiKey: String,
    private val modelName: String = "llama-3.1-8b-instant"
) : LlmClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    override suspend fun generate(prompt: String): String {
        // openai/* models use the Responses API (/v1/responses)
        return if (modelName.startsWith("openai/")) {
            callResponsesApi(prompt)
        } else {
            callChatCompletionsApi(prompt)
        }
    }

    private suspend fun callChatCompletionsApi(prompt: String): String {
        val requestBody = GroqRequest(
            model = modelName,
            messages = listOf(GroqMessage(role = "user", content = prompt))
        )
        val response: HttpResponse = client.post("https://api.groq.com/openai/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(requestBody)
        }
        return if (response.status.isSuccess()) {
            val r = response.body<GroqResponse>()
            r.choices?.firstOrNull()?.message?.content ?: "No response generated."
        } else {
            "Failed to fetch from Groq API. Status: ${response.status}, Body: ${response.bodyAsText()}"
        }
    }

    private suspend fun callResponsesApi(prompt: String): String {
        val requestBody = GroqResponsRequest(model = modelName, input = prompt)
        val response: HttpResponse = client.post("https://api.groq.com/openai/v1/responses") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(requestBody)
        }
        return if (response.status.isSuccess()) {
            val r = response.body<GroqResponsResponse>()
            r.output?.firstOrNull()?.content?.firstOrNull()?.text ?: "No response generated."
        } else {
            "Failed to fetch from Groq API. Status: ${response.status}, Body: ${response.bodyAsText()}"
        }
    }
}

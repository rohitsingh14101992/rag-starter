package com.example.rag.core

/**
 * Abstraction over any LLM provider (Gemini, Groq, etc.).
 * Implementations are selected and bound by the Koin [appModule] based on config.properties.
 */
interface LlmClient {
    suspend fun generate(prompt: String): String
}

package com.example.rag.service

/**
 * Input to the RAG pipeline.
 *
 * @property question The user's natural-language question.
 * @property sessionId Optional identifier so a [com.example.rag.core.MemoryStore]
 *   implementation can scope history to a specific conversation session.
 *   Defaults to "default" for single-session CLI usage.
 */
data class RagQuery(
    val question: String,
    val sessionId: String = "default"
)

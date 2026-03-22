package com.example.rag.core

import com.example.rag.api.ConversationMessage

/**
 * Stores and retrieves conversation history for a RAG session.
 * Implementations can be in-memory (per-process), session-keyed, or
 * backed by a persistent store (database, Redis, etc.).
 */
interface MemoryStore {
    fun add(message: ConversationMessage)
    fun getHistory(): List<ConversationMessage>
    fun clear()
}

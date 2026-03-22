package com.example.rag.pipeline

import com.example.rag.api.ConversationMessage
import com.example.rag.core.MemoryStore

/**
 * In-process, non-thread-safe implementation of [MemoryStore].
 * Suitable for single-session CLI usage. For concurrent or
 * multi-session use, replace with a session-keyed or persistent store.
 */
class InMemoryMemoryStore : MemoryStore {

    private val history = mutableListOf<ConversationMessage>()

    override fun add(message: ConversationMessage) {
        history.add(message)
    }

    override fun getHistory(): List<ConversationMessage> = history.toList()

    override fun clear() = history.clear()
}

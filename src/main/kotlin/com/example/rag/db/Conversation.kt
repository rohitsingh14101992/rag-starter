package com.example.rag.db

import java.util.*
import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Serializable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
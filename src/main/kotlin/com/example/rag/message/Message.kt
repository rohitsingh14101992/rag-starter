package com.example.rag.message

import com.example.rag.common.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String,
    val content: String,
    @Serializable(with = LocalDateTimeSerializer::class) val createdAt: LocalDateTime = LocalDateTime.now()
)

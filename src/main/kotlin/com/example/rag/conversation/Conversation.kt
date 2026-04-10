package com.example.rag.conversation

import com.example.rag.common.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class) val createdAt: LocalDateTime = LocalDateTime.now(),
    @Serializable(with = LocalDateTimeSerializer::class) val updatedAt: LocalDateTime = LocalDateTime.now()
)

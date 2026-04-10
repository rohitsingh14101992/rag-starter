package com.example.rag.message

import kotlinx.serialization.Serializable

@Serializable
data class CreateMessageRequest(val role: String, val content: String)

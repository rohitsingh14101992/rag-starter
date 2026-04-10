package com.example.rag.conversation

import kotlinx.serialization.Serializable

@Serializable
data class CreateConversationRequest(val title: String? = null)

@Serializable
data class UpdateConversationRequest(val title: String)

package com.example.rag.conversation

sealed class ConversationResult {
    data class Success(val conversations: List<Conversation>, val total: Int) : ConversationResult()
    data class SingleSuccess(val conversation: Conversation) : ConversationResult()
    data class Failure(val reason: String) : ConversationResult()
    data object NotFound : ConversationResult()
    data object Forbidden : ConversationResult()
    data object Deleted : ConversationResult()
}

class ConversationService(
    private val conversationRepo: ConversationRepository
) {
    fun getUserConversations(userId: String, limit: Int, offset: Int): ConversationResult {
        return try {
            val conversations = conversationRepo.getUserConversations(userId, limit, offset)
            val total = conversationRepo.countUserConversations(userId)
            ConversationResult.Success(conversations, total)
        } catch (e: Exception) {
            ConversationResult.Failure(e.message ?: "Unknown database error")
        }
    }

    fun createConversation(userId: String, title: String?): ConversationResult {
        return try {
            val conversation = conversationRepo.createConversation(
                Conversation(userId = userId, title = title)
            )
            ConversationResult.SingleSuccess(conversation)
        } catch (e: Exception) {
            ConversationResult.Failure(e.message ?: "Unknown database error")
        }
    }

    fun updateConversation(userId: String, conversationId: String, title: String): ConversationResult {
        return try {
            val existing = conversationRepo.getConversation(conversationId)
                ?: return ConversationResult.NotFound
            if (existing.userId != userId) return ConversationResult.Forbidden

            val updated = existing.copy(title = title)
            conversationRepo.updateConversation(updated)
            ConversationResult.SingleSuccess(updated)
        } catch (e: Exception) {
            ConversationResult.Failure(e.message ?: "Unknown database error")
        }
    }

    fun deleteConversation(userId: String, conversationId: String): ConversationResult {
        return try {
            val existing = conversationRepo.getConversation(conversationId)
                ?: return ConversationResult.NotFound
            if (existing.userId != userId) return ConversationResult.Forbidden

            conversationRepo.deleteConversation(conversationId)
            ConversationResult.Deleted
        } catch (e: Exception) {
            ConversationResult.Failure(e.message ?: "Unknown database error")
        }
    }

    fun getConversation(conversationId: String): Conversation? {
        return conversationRepo.getConversation(conversationId)
    }
}

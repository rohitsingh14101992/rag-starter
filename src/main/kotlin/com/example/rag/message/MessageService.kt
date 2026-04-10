package com.example.rag.message

import com.example.rag.conversation.ConversationRepository

sealed class MessageResult {
    data class Success(val messages: List<Message>, val total: Int) : MessageResult()
    data class SingleSuccess(val message: Message) : MessageResult()
    data class Failure(val reason: String) : MessageResult()
    data object NotFound : MessageResult()
    data object Forbidden : MessageResult()
}

class MessageService(
    private val messageRepo: MessageRepository,
    private val conversationRepo: ConversationRepository
) {
    fun getMessagesForConversation(userId: String, conversationId: String, limit: Int, offset: Int): MessageResult {
        return try {
            val conversation = conversationRepo.getConversation(conversationId)
                ?: return MessageResult.NotFound
            if (conversation.userId != userId) return MessageResult.Forbidden

            val messages = messageRepo.getMessagesForConversation(conversationId, limit, offset)
            val total = messageRepo.countMessagesForConversation(conversationId)
            MessageResult.Success(messages, total)
        } catch (e: Exception) {
            MessageResult.Failure(e.message ?: "Unknown database error")
        }
    }

    fun createMessage(userId: String, conversationId: String, role: String, content: String): MessageResult {
        return try {
            val conversation = conversationRepo.getConversation(conversationId)
                ?: return MessageResult.NotFound
            if (conversation.userId != userId) return MessageResult.Forbidden

            val message = messageRepo.createMessage(
                Message(conversationId = conversationId, role = role, content = content)
            )
            MessageResult.SingleSuccess(message)
        } catch (e: Exception) {
            MessageResult.Failure(e.message ?: "Unknown database error")
        }
    }
}

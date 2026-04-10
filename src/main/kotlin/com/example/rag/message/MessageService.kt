package com.example.rag.message

import com.example.rag.conversation.ConversationRepository
import com.example.rag.service.KafkaMessage
import com.example.rag.service.KafkaProducerService
import java.util.*

sealed class MessageResult {
    data class Success(val messages: List<Message>, val total: Int) : MessageResult()
    data class SingleSuccess(val message: Message) : MessageResult()
    data class Failure(val reason: String) : MessageResult()
    data object NotFound : MessageResult()
    data object Forbidden : MessageResult()
}

class MessageService(
    private val messageRepo: MessageRepository,
    private val conversationRepo: ConversationRepository,
    private val kafkaProducerService: KafkaProducerService
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

            // If it's a user message, send to Kafka to trigger LLM response
            if (role == "user") {
                kafkaProducerService.sendQuery(
                    KafkaMessage(
                        id = message.id ?: UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        userId = userId,
                        query = content
                    )
                )
            }

            MessageResult.SingleSuccess(message)
        } catch (e: Exception) {
            MessageResult.Failure(e.message ?: "Unknown database error")
        }
    }
}

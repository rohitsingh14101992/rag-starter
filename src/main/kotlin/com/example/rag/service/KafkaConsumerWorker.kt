package com.example.rag.service

import com.example.rag.message.Message
import com.example.rag.message.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

class KafkaConsumerWorker(
    private val properties: Properties,
    private val ragService: RagService,
    private val messageRepo: MessageRepository,
    private val kafkaProducerService: KafkaProducerService
) {
    private val consumer: KafkaConsumer<String, String>

    init {
        val kafkaProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("kafka.bootstrap.servers", "localhost:9092"))
            put(ConsumerConfig.GROUP_ID_CONFIG, properties.getProperty("kafka.group.id", "rag-worker-group"))
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
        consumer = KafkaConsumer<String, String>(kafkaProps)
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        consumer.subscribe(listOf("queries"))
        println("Kafka Consumer Worker started, subscribing to 'queries' topic...")

        try {
            while (isActive) {
                val records = consumer.poll(Duration.ofMillis(500))
                for (record in records) {
                    try {
                        val queryMsg = Json.decodeFromString<KafkaMessage>(record.value())
                        processQuery(queryMsg)
                    } catch (e: Exception) {
                        println("Error processing Kafka message: ${e.message}")
                    }
                }
            }
        } finally {
            consumer.close()
        }
    }

    private suspend fun processQuery(queryMsg: KafkaMessage) {
        println("Processing query: ${queryMsg.query} for conversation ${queryMsg.conversationId}")

        // 1. Run RAG pipeline
        val ragQuery = RagQuery(question = queryMsg.query, chatId = queryMsg.conversationId)
        val ragResponse = ragService.ask(ragQuery)

        // 2. Save result to database
        val assistantMessage = Message(
            conversationId = queryMsg.conversationId,
            role = "assistant",
            content = ragResponse.answer
        )
        val savedMessage = messageRepo.createMessage(assistantMessage)

        // 3. Push to responses topic
        val responseId = savedMessage.id ?: UUID.randomUUID().toString()
        kafkaProducerService.sendResponse(
            KafkaResponseMessage(
                id = responseId,
                conversationId = queryMsg.conversationId,
                response = ragResponse.answer,
                sourceDocs = ragResponse.sources.map { it.content }
            )
        )
    }
}

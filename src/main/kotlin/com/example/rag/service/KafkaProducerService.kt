package com.example.rag.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

@Serializable
data class KafkaMessage(
    val id: String,
    val conversationId: String,
    val userId: String,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class KafkaResponseMessage(
    val id: String,
    val conversationId: String,
    val response: String,
    val sourceDocs: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class KafkaProducerService(private val properties: Properties) {
    private val producer: KafkaProducer<String, String>

    init {
        val kafkaProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("kafka.bootstrap.servers", "localhost:9092"))
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ProducerConfig.ACKS_CONFIG, "all")
        }
        producer = KafkaProducer<String, String>(kafkaProps)
    }

    fun sendQuery(message: KafkaMessage) {
        val json = Json.encodeToString(message)
        val record = ProducerRecord("queries", message.conversationId, json)
        producer.send(record) { metadata, exception ->
            if (exception != null) {
                println("Error sending query to Kafka: ${exception.message}")
            } else {
                println("Query sent to topic ${metadata.topic()} partition ${metadata.partition()} offset ${metadata.offset()}")
            }
        }
    }

    fun sendResponse(message: KafkaResponseMessage) {
        val json = Json.encodeToString(message)
        val record = ProducerRecord("responses", message.conversationId, json)
        producer.send(record) { metadata, exception ->
            if (exception != null) {
                println("Error sending response to Kafka: ${exception.message}")
            } else {
                println("Response sent to topic ${metadata.topic()} partition ${metadata.partition()} offset ${metadata.offset()}")
            }
        }
    }

    fun close() {
        producer.close()
    }
}

package com.example.rag.server

import com.example.rag.service.KafkaResponseMessage
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WebSocketResponseService(private val properties: Properties) {
    private val sessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _responseFlow = MutableSharedFlow<KafkaResponseMessage>()
    
    init {
        startKafkaConsumer()
    }

    private fun startKafkaConsumer() {
        scope.launch {
            val kafkaProps = Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("kafka.bootstrap.servers", "localhost:9092"))
                put(ConsumerConfig.GROUP_ID_CONFIG, "ws-bridge-group-${UUID.randomUUID()}")
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
            }
            val consumer = KafkaConsumer<String, String>(kafkaProps)
            consumer.subscribe(listOf("responses"))
            
            try {
                while (isActive) {
                    val records = consumer.poll(Duration.ofMillis(500))
                    for (record in records) {
                        try {
                            val response = Json.decodeFromString<KafkaResponseMessage>(record.value())
                            broadcastResponse(response)
                        } catch (e: Exception) {
                            println("Error in WS bridge Kafka consumer: ${e.message}")
                        }
                    }
                }
            } finally {
                consumer.close()
            }
        }
    }

    private suspend fun broadcastResponse(response: KafkaResponseMessage) {
        val conversationSessions = sessions[response.conversationId] ?: return
        val messageJson = Json.encodeToString(response)
        
        conversationSessions.toList().forEach { session ->
            try {
                if (session.isActive) {
                    session.send(messageJson)
                }
            } catch (e: Exception) {
                // Remove failed session
                sessions[response.conversationId]?.remove(session)
            }
        }
    }

    fun addSession(conversationId: String, session: DefaultWebSocketServerSession) {
        sessions.computeIfAbsent(conversationId) { Collections.newSetFromMap(ConcurrentHashMap()) }.add(session)
    }

    fun removeSession(conversationId: String, session: DefaultWebSocketServerSession) {
        sessions[conversationId]?.remove(session)
    }
}

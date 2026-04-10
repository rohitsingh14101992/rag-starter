package com.example.rag

import com.example.rag.auth.AuthService
import com.example.rag.auth.JwtConfig
import com.example.rag.auth.UserRepository
import com.example.rag.conversation.ConversationRepository
import com.example.rag.conversation.ConversationService
import com.example.rag.db.DataSourceFactory
import com.example.rag.message.MessageRepository
import com.example.rag.message.MessageService
import com.example.rag.di.appModule
import com.example.rag.pipeline.DocumentSyncService
import com.example.rag.server.createKtorServer
import com.example.rag.service.KafkaProducerService
import com.example.rag.service.RagQuery
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.startKoin
import java.io.File
import java.util.Properties

fun main() = runBlocking {
    println("RAG Assistant — starting up…")

    // ── 1. Load configuration ──────────────────────────────────────────────────
    val properties = Properties()
    File("config.properties").takeIf { it.exists() }
        ?.inputStream()?.use { properties.load(it) }

    // ── 2. Validate LLM API key ────────────────────────────────────────────────
    val provider = properties.getProperty("llm.provider", "gemini").lowercase()
    if (provider == "groq") {
        val key = properties.getProperty("groq.api.key", "")
        if (key.isBlank()) { println("Error: groq.api.key missing"); return@runBlocking }
        println("LLM provider: Groq (${properties.getProperty("groq.model", "llama3-8b-8192")})")
    } else {
        val key = System.getenv("GEMINI_API_KEY") ?: properties.getProperty("gemini.api.key", "")
        if (key.isBlank()) { println("Error: gemini.api.key missing"); return@runBlocking }
        println("LLM provider: Gemini (${properties.getProperty("gemini.model", "gemini-1.5-flash")})")
    }

    // ── 3. Initialize Database ────────────────────────────────────────────────
    val dataSource = DataSourceFactory.create(properties)

    // ── 4. Start Koin (RAG pipeline) ───────────────────────────────────────────
    println("App: Starting Koin...")
    val koin = startKoin { modules(appModule(properties, dataSource)) }.koin
    println("App: Koin started, retrieving syncService...")
    val syncService = koin.get<DocumentSyncService>()
    println("App: syncService retrieved successfully.")
    
    // Initial sync and then every 5 minutes
    launch(Dispatchers.Default) {
        println("App: Background sync coroutine launched.")
        while (true) {
            println("Sync: Starting background synchronization...")
            try {
                syncService.syncFolder("files")
                println("Sync: Background synchronization completed.")
            } catch (e: Exception) {
                println("Sync: Background synchronization failed: ${e.message}")
            }
            val intervalMinutes = properties.getProperty("sync.interval.minutes", "60").toLong()
            delay(intervalMinutes * 60 * 1000)
        }
    }

    // ── 5. Build auth layer & repos ────────────────────────────────────────────
    val userRepo         = UserRepository(dataSource)
    val conversationRepo = ConversationRepository(dataSource)
    val messageRepo      = MessageRepository(dataSource)
    val authService      = AuthService(userRepo, JwtConfig.from(properties))
    val conversationService = ConversationService(conversationRepo)
    val kafkaProducerService = koin.get<KafkaProducerService>()
    val messageService      = MessageService(messageRepo, conversationRepo, kafkaProducerService)
    val wsService           = com.example.rag.server.WebSocketResponseService(properties)

    // ── 5. Start Kafka worker (asynchronous) ──────────────────────────────────
    val kafkaConsumerWorker = com.example.rag.service.KafkaConsumerWorker(
        properties          = properties,
        ragService          = koin.get(),
        messageRepo         = messageRepo,
        kafkaProducerService = kafkaProducerService
    )
    launch(Dispatchers.Default) { kafkaConsumerWorker.start() }

    Runtime.getRuntime().addShutdownHook(Thread {
        kafkaProducerService.close()
    })

    // ── 6. Start Ktor server (blocks until stopped) ────────────────────────────
    createKtorServer(
        port                = properties.getProperty("server.port", "8080").toInt(),
        authService         = authService,
        jwtConfig           = JwtConfig.from(properties),
        conversationService = conversationService,
        messageService      = messageService,
        wsService           = wsService
    ).start(wait = true)
}

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
import com.example.rag.pipeline.DataIngestionService
import com.example.rag.server.createKtorServer
import kotlinx.coroutines.runBlocking
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

    // ── 3. Start Koin (RAG pipeline) ───────────────────────────────────────────
    val koin = startKoin { modules(appModule(properties)) }.koin
    koin.get<DataIngestionService>().ingestFolder("files")

    // ── 4. Build auth layer & repos ────────────────────────────────────────────
    val dataSource       = DataSourceFactory.create(properties)
    val userRepo         = UserRepository(dataSource)
    val conversationRepo = ConversationRepository(dataSource)
    val messageRepo      = MessageRepository(dataSource)
    val authService      = AuthService(userRepo, JwtConfig.from(properties))
    val conversationService = ConversationService(conversationRepo)
    val messageService      = MessageService(messageRepo, conversationRepo)

    // ── 5. Start Ktor server (blocks until stopped) ────────────────────────────
    createKtorServer(
        port                = properties.getProperty("server.port", "8080").toInt(),
        authService         = authService,
        jwtConfig           = JwtConfig.from(properties),
        conversationService = conversationService,
        messageService      = messageService
    ).start(wait = true)
}

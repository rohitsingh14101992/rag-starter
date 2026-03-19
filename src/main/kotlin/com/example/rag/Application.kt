package com.example.rag

import com.example.rag.api.ConversationMessage
import com.example.rag.api.PromptTemplate
import com.example.rag.core.ContentBlock
import com.example.rag.core.Embedder
import com.example.rag.core.LlmClient
import com.example.rag.core.VectorStore
import com.example.rag.di.appModule
import com.example.rag.pipeline.DataIngestionService
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import java.io.File
import java.util.Properties

fun main() = runBlocking {
    println("Welcome to the Kotlin RAG Starter!")

    // ── 1. Load configuration ───────────────────────────────────
    val properties = Properties()
    val configFile = File("config.properties")
    if (configFile.exists()) {
        configFile.inputStream().use { properties.load(it) }
    }

    // ── 2. Validate API key before starting Koin ────────────────
    val provider = properties.getProperty("llm.provider", "gemini").lowercase()
    if (provider == "groq") {
        val apiKey = properties.getProperty("groq.api.key", "")
        if (apiKey.isBlank()) {
            println("Error: groq.api.key is not set in config.properties.")
            return@runBlocking
        }
        println("Using Groq provider with model: ${properties.getProperty("groq.model", "llama3-8b-8192")}")
    } else {
        val apiKey = System.getenv("GEMINI_API_KEY") ?: properties.getProperty("gemini.api.key", "")
        if (apiKey.isBlank()) {
            println("Error: gemini.api.key is not set in config.properties or GEMINI_API_KEY env var.")
            return@runBlocking
        }
        println("Using Gemini provider with model: ${properties.getProperty("gemini.model", "gemini-1.5-flash")}")
    }

    // ── 3. Start Koin and resolve dependencies ──────────────────
    val koin = startKoin {
        modules(appModule(properties))
    }.koin

    val llmClient        = koin.get<LlmClient>()
    val embedder         = koin.get<Embedder>()
    val vectorStore      = koin.get<VectorStore>()
    val ingestionService = koin.get<DataIngestionService>()

    // ── 4. Ingest documents ─────────────────────────────────────
    ingestionService.ingestFolder("files")

    println("\nSystem Ready!")
    println("Enter your prompt (or type 'exit' to quit):")

    // ── 5. Interactive query loop ────────────────────────────────
    val conversationHistory = mutableListOf<ConversationMessage>()

    while (true) {
        print("> ")
        val input = readlnOrNull() ?: break

        if (input.trim().lowercase() == "exit") break

        if (input.isNotBlank()) {
            println("Searching vector database for context...")
            try {
                val queryEmbedding = embedder.embedBatch(listOf(
                    ContentBlock.TextBlock(input)
                )).first()

                val searchResults = vectorStore.search(queryEmbedding, limit = 3)

                println("Found ${searchResults.size} context chunk(s):")
                searchResults.forEachIndexed { i, (block, score) ->
                    if (block is ContentBlock.TextBlock) {
                        val source  = block.metadata["source_id"] ?: "unknown"
                        val page    = block.metadata["page_number"] ?: "?"
                        val preview = block.text.take(80).replace('\n', ' ')
                        println("  [${i + 1}] score=%.4f | $source (page $page) | \"$preview...\"".format(score))
                    }
                }

                val fullPrompt = PromptTemplate.build {
                    withContext(searchResults)
                    withHistory(conversationHistory)
                    withQuestion(input)
                }

                println("Generating response...")
                val response = llmClient.generate(fullPrompt)
                println("\n--- Response ---\n$response\n----------------")

                conversationHistory.add(ConversationMessage("user", input))
                conversationHistory.add(ConversationMessage("assistant", response))

            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    println("Goodbye!")
}

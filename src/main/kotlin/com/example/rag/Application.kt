package com.example.rag

import com.example.rag.di.appModule
import com.example.rag.pipeline.DataIngestionService
import com.example.rag.service.RagQuery
import com.example.rag.service.RagService
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import java.io.File
import java.util.Properties

fun main() = runBlocking {
    println("Welcome to the Kotlin RAG Starter!")

    // ── 1. Load configuration ─────────────────────────────────────
    val properties = Properties()
    val configFile = File("config.properties")
    if (configFile.exists()) {
        configFile.inputStream().use { properties.load(it) }
    }

    // ── 2. Validate API key before starting Koin ──────────────────
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

    // ── 3. Start Koin ─────────────────────────────────────────────
    val koin = startKoin {
        modules(appModule(properties))
    }.koin

    // ── 4. Ingest documents ───────────────────────────────────────
    koin.get<DataIngestionService>().ingestFolder("files")

    // ── 5. Interactive query loop via RagService ──────────────────
    val ragService = koin.get<RagService>()

    println("\nSystem Ready!")
    println("Enter your prompt (or type 'exit' to quit):")

    while (true) {
        print("> ")
        val input = readlnOrNull() ?: break
        if (input.trim().lowercase() == "exit") break

        if (input.isNotBlank()) {
            try {
                val response = ragService.ask(RagQuery(question = input))

                println("Found ${response.sources.size} source chunk(s):")
                response.sources.forEachIndexed { i, (block, score) ->
                    val meta    = block.metadata
                    val source  = meta["source_id"] ?: "unknown"
                    val page    = meta["page_number"] ?: "?"
                    val preview = if (block is com.example.rag.core.ContentBlock.TextBlock)
                        block.text.take(80).replace('\n', ' ') else "[non-text]"
                    println("  [${i + 1}] score=%.4f | $source (page $page) | \"$preview...\"".format(score))
                }

                println("\n--- Response ---\n${response.answer}\n----------------")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    println("Goodbye!")
}

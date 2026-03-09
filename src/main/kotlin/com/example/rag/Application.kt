package com.example.rag

import com.example.rag.api.GeminiApiClient
import com.example.rag.api.GroqApiClient
import com.example.rag.api.PromptTemplate
import com.example.rag.core.ContentBlock
import com.example.rag.db.DatabaseManager
import com.example.rag.pipeline.AllMiniLmL6V2Embedder
import com.example.rag.pipeline.DataIngestionService
import com.example.rag.pipeline.FixedSizeChunker
import com.example.rag.pipeline.PDFBoxDocumentLoader
import com.example.rag.pipeline.RagPipeline
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    println("Welcome to the Kotlin RAG Starter!")

    // Load config.properties
    val properties = java.util.Properties()
    val configFile = java.io.File("config.properties")
    if (configFile.exists()) {
        configFile.inputStream().use { properties.load(it) }
    }

    val provider = properties.getProperty("llm.provider", "gemini").lowercase()

    // Build a simple suspend lambda for whichever provider is active
    val groqClient: GroqApiClient?
    val geminiClient: GeminiApiClient?

    if (provider == "groq") {
        val apiKey = properties.getProperty("groq.api.key") ?: ""
        val model  = properties.getProperty("groq.model", "llama3-8b-8192")
        if (apiKey.isBlank()) {
            println("Error: groq.api.key is not set in config.properties.")
            return@runBlocking
        }
        println("Using Groq provider with model: $model")
        groqClient  = GroqApiClient(apiKey, model)
        geminiClient = null
    } else {
        val apiKey = System.getenv("GEMINI_API_KEY") ?: properties.getProperty("gemini.api.key") ?: ""
        val model  = properties.getProperty("gemini.model", "gemini-1.5-flash")
        if (apiKey.isBlank()) {
            println("Error: gemini.api.key is not set.")
            return@runBlocking
        }
        println("Using Gemini provider with model: $model")
        geminiClient = GeminiApiClient(apiKey, model)
        groqClient   = null
    }

    suspend fun generate(prompt: String): String =
        groqClient?.generateContent(prompt) ?: geminiClient!!.generateContent(prompt)

    // 1. Initialize Vector Database connection
    val vectorStore = try {
        DatabaseManager.setupVectorStore(properties)
    } catch (e: Exception) {
        return@runBlocking
    }

    // 2. Initialize Pipeline Components
    val documentLoader = PDFBoxDocumentLoader()
    val pipeline = RagPipeline(loader = documentLoader)
    val chunker = FixedSizeChunker(maxLength = 500, overlap = 50)
    val embedder = AllMiniLmL6V2Embedder()

    val ingestionService = DataIngestionService(pipeline, chunker, embedder, vectorStore)

    // 3. Process the files folder
    ingestionService.ingestFolder("files")

    println("\nSystem Ready!")
    println("Enter your prompt (or type 'exit' to quit):")

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
                        val source = block.metadata["source"] ?: "unknown"
                        val page   = block.metadata["page"] ?: "?"
                        val preview = block.text.take(80).replace('\n', ' ')
                        println("  [${i + 1}] score=%.4f | $source (page $page) | \"$preview...\"".format(score))
                    }
                }

                val fullPrompt = PromptTemplate.formatRagPrompt(input, searchResults)

                println("Generating response...")
                val response = generate(fullPrompt)
                println("\n--- Response ---\n$response\n----------------")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    println("Goodbye!")
}

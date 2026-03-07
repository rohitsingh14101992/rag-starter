package com.example.rag

import com.example.rag.api.GeminiApiClient
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
    
    val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: properties.getProperty("gemini.api.key")
    val geminiModel = properties.getProperty("gemini.model", "gemini-1.5-flash")
    
    if (geminiApiKey.isNullOrEmpty()) {
        println("Error: Gemini API key is not set.")
        println("Please set it using the GEMINI_API_KEY environment variable or in config.properties as 'gemini.api.key=your_api_key'")
        return@runBlocking
    }
        
    val geminiClient = GeminiApiClient(geminiApiKey, geminiModel)
    
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
    println("Enter your prompt for Gemini (or type 'exit' to quit):")
    
    while (true) {
        print("> ")
        val input = readlnOrNull() ?: break
        
        if (input.trim().lowercase() == "exit") {
            break
        }
        
        if (input.isNotBlank()) {
            println("Searching vector database for context...")
            try {
                // 1. Embed the user's question
                val queryEmbedding = embedder.embedBatch(listOf(
                    com.example.rag.core.ContentBlock.TextBlock(input)
                )).first()
                
                // 3. Search for the most relevant document chunks
                val searchResults = vectorStore.search(queryEmbedding, limit = 3)
                
                // 4. Build strict instruction context prompt
                val fullPrompt = PromptTemplate.formatRagPrompt(input, searchResults)
                
                println("Generating response from Gemini...")
                val response = geminiClient.generateContent(fullPrompt)
                println("\n--- Gemini Response ---\n$response\n-----------------------")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
    
    println("Goodbye!")
}

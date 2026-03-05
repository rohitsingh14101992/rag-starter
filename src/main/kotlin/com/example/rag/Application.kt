package com.example.rag

import com.example.rag.api.GeminiApiClient
import kotlinx.coroutines.runBlocking

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

    println("Enter your prompt for Gemini (or type 'exit' to quit):")
    
    while (true) {
        print("> ")
        val input = readlnOrNull() ?: break
        
        if (input.trim().lowercase() == "exit") {
            break
        }
        
        if (input.isNotBlank()) {
            println("Generating response...")
            try {
                val response = geminiClient.generateContent(input)
                println("\nGemini: $response\n")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
    
    println("Goodbye!")
}

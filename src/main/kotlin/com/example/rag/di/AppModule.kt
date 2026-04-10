package com.example.rag.di

import com.example.rag.api.GeminiApiClient
import com.example.rag.api.GroqApiClient
import com.example.rag.api.PromptTemplate
import com.example.rag.core.Chunker
import com.example.rag.core.DocumentLoader
import com.example.rag.core.Embedder
import com.example.rag.core.LlmClient
import com.example.rag.core.MemoryStore
import com.example.rag.core.VectorStore
import com.example.rag.db.DatabaseManager
import com.example.rag.pipeline.AllMiniLmL6V2Embedder
import com.example.rag.pipeline.DocumentSyncService
import com.example.rag.db.FileTrackerRepository
import com.example.rag.pipeline.FixedSizeChunker
import com.example.rag.pipeline.InMemoryMemoryStore
import com.example.rag.pipeline.PDFBoxDocumentLoader
import com.example.rag.pipeline.RagPipeline
import com.example.rag.service.RagService
import com.example.rag.service.KafkaProducerService
import org.koin.dsl.module
import java.util.Properties

/**
 * Main Koin module for the RAG application.
 *
 * Bindings:
 * - [DocumentLoader]  → [PDFBoxDocumentLoader]
 * - [Chunker]         → [FixedSizeChunker] (500 chars, 50-char overlap)
 * - [Embedder]        → [AllMiniLmL6V2Embedder] (local ONNX)
 * - [VectorStore]     → [PgVectorStore] with [InMemoryVectorStore] fallback
 * - [MemoryStore]     → [InMemoryMemoryStore]
 * - [LlmClient]       → [GroqApiClient] or [GeminiApiClient] based on config
 * - [RagService]      → orchestrates the full ask() pipeline
 */
fun appModule(properties: Properties) = module {

    // ── Config ────────────────────────────────────────────────────
    single { properties }

    // ── Ingestion pipeline ────────────────────────────────────────
    single<DocumentLoader> { PDFBoxDocumentLoader() }
    single { RagPipeline(loader = get()) }
    single<Chunker>        { FixedSizeChunker(maxLength = 500, overlap = 50) }
    single<Embedder>       { AllMiniLmL6V2Embedder() }

    // DatabaseManager handles the PgVector → InMemoryVectorStore fallback
    single<VectorStore>    { DatabaseManager.setupVectorStore(get()) }
    single                 { FileTrackerRepository(dataSource = get()) }
    single                 { DocumentSyncService(get(), get(), get(), get(), get()) }

    // ── LLM client — provider selected at startup from config ─────
    single<LlmClient> {
        val p = get<Properties>()
        val provider = p.getProperty("llm.provider", "gemini").lowercase()
        if (provider == "groq") {
            GroqApiClient(
                apiKey    = p.getProperty("groq.api.key", ""),
                modelName = p.getProperty("groq.model", "llama3-8b-8192")
            )
        } else {
            GeminiApiClient(
                apiKey    = System.getenv("GEMINI_API_KEY") ?: p.getProperty("gemini.api.key", ""),
                modelName = p.getProperty("gemini.model", "gemini-1.5-flash")
            )
        }
    }

    // ── Service layer ─────────────────────────────────────────────
    single<MemoryStore>    { InMemoryMemoryStore() }

    // PromptTemplate is a stateless object — bind the singleton directly
    single                 { PromptTemplate }

    single                 { RagService(get(), get(), get(), get(), get()) }

    // Kafka
    single { KafkaProducerService(properties = get()) }
}

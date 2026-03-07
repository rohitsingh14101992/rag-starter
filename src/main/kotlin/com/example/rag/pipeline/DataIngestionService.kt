package com.example.rag.pipeline

import com.example.rag.core.ContentBlock
import com.example.rag.core.Embedder
import com.example.rag.core.VectorStore
import kotlinx.coroutines.flow.toList
import java.io.File

class DataIngestionService(
    private val pipeline: RagPipeline,
    private val chunker: FixedSizeChunker,
    private val embedder: Embedder,
    private val vectorStore: VectorStore
) {
    suspend fun ingestFolder(folderPath: String) {
        val filesFolder = File(folderPath)
        
        if (!filesFolder.exists() || !filesFolder.isDirectory) {
            println("Warning: '$folderPath' folder not found. Skipping indexing step.")
            return
        }

        println("Reading documents from ${filesFolder.absolutePath}...")
        
        try {
            val documents = pipeline.processFolder(filesFolder.absolutePath).toList()
            println("Found ${documents.size} documents. Chunking and embedding...")
            
            for (doc in documents) {
                // For each document, run every block through the chunker
                val allChunks = doc.blocks.flatMap { block -> chunker.chunk(block) }
                
                // Keep only TextBlocks for embedding right now
                val textBlocks = allChunks.filterIsInstance<ContentBlock.TextBlock>()
                
                if (textBlocks.isNotEmpty()) {
                    val embeddings = embedder.embedBatch(textBlocks)
                    vectorStore.storeBatch(textBlocks, embeddings)
                    println("Indexed ${textBlocks.size} chunks for document: ${doc.metadata["source"]}")
                }
            }
            println("Finished indexing all documents!")
        } catch (e: Exception) {
            println("Error indexing documents: ${e.message}")
        }
    }
}

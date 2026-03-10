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

        val allFiles = filesFolder.listFiles()?.filter { it.isFile } ?: emptyList()
        if (allFiles.isEmpty()) {
            println("No files found in ${filesFolder.absolutePath}.")
            return
        }

        // Filter out files already indexed — BEFORE reading any file from disk
        val newFiles = allFiles.filter { file ->
            val alreadyIndexed = vectorStore.isAlreadyIndexed(file.absolutePath)
            if (alreadyIndexed) println("Skipping already-indexed: ${file.name}")
            !alreadyIndexed
        }

        if (newFiles.isEmpty()) {
            println("All documents are already indexed. Nothing to do.")
            return
        }

        println("Found ${newFiles.size} new document(s) to index. Loading and embedding...")

        try {
            val documents = pipeline.processFiles(newFiles).toList()

            for (doc in documents) {
                val allChunks = doc.blocks.flatMap { block -> chunker.chunk(block) }
                val textBlocks = allChunks.filterIsInstance<ContentBlock.TextBlock>()

                if (textBlocks.isNotEmpty()) {
                    val embeddings = embedder.embedBatch(textBlocks)
                    vectorStore.storeBatch(textBlocks, embeddings)
                    println("Indexed ${textBlocks.size} chunks for: ${doc.sourcePath}")
                }
            }
            println("Finished indexing all new documents!")
        } catch (e: Exception) {
            println("Error indexing documents: ${e.message}")
        }
    }
}

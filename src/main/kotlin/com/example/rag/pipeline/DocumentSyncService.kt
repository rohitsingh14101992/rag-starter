package com.example.rag.pipeline

import com.example.rag.common.HashUtils
import com.example.rag.core.Chunker
import com.example.rag.core.ContentBlock
import com.example.rag.core.Embedder
import com.example.rag.core.VectorStore
import com.example.rag.db.FileTracker
import com.example.rag.db.FileTrackerRepository
import kotlinx.coroutines.flow.toList
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DocumentSyncService(
    private val pipeline: RagPipeline,
    private val chunker: Chunker,
    private val largeChunker: Chunker,
    private val embedder: Embedder,
    private val vectorStore: VectorStore,
    private val fileTrackerRepo: FileTrackerRepository
) {
    suspend fun syncFolder(folderPath: String) {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            println("Sync: Folder '$folderPath' not found.")
            return
        }

        val currentFiles = folder.listFiles()?.filter { it.isFile } ?: emptyList()
        val currentFilesMap = currentFiles.associateBy { it.absolutePath }
        
        val trackedFiles = fileTrackerRepo.getAll()
        val trackedFilesMap = trackedFiles.associateBy { it.filePath }

        // 1. Handle Deletions
        val deletedPaths = trackedFilesMap.keys - currentFilesMap.keys
        for (path in deletedPaths) {
            println("Sync: Deleting removed file: $path")
            vectorStore.deleteBySourceId(path)
            fileTrackerRepo.delete(path)
        }

        // 2. Handle New and Updated Files
        for (file in currentFiles) {
            val path = file.absolutePath
            val currentHash = HashUtils.calculateSha256(file)
            val tracked = trackedFilesMap[path]

            if (tracked == null || tracked.hash != currentHash) {
                if (tracked == null) {
                    println("Sync: Found new file: ${file.name}")
                } else {
                    println("Sync: Found updated file: ${file.name} (hash changed)")
                    vectorStore.deleteBySourceId(path)
                }

                try {
                    ingestFile(file)
                    val lastModified = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(file.lastModified()),
                        ZoneId.systemDefault()
                    )
                    fileTrackerRepo.upsert(FileTracker(path, currentHash, lastModified))
                    println("Sync: Successfully indexed ${file.name}")
                } catch (e: Exception) {
                    println("Sync: Error indexing ${file.name}: ${e.message}")
                }
            }
        }
    }

    private suspend fun ingestFile(file: File) {
        val documents = pipeline.processFiles(listOf(file)).toList()
        for (doc in documents) {
            // 1. Small chunks for vector embeddings
            val smallChunks = doc.blocks.flatMap { block -> chunker.chunk(block) }
            val textBlocks = smallChunks.filterIsInstance<ContentBlock.TextBlock>()

            if (textBlocks.isNotEmpty()) {
                val embeddings = embedder.embedBatch(textBlocks)
                vectorStore.storeBatch(textBlocks, embeddings)
            }

            // 2. Large chunks for keyword indexing (BM25)
            val largeChunks = doc.blocks.flatMap { block -> largeChunker.chunk(block) }
            val largeTextBlocks = largeChunks.filterIsInstance<ContentBlock.TextBlock>()
            if (largeTextBlocks.isNotEmpty()) {
                vectorStore.storeKeywords(largeTextBlocks)
            }
        }
    }
}

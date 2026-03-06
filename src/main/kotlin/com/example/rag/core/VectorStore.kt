package com.example.rag.core

/**
 * Stores and retrieves [ContentBlock]s with their vector embeddings.
 * Implementations should handle all block variants (TextBlock,
 * ImageBlock, TableBlock) appropriately when storing and returning results.
 */
interface VectorStore {
    suspend fun store(block: ContentBlock, embedding: FloatArray)
    suspend fun storeBatch(blocks: List<ContentBlock>, embeddings: List<FloatArray>)
    suspend fun search(queryEmbedding: FloatArray, limit: Int = 5): List<Pair<ContentBlock, Double>>
}



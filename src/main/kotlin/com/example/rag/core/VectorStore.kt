package com.example.rag.core

/**
 * Stores and retrieves [ContentBlock]s with their vector embeddings.
 * Implementations should handle all block variants (TextBlock,
 * ImageBlock, TableBlock) appropriately when storing and returning results.
 */
interface VectorStore {
    suspend fun store(block: ContentBlock, embedding: FloatArray)
    suspend fun storeBatch(blocks: List<ContentBlock>, embeddings: List<FloatArray>)
    suspend fun storeKeywords(blocks: List<ContentBlock>)
    suspend fun search(queryEmbedding: FloatArray, limit: Int = 5): List<Pair<ContentBlock, Double>>
    suspend fun hybridSearch(queryText: String, queryEmbedding: FloatArray, limit: Int = 5): List<Pair<ContentBlock, Double>>

    /**
     * Returns true if any chunks from the given sourceId are already stored.
     * Used to skip re-indexing files that were already processed in a previous run.
     */
    suspend fun isAlreadyIndexed(sourceId: String): Boolean

    /**
     * Deletes all vectors and associated content blocks for the given sourceId.
     */
    suspend fun deleteBySourceId(sourceId: String)
}



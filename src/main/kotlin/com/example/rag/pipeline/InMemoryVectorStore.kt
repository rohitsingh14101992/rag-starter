package com.example.rag.pipeline

import com.example.rag.core.ContentBlock
import com.example.rag.core.VectorStore
import kotlin.math.sqrt

/**
 * A simple, in-memory VectorStore for testing and running without external dependencies.
 * It computes exact cosine distances sequentially. Not suited for massive datasets,
 * but perfectly fine for a starter project's document base.
 */
class InMemoryVectorStore : VectorStore {
    
    // Store pairs of Block -> Embedding
    private val data = mutableListOf<Pair<ContentBlock, FloatArray>>()

    override suspend fun store(block: ContentBlock, embedding: FloatArray) {
        data.add(block to embedding)
    }

    override suspend fun storeBatch(blocks: List<ContentBlock>, embeddings: List<FloatArray>) {
        require(blocks.size == embeddings.size) { "blocks and embeddings must have the same size" }
        blocks.zip(embeddings).forEach { (block, embedding) ->
            data.add(block to embedding)
        }
    }

    override suspend fun search(queryEmbedding: FloatArray, limit: Int): List<Pair<ContentBlock, Double>> {
        if (data.isEmpty()) return emptyList()

        // Calculate cosine distance for every stored vector
        val scoredResults = data.map { (block, storedEmbedding) ->
            val distance = cosineDistance(queryEmbedding, storedEmbedding)
            block to distance
        }

        // Sort by distance (smallest is closest) and take the top N
        return scoredResults
            .sortedBy { it.second }
            .take(limit)
    }

    /**
     * Calculates the Cosine Distance between two vectors.
     * Distance range is 0.0 (identical) to 2.0 (opposite).
     * Exact equivalent to pgvector's <=> operator.
     */
    private fun cosineDistance(v1: FloatArray, v2: FloatArray): Double {
        require(v1.size == v2.size) { "Vectors must be of same dimensions" }
        
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        
        for (i in v1.indices) {
            val a = v1[i].toDouble()
            val b = v2[i].toDouble()
            dotProduct += a * b
            norm1 += a * a
            norm2 += b * b
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) return 1.0 // Fallback for zero-vectors
        
        val similarity = dotProduct / (sqrt(norm1) * sqrt(norm2))
        // Cosine similarity ranges from -1 to 1. 
        // Cosine distance is 1 - similarity (ranges from 0 to 2).
        return 1.0 - similarity
    }

    // In-memory store is always empty at startup — data doesn't survive restarts
    override suspend fun isAlreadyIndexed(sourceId: String): Boolean = false
}

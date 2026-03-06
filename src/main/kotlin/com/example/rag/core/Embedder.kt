package com.example.rag.core

/**
 * Converts a [ContentBlock] into a vector embedding.
 * Implementations can inspect the block variant to apply
 * different strategies (e.g. caption an [ContentBlock.ImageBlock],
 * serialise a [ContentBlock.TableBlock], embed text for [ContentBlock.TextBlock]).
 */
interface Embedder {
    suspend fun embed(block: ContentBlock): FloatArray
    suspend fun embedBatch(blocks: List<ContentBlock>): List<FloatArray>
}



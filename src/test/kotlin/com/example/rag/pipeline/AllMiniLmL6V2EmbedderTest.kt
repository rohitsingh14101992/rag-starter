package com.example.rag.pipeline

import com.example.rag.core.ContentBlock.TextBlock
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AllMiniLmL6V2EmbedderTest {

    @Test
    fun `test embedding single text block`() = runBlocking {
        val embedder = AllMiniLmL6V2Embedder()
        val text = "This is a test document."
        val block = TextBlock(text)

        val result = embedder.embed(block)
        
        // all-MiniLM-L6-v2 always outputs 384 dimensions
        assertEquals(384, result.size)
    }

    @Test
    fun `test embedding batch of text blocks`() = runBlocking {
        val embedder = AllMiniLmL6V2Embedder()
        val blocks = listOf(
            TextBlock("This is the first document."),
            TextBlock("This is the second document.")
        )

        val results = embedder.embedBatch(blocks)
        
        assertEquals(2, results.size)
        assertEquals(384, results[0].size)
        assertEquals(384, results[1].size)
        
        // the embeddings for two different texts should not be identical
        val isDifferent = results[0].zip(results[1]).any { (a, b) -> a != b }
        assertTrue(isDifferent, "Embeddings for different texts should be different")
    }
}

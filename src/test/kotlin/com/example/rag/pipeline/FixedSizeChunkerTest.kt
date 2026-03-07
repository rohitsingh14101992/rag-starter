package com.example.rag.pipeline

import com.example.rag.core.ContentBlock.ImageBlock
import com.example.rag.core.ContentBlock.TextBlock
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixedSizeChunkerTest {

    @Test
    fun `test chunking with exact length no overlap`() = runBlocking {
        val chunker = FixedSizeChunker(maxLength = 10, overlap = 0)
        val text = "12345678901234567890" // 20 chars
        val block = TextBlock(text)

        val result = chunker.chunk(block)
        
        assertEquals(2, result.size)
        assertEquals("1234567890", (result[0] as TextBlock).text)
        assertEquals("1234567890", (result[1] as TextBlock).text)
        assertEquals(0, result[0].metadata["chunk_index"])
        assertEquals(1, result[1].metadata["chunk_index"])
    }

    @Test
    fun `test chunking with overlap`() = runBlocking {
        val chunker = FixedSizeChunker(maxLength = 10, overlap = 5)
        val text = "12345678901234567890" // 20 chars
        val block = TextBlock(text)

        val result = chunker.chunk(block)
        
        // chunk 1: 0-10 -> "1234567890"
        // step: 10 - 5 = 5
        // chunk 2: 5-15 -> "6789012345"
        // step: 5
        // chunk 3: 10-20 -> "6789012345" (Wait, 67890 from first string, 12345 from second)
        
        assertEquals(3, result.size)
        assertEquals("1234567890", (result[0] as TextBlock).text)
        assertEquals("6789012345", (result[1] as TextBlock).text)
        assertEquals("1234567890", (result[2] as TextBlock).text)
    }

    @Test
    fun `test returns block as is if text length is less than max length`() = runBlocking {
        val chunker = FixedSizeChunker(maxLength = 10, overlap = 5)
        val text = "1234"
        val block = TextBlock(text)

        val result = chunker.chunk(block)
        
        assertEquals(1, result.size)
        assertEquals("1234", (result[0] as TextBlock).text)
    }

    @Test
    fun `test non-text blocks are returned unmodified`() = runBlocking {
        val chunker = FixedSizeChunker(maxLength = 10, overlap = 5)
        val block = ImageBlock("base64", "image/png")

        val result = chunker.chunk(block)
        
        assertEquals(1, result.size)
        assertTrue(result[0] === block)
    }
}

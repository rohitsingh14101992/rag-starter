package com.example.rag.pipeline

import com.example.rag.core.Chunker
import com.example.rag.core.ContentBlock
import com.example.rag.core.ContentBlock.TextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class FixedSizeChunker(
    private val maxLength: Int = 500,
    private val overlap: Int = 50
) : Chunker {

    init {
        require(maxLength > 0) { "maxLength must be greater than 0" }
        require(overlap >= 0) { "overlap must be non-negative" }
        require(overlap < maxLength) { "overlap must be strictly less than maxLength" }
    }

    override suspend fun chunk(block: ContentBlock): List<ContentBlock> = withContext(Dispatchers.Default) {
        when (block) {
            is TextBlock -> {
                val text = block.text
                if (text.isEmpty()) {
                    return@withContext listOf(block)
                }

                val chunks = mutableListOf<ContentBlock>()
                var startIndex = 0

                while (startIndex < text.length) {
                    val endIndex = min(startIndex + maxLength, text.length)
                    val chunkText = text.substring(startIndex, endIndex)
                    
                    // Add chunk index to metadata to preserve ordering if needed
                    val newMetadata = block.metadata.toMutableMap()
                    newMetadata["chunk_index"] = chunks.size
                    
                    chunks.add(TextBlock(text = chunkText, metadata = newMetadata))
                    
                    if (endIndex == text.length) {
                        break
                    }
                    
                    startIndex += (maxLength - overlap)
                }
                
                chunks
            }
            // For ImageBlock, TableBlock or other unforeseen blocks, return as is
            else -> listOf(block)
        }
    }
}

package com.example.rag.pipeline

import com.example.rag.core.ContentBlock
import com.example.rag.core.ContentBlock.TextBlock
import com.example.rag.core.Embedder
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AllMiniLmL6V2Embedder : Embedder {
    // This model encapsulates the ONNX runtime and loads the weights locally in process
    private val model = AllMiniLmL6V2EmbeddingModel()

    override suspend fun embed(block: ContentBlock): FloatArray = withContext(Dispatchers.IO) {
        when (block) {
            is TextBlock -> model.embed(block.text).content().vector()
            else -> throw IllegalArgumentException("AllMiniLmL6V2Embedder only supports TextBlock")
        }
    }

    override suspend fun embedBatch(blocks: List<ContentBlock>): List<FloatArray> = withContext(Dispatchers.IO) {
        val textBlocks = blocks.map { 
            require(it is TextBlock) { "AllMiniLmL6V2Embedder only supports TextBlock" }
            TextSegment.from(it.text)
        }
        if (textBlocks.isEmpty()) return@withContext emptyList()
        model.embedAll(textBlocks).content().map { it.vector() }
    }
}

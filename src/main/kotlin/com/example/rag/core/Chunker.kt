package com.example.rag.core

interface Chunker {
    suspend fun chunk(block: ContentBlock): List<ContentBlock>
}


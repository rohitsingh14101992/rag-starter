package com.example.rag.core

interface ContentExtractor {
    suspend fun extract(doc: Document): List<ContentBlock>
}

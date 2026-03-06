package com.example.rag.core

data class Document(
    val id: String,
    val sourcePath: String,
    val blocks: List<ContentBlock>,
    val metadata: Map<String, Any> = emptyMap()
)

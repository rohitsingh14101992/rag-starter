package com.example.rag.core

sealed interface ContentBlock {
    val metadata: Map<String, Any>

    data class TextBlock(
        val text: String,
        override val metadata: Map<String, Any> = emptyMap()
    ) : ContentBlock

    data class ImageBlock(
        val base64Data: String,
        val mimeType: String,
        override val metadata: Map<String, Any> = emptyMap()
    ) : ContentBlock

    data class TableBlock(
        val rawHtml: String,
        override val metadata: Map<String, Any> = emptyMap()
    ) : ContentBlock
}

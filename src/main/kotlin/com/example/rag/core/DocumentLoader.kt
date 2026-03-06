package com.example.rag.core

import java.io.InputStream

interface DocumentLoader {
    suspend fun load(inputStream: InputStream, sourceId: String): Document
}

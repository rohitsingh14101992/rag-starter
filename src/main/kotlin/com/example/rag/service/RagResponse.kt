package com.example.rag.service

import com.example.rag.core.ContentBlock

/**
 * Output from the RAG pipeline after a call to [RagService.ask].
 *
 * @property answer     The LLM-generated answer grounded in the retrieved context.
 * @property sources    The top-k chunks retrieved from the vector store, paired with
 *                      their cosine distance scores (lower = more similar).
 */
data class RagResponse(
    val answer: String,
    val sources: List<Pair<ContentBlock, Double>>
)

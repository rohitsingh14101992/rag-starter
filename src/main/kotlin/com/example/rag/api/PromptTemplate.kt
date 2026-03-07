package com.example.rag.api

import com.example.rag.core.ContentBlock

object PromptTemplate {

    /**
     * Constructs a prompt for the LLM using the user's question and the retrieved context.
     * 
     * @param question The user's original query
     * @param contextBlocks The list of relevant ContentBlocks retrieved from the VectorStore
     * @return A formatted strictly instructed prompt for the LLM
     */
    fun formatRagPrompt(question: String, contextBlocks: List<Pair<ContentBlock, Double>>): String {
        if (contextBlocks.isEmpty()) {
            return question
        }

        val contextStr = buildString {
            append("--- CONTEXT START ---\n")
            contextBlocks.forEachIndexed { index, (block, _) ->
                if (block is ContentBlock.TextBlock) {
                    val source = block.metadata["source"] ?: "Unknown"
                    val page = block.metadata["page"] ?: "?"
                    append("\n[Document $index | Source: $source | Page: $page]\n")
                    append("${block.text}\n")
                }
            }
            append("--- CONTEXT END ---\n")
        }

        return """
            You are a helpful and precise assistant. You have been provided with context from the user's internal documents.
            
            Please answer the user's question based ONLY on the context provided above.
            If the context does not contain the answer, politely state that you do not know based on the provided documents.
            Do not make up information or use outside knowledge.
            
            $contextStr
            
            User Question: $question
            
            Answer:
        """.trimIndent()
    }
}

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
                    val source = block.metadata["source_id"] ?: "Unknown"
                    val page = block.metadata["page_number"] ?: "?"
                    append("\n[Document ${index + 1} | Source: $source | Page: $page]\n")
                    append("${block.text}\n")
                }
            }
            append("--- CONTEXT END ---\n")
        }

        return """
            You are a helpful and precise assistant.
            
            The following context has been retrieved from the user's documents to help you answer their question:
            
            $contextStr
            
            Using ONLY the context provided above, answer the user's question.
            If the answer is not found in the context, say: "I could not find this information in the provided documents."
            Do not make up information or use outside knowledge.
            
            User Question: $question
            
            Answer:
        """.trimIndent()
    }
}

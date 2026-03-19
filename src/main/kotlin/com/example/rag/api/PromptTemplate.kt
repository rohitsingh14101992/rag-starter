package com.example.rag.api

import com.example.rag.core.ContentBlock

data class ConversationMessage(val role: String, val content: String)

/**
 * A structured, builder-based prompt template.
 *
 * Usage:
 *   val prompt = PromptTemplate.build {
 *       withContext(searchResults)
 *       withHistory(conversationHistory)
 *       withQuestion(userInput)
 *   }
 */
object PromptTemplate {

    // The master template — placeholders are filled in by the builder
    private val TEMPLATE = """
        You are a retrieval-based assistant. Your job is to answer the user's questions strictly
        using information retrieved from their documents. You do not have general knowledge access.

        Rules:
        - Answer ONLY from the retrieved context provided below.
        - If the context does not contain the answer, respond with:
          "I could not find this information in the provided documents."
        - Be concise and factual. Avoid filler phrases.
        - Where possible, reference which document or section your answer comes from.
        - Do NOT speculate, infer, or use outside knowledge.

        {{CONTEXT_SECTION}}
        {{HISTORY_SECTION}}
        User Question: {{QUESTION}}

        Answer:
    """.trimIndent()

    fun build(block: Builder.() -> Unit): String =
        Builder().apply(block).build()

    class Builder {
        private var contextBlocks: List<Pair<ContentBlock, Double>> = emptyList()
        private var history: List<ConversationMessage> = emptyList()
        private var question: String = ""

        fun withContext(blocks: List<Pair<ContentBlock, Double>>): Builder {
            this.contextBlocks = blocks
            return this
        }

        fun withHistory(messages: List<ConversationMessage>): Builder {
            this.history = messages
            return this
        }

        fun withQuestion(q: String): Builder {
            this.question = q
            return this
        }

        fun build(): String {
            val contextSection = buildContextSection()
            val historySection = buildHistorySection()

            return TEMPLATE
                .replace("{{CONTEXT_SECTION}}", contextSection)
                .replace("{{HISTORY_SECTION}}", historySection)
                .replace("{{QUESTION}}", question)
        }

        private fun buildContextSection(): String {
            if (contextBlocks.isEmpty()) return ""

            return buildString {
                appendLine("The following context has been retrieved from the user's documents:")
                appendLine()
                appendLine("--- CONTEXT START ---")
                contextBlocks.forEachIndexed { index, (block, _) ->
                    if (block is ContentBlock.TextBlock) {
                        val source = block.metadata["source_id"] ?: "Unknown"
                        val page = block.metadata["page_number"] ?: "?"
                        appendLine()
                        appendLine("[Document ${index + 1} | Source: $source | Page: $page]")
                        appendLine(block.text)
                    }
                }
                appendLine("--- CONTEXT END ---")
                appendLine()
                appendLine("Using ONLY the context above to answer. If the answer is not in the context, say: \"I could not find this information in the provided documents.\"")
            }
        }

        private fun buildHistorySection(): String {
            if (history.isEmpty()) return ""

            return buildString {
                appendLine("--- CONVERSATION HISTORY ---")
                history.forEach { msg ->
                    val label = if (msg.role == "user") "User" else "Assistant"
                    appendLine("$label: ${msg.content}")
                }
                appendLine("--- END OF HISTORY ---")
            }
        }
    }
}

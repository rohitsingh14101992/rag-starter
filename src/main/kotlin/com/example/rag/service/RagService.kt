package com.example.rag.service

import com.example.rag.api.ConversationMessage
import com.example.rag.api.PromptTemplate
import com.example.rag.core.ContentBlock
import com.example.rag.core.Embedder
import com.example.rag.core.LlmClient
import com.example.rag.core.MemoryStore
import com.example.rag.core.VectorStore

/**
 * Orchestrates a single question-answer turn in the RAG pipeline.
 *
 * Responsibilities:
 *  1. Embed the user's question.
 *  2. Retrieve the top-k most similar chunks from the [VectorStore].
 *  3. Build a grounded prompt (context + conversation history + question).
 *  4. Generate an answer via the [LlmClient].
 *  5. Persist the turn to [MemoryStore] for multi-turn continuity.
 *
 * This class is intentionally free of I/O wiring — it depends only on
 * abstractions from [com.example.rag.core], making it straightforward to test
 * by substituting fakes for any dependency.
 *
 * @param embedder        Converts text into a vector for similarity search.
 * @param vectorStore     Persists and retrieves content chunks by embedding.
 * @param llm             Generates a natural-language answer from a prompt.
 * @param promptTemplate  Assembles the final prompt from context and history.
 * @param memoryStore     Stores and retrieves conversation history.
 */
class RagService(
    private val embedder: Embedder,
    private val vectorStore: VectorStore,
    private val llm: LlmClient,
    private val promptTemplate: PromptTemplate,
    private val memoryStore: MemoryStore
) {

    /**
     * Answers [request.question] using the RAG pipeline.
     *
     * @param request A [RagQuery] containing the user's question and session ID.
     * @return A [RagResponse] with the LLM answer and the source chunks used.
     */
    suspend fun ask(request: RagQuery): RagResponse {
        // 1. Embed the question
        val queryEmbedding = embedder
            .embedBatch(listOf(ContentBlock.TextBlock(request.question)))
            .first()

        // 2. Retrieve top-k relevant chunks using Hybrid Search (Vector + Keyword)
        val sources = vectorStore.hybridSearch(
            queryText = request.question,
            queryEmbedding = queryEmbedding,
            limit = 3
        )

        // 3. Build grounded prompt
        val prompt = promptTemplate.build {
            withContext(sources)
            withHistory(memoryStore.getHistory())
            withQuestion(request.question)
        }

        // 4. Generate answer
        val answer = llm.generate(prompt)

        // 5. Persist this turn for multi-turn context
        memoryStore.add(ConversationMessage("user", request.question))
        memoryStore.add(ConversationMessage("assistant", answer))

        return RagResponse(answer = answer, sources = sources)
    }
}

package com.example.rag.pipeline

import com.example.rag.core.ContentBlock
import com.example.rag.core.ContentBlock.ImageBlock
import com.example.rag.core.ContentBlock.TableBlock
import com.example.rag.core.ContentBlock.TextBlock
import com.example.rag.core.VectorStore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.pgvector.PGvector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource

class PgVectorStore(
    private val dataSource: DataSource,
    private val tableName: String = "embeddings",
    private val dimensions: Int = 384
) : VectorStore {

    private val mapper = jacksonObjectMapper()

    init {
        // Security check: Prevent SQL injection by ensuring the table name only contains safe characters.
        // This is important because we interpolate $tableName directly into SQL strings below.
        require(tableName.matches(Regex("^[a-zA-Z0-9_]+$"))) { 
            "tableName must contain only alphanumeric characters and underscores to prevent SQL injection" 
        }
        
        dataSource.connection.use { conn ->
            // pgcrypto is required for the gen_random_uuid() function used as the primary key default
            conn.createStatement().execute("CREATE EXTENSION IF NOT EXISTS pgcrypto")
            // vector is the pgvector extension that enables the 'vector' data type and similarity search
            conn.createStatement().execute("CREATE EXTENSION IF NOT EXISTS vector")
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS $tableName (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    -- block_type helps us know if we should reconstruct a TextBlock, ImageBlock, etc.
                    block_type VARCHAR(50) NOT NULL,
                    text_content TEXT,
                    base64_data TEXT,
                    mime_type VARCHAR(100),
                    raw_html TEXT,
                    -- metadata is stored as JSONB so we can efficiently query arbitrary key-value pairs later
                    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
                    -- The actual vector embedding. 'dimensions' must match the output size of the embedding model
                    embedding vector($dimensions)
                )
            """.trimIndent())
            
            // Add vector index for performance with cosine distance
            // HNSW (Hierarchical Navigable Small World) is an advanced algorithm that makes searches 
            // extremely fast even with millions of rows, though building the index takes some time/memory.
            // 'vector_cosine_ops' tells the index we will be searching using cosine distance (<=> operator).
            conn.createStatement().execute("""
                CREATE INDEX IF NOT EXISTS ${tableName}_embedding_idx 
                ON $tableName USING hnsw (embedding vector_cosine_ops)
            """.trimIndent())
        }
    }

    private fun getConnection(): Connection {
        val conn = dataSource.connection
        PGvector.addVectorType(conn)
        return conn
    }

    override suspend fun store(block: ContentBlock, embedding: FloatArray) = withContext(Dispatchers.IO) {
        storeBatch(listOf(block), listOf(embedding))
    }

    override suspend fun storeBatch(blocks: List<ContentBlock>, embeddings: List<FloatArray>) = withContext(Dispatchers.IO) {
        require(blocks.size == embeddings.size) { "blocks and embeddings must have the same size" }
        if (blocks.isEmpty()) return@withContext

        getConnection().use { conn ->
            // We use a prepared statement (?) to safely insert dynamic data and prevent SQL injection attacks.
            // the metadata ?::jsonb tells Postgres to cast the JSON string into its native JSONB binary format.
            val sql = """
                INSERT INTO $tableName (block_type, text_content, base64_data, mime_type, raw_html, metadata, embedding) 
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                blocks.zip(embeddings).forEach { (block, embedding) ->
                    // Validate that the embedding model generated the exact number of dimensions our table expects.
                    // If this fails, the model configuration or table schema is incorrect.
                    require(embedding.size == dimensions) {
                        "Embedding size ${embedding.size} does not match expected $dimensions"
                    }
                    when (block) {
                        is TextBlock -> {
                            stmt.setString(1, "TEXT")
                            stmt.setString(2, block.text)
                            stmt.setNull(3, java.sql.Types.VARCHAR)
                            stmt.setNull(4, java.sql.Types.VARCHAR)
                            stmt.setNull(5, java.sql.Types.VARCHAR)
                        }
                        is ImageBlock -> {
                            stmt.setString(1, "IMAGE")
                            stmt.setNull(2, java.sql.Types.VARCHAR)
                            stmt.setString(3, block.base64Data)
                            stmt.setString(4, block.mimeType)
                            stmt.setNull(5, java.sql.Types.VARCHAR)
                        }
                        is TableBlock -> {
                            stmt.setString(1, "TABLE")
                            stmt.setNull(2, java.sql.Types.VARCHAR)
                            stmt.setNull(3, java.sql.Types.VARCHAR)
                            stmt.setNull(4, java.sql.Types.VARCHAR)
                            stmt.setString(5, block.rawHtml)
                        }
                    }
                    // Convert the metadata map into a JSON string using Jackson
                    stmt.setString(6, mapper.writeValueAsString(block.metadata))
                    
                    // Wrap the FloatArray in a PGvector object so the Postgres driver knows how to handle the vector type
                    stmt.setObject(7, PGvector(embedding))
                    
                    // Add this specific row to the batch of SQL statements waiting to be run
                    stmt.addBatch()
                }
                
                // Execute all the batched inserts in one network call for significantly better performance
                stmt.executeBatch()
                Unit // explicitly return Unit
            }
        }
    }

    override suspend fun search(queryEmbedding: FloatArray, limit: Int): List<Pair<ContentBlock, Double>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<ContentBlock, Double>>()
        getConnection().use { conn ->
            // <=> is the pgvector operator for "cosine distance".
            // It measures the angle between two vectors: smaller angle = more semantically similar.
            // By putting it in the ORDER BY clause, Postgres handles calculating and sorting the results for us natively.
            val sql = """
                SELECT block_type, text_content, base64_data, mime_type, raw_html, metadata, (embedding <=> ?) as distance
                FROM $tableName
                ORDER BY embedding <=> ?
                LIMIT ?
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                val vector = PGvector(queryEmbedding)
                stmt.setObject(1, vector)
                stmt.setObject(2, vector)
                stmt.setInt(3, limit)
                
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val distance = rs.getDouble("distance")
                        val blockType = rs.getString("block_type")
                        val metadataJson = rs.getString("metadata")
                        val metadata: Map<String, Any> = mapper.readValue(metadataJson)
                        
                        val block = when (blockType) {
                            "TEXT" -> TextBlock(rs.getString("text_content"), metadata)
                            "IMAGE" -> ImageBlock(rs.getString("base64_data"), rs.getString("mime_type"), metadata)
                            "TABLE" -> TableBlock(rs.getString("raw_html"), metadata)
                            else -> throw IllegalStateException("Unknown block_type: $blockType")
                        }
                        results.add(block to distance)
                    }
                }
            }
        }
        results
    }
    override suspend fun isAlreadyIndexed(sourceId: String): Boolean = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            // Check if any row exists with this source_id in the metadata JSONB column
            val sql = "SELECT 1 FROM $tableName WHERE metadata->>'source_id' = ? LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, sourceId)
                stmt.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    override suspend fun deleteBySourceId(sourceId: String): Unit = withContext(Dispatchers.IO) {
        getConnection().use { conn ->
            val sql = "DELETE FROM $tableName WHERE metadata->>'source_id' = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, sourceId)
                stmt.executeUpdate()
            }
        }
        Unit
    }
}

package com.example.rag.pipeline

import com.example.rag.core.ContentBlock.TextBlock
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(DockerImageName.parse(imageName))

@Ignore("Requires Docker to run PostgreSQL with pgvector")
class PgVectorStoreTest {

    companion object {
        private val postgres = KPostgreSQLContainer("pgvector/pgvector:pg16").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        private lateinit var dataSource: PGSimpleDataSource
        private lateinit var vectorStore: PgVectorStore

        @JvmStatic
        @BeforeClass
        fun setUp() {
            postgres.start()
            dataSource = PGSimpleDataSource().apply {
                setURL(postgres.jdbcUrl)
                user = postgres.username
                password = postgres.password
            }
            vectorStore = PgVectorStore(dataSource, dimensions = 3)
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            postgres.stop()
        }
    }

    @Test
    fun `test store and search text block`() = runBlocking {
        val block1 = TextBlock("Apple", mapOf("source" to "web"))
        val emb1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        
        val block2 = TextBlock("Banana", mapOf("source" to "local"))
        val emb2 = floatArrayOf(0.9f, 0.1f, 0.0f)
        
        val block3 = TextBlock("Car", mapOf("category" to "vehicle"))
        val emb3 = floatArrayOf(0.0f, 1.0f, 0.0f)
        
        vectorStore.storeBatch(listOf(block1, block2, block3), listOf(emb1, emb2, emb3))
        
        // Search near Apple
        val results = vectorStore.search(floatArrayOf(1.0f, 0.0f, 0.0f), limit = 2)
        
        assertEquals(2, results.size)
        // Cosine distance: 0 for identical, > 0 for further
        assertTrue(results[0].second < results[1].second)
        
        val retrievedBlock1 = results[0].first as TextBlock
        assertEquals("Apple", retrievedBlock1.text)
        assertEquals("web", retrievedBlock1.metadata["source"])
        
        val retrievedBlock2 = results[1].first as TextBlock
        assertEquals("Banana", retrievedBlock2.text)
        assertEquals("local", retrievedBlock2.metadata["source"])
    }
}

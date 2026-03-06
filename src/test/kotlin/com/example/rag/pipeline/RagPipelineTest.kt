package com.example.rag.pipeline

import com.example.rag.core.ContentBlock
import com.example.rag.core.Document
import com.example.rag.core.DocumentLoader
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RagPipelineTest {

    private lateinit var tempFolder: File

    // A mock loader that just returns a simple Document without actually parsing PDFs
    class MockDocumentLoader : DocumentLoader {
        override suspend fun load(inputStream: InputStream, sourceId: String): Document {
            return Document(
                id = "mock-id",
                sourcePath = sourceId,
                blocks = listOf(
                    ContentBlock.TextBlock("Mock content for \$sourceId")
                ),
                metadata = mapOf("loader" to "mock")
            )
        }
    }

    @Before
    fun setup() {
        // Create a temporary folder for our test files using the modern API 
        // to avoid the deprecated warning and ensure correct folder creation
        tempFolder = kotlin.io.path.createTempDirectory("rag-test-folder").toFile()
        
        // Create 5 dummy PDF files
        for (i in 1..5) {
            val file = File(tempFolder, "dummy_file_$i.pdf")
            file.writeText("fake pdf content")
        }
        
        // Create 1 unsupported text file to ensure it gets skipped
        val txtFile = File(tempFolder, "ignore_me.txt")
        txtFile.writeText("hello world")
    }

    @After
    fun teardown() {
        tempFolder.deleteRecursively()
    }

    @Test
    fun `processFolder reads all pdfs concurrently and ignores other files`() = runBlocking {
        // Arrange
        val pipeline = RagPipeline(MockDocumentLoader())

        // Act
        // Process the folder and collect all the emitted Documents into a List
        val results = pipeline.processFolder(tempFolder.absolutePath, concurrencyLimit = 3).toList()

        // Assert
        // We put 6 files in, but only 5 were PDFs, so we should only get 5 Documents out
        assertEquals(5, results.size, "Should have processed exactly 5 PDF files")
        
        // Ensure all loaded documents have the correct extension
        val allValid = results.all { doc -> doc.sourcePath.endsWith(".pdf") }
        assertTrue(allValid, "All processed documents should end with .pdf")
    }
}

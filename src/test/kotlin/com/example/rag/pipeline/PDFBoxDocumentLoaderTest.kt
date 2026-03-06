package com.example.rag.pipeline

import com.example.rag.core.ContentBlock
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PDFBoxDocumentLoaderTest {

    private lateinit var testPdfFile: File

    @Before
    fun setup() {
        // Create a real PDF document programmatically for testing
        testPdfFile = kotlin.io.path.createTempFile("test_rag_doc", ".pdf").toFile()
        
        val document = PDDocument()
        
        // Add Metadata
        val info = PDDocumentInformation()
        info.title = "Test RAG Document"
        info.author = "Test Author"
        document.documentInformation = info

        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)

        // Add Page 1
        val page1 = PDPage()
        document.addPage(page1)
        var contentStream = PDPageContentStream(document, page1)
        contentStream.beginText()
        contentStream.setFont(font, 12f)
        contentStream.newLineAtOffset(100f, 700f)
        contentStream.showText("This is page 1 text.")
        contentStream.endText()
        contentStream.close()
        
        // Add Page 2
        val page2 = PDPage()
        document.addPage(page2)
        contentStream = PDPageContentStream(document, page2)
        contentStream.beginText()
        contentStream.setFont(font, 12f)
        contentStream.newLineAtOffset(100f, 700f)
        contentStream.showText("This is page 2 text.")
        contentStream.endText()
        contentStream.close()

        document.save(testPdfFile)
        document.close()
    }

    @After
    fun teardown() {
        if (this::testPdfFile.isInitialized && testPdfFile.exists()) {
            testPdfFile.delete()
        }
    }

    @Test
    fun `loader extracts correct text and metadata page by page`() = runBlocking {
        // Arrange
        val loader = PDFBoxDocumentLoader()
        val sourceId = "test-source-id"

        // Act
        val document = FileInputStream(testPdfFile).use { inputStream ->
            loader.load(inputStream, sourceId)
        }

        // Assert - Document Level
        assertEquals(sourceId, document.sourcePath)
        assertEquals(2, document.metadata["pages"])
        assertEquals("Test RAG Document", document.metadata["title"])
        assertEquals("Test Author", document.metadata["author"])

        // Assert - Block Level (Page by Page extraction)
        assertEquals(2, document.blocks.size, "Should have exactly 2 content blocks (1 per page)")
        
        val block1 = document.blocks[0] as ContentBlock.TextBlock
        assertEquals("This is page 1 text.", block1.text)
        assertEquals(1, block1.metadata["page_number"])
        assertEquals(sourceId, block1.metadata["source_id"])
        assertEquals("Test RAG Document", block1.metadata["title"])
        
        val block2 = document.blocks[1] as ContentBlock.TextBlock
        assertEquals("This is page 2 text.", block2.text)
        assertEquals(2, block2.metadata["page_number"])
        assertEquals("Test Author", block2.metadata["author"])
    }
}

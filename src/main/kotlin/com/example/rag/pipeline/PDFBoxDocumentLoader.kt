package com.example.rag.pipeline

import com.example.rag.core.ContentBlock
import com.example.rag.core.Document
import com.example.rag.core.DocumentLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.io.File
import java.util.UUID

class PDFBoxDocumentLoader : DocumentLoader {

    override suspend fun load(inputStream: InputStream, sourceId: String): Document {
        return withContext(Dispatchers.IO) {
            
            val bytes = inputStream.readAllBytes()
            
            Loader.loadPDF(bytes).use { pdfDoc ->
                val totalPages = pdfDoc.numberOfPages
                val blocks = mutableListOf<ContentBlock>()
                val stripper = PDFTextStripper()
                
                // Get Document-Level Info First
                val info = pdfDoc.documentInformation
                val docTitle = info?.title ?: "Unknown"
                val docAuthor = info?.author ?: "Unknown"

                // Extract text page by page
                for (pageNumber in 1..totalPages) {
                    stripper.startPage = pageNumber
                    stripper.endPage = pageNumber
                    
                    val text = stripper.getText(pdfDoc).trim()
                    
                    if (text.isNotEmpty()) {
                        blocks.add(
                            ContentBlock.TextBlock(
                                text = text,
                                metadata = mapOf(
                                    "page_number" to pageNumber,
                                    "source_id" to sourceId,
                                    "title" to docTitle,
                                    "author" to docAuthor // Propagating down to the block level!
                                )
                            )
                        )
                    }
                }
                
                val metadata = mapOf(
                    "pages" to totalPages,
                    "title" to docTitle,
                    "author" to docAuthor,
                    "loader_type" to "pdfbox"
                )
                
                Document(
                    id = UUID.randomUUID().toString(),
                    sourcePath = sourceId,
                    blocks = blocks,
                    metadata = metadata
                )
            }
        }
    }
}

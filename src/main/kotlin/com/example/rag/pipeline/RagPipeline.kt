package com.example.rag.pipeline

import com.example.rag.core.Document
import com.example.rag.core.DocumentLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class RagPipeline(
    private val loader: DocumentLoader
) {
    /**
     * Reads all files from the given directory and creates Document objects concurrently.
     * @param folderPath The directory to read files from.
     * @param concurrencyLimit Maximum number of files to process in parallel at the same time.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun processFolder(folderPath: String, concurrencyLimit: Int = 4): Flow<Document> {
        val folder = File(folderPath)
        
        if (!folder.exists() || !folder.isDirectory) {
            throw IllegalArgumentException("Provided path is not a valid directory: \$folderPath")
        }

        val files = folder.listFiles()?.filter { it.isFile } ?: emptyList()

        return files.asFlow()
            // Process files concurrently up to the given limit
            .flatMapMerge(concurrency = concurrencyLimit) { file ->
                flow {
                    when (file.extension.lowercase()) {
                        "pdf" -> {
                            // Run the blocking IO and PDF parsing on a background thread pool
                            val doc = withContext(Dispatchers.IO) {
                                FileInputStream(file).use { inputStream ->
                                    loader.load(inputStream, file.absolutePath)
                                }
                            }
                            emit(doc)
                        }
                        else -> {
                            println("Skipping unsupported file type: \${file.name}")
                        }
                    }
                }
            }
    }
}

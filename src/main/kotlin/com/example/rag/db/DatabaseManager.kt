package com.example.rag.db

import com.example.rag.pipeline.InMemoryVectorStore
import com.example.rag.pipeline.PgVectorStore
import com.example.rag.core.VectorStore
import org.postgresql.ds.PGSimpleDataSource
import java.util.Properties

object DatabaseManager {
    fun setupVectorStore(properties: Properties): VectorStore {
        val dbHost = properties.getProperty("db.host", "localhost")
        val dbPort = properties.getProperty("db.port", "5432").toInt()
        val dbUser = properties.getProperty("db.user", "raguser")
        val dbPassword = properties.getProperty("db.password", "ragpass")
        val dbName = properties.getProperty("db.name", "ragdb")
        
        val dataSource = PGSimpleDataSource().apply {
            serverNames = arrayOf(dbHost)
            portNumbers = intArrayOf(dbPort)
            user = dbUser
            password = dbPassword
            databaseName = dbName
        }

        println("Attempting to connect to PostgreSQL at $dbHost:$dbPort...")
        try {
            val store = PgVectorStore(dataSource = dataSource, dimensions = 384)
            println("Successfully connected to PostgreSQL for Vector Storage!")
            return store
        } catch (e: Exception) {
            println("Failed to connect to PostgreSQL. Falling back to InMemoryVectorStore.")
            println("Reason: ${e.message}")
            return InMemoryVectorStore()
        }
    }
}

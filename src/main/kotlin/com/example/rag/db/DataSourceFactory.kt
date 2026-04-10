package com.example.rag.db

import org.postgresql.ds.PGSimpleDataSource
import java.util.Properties

object DataSourceFactory {
    fun create(properties: Properties): PGSimpleDataSource = PGSimpleDataSource().apply {
        serverNames = arrayOf(properties.getProperty("db.host", "localhost"))
        portNumbers = intArrayOf(properties.getProperty("db.port", "5432").toInt())
        user = properties.getProperty("db.user", "raguser")
        password = properties.getProperty("db.password", "ragpass")
        databaseName = properties.getProperty("db.name", "ragdb")
        assumeMinServerVersion = "9.0"
    }
}

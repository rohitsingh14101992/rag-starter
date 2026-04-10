package com.example.rag.db

import java.sql.ResultSet
import java.time.LocalDateTime
import javax.sql.DataSource

data class FileTracker(
    val filePath: String,
    val hash: String,
    val lastModified: LocalDateTime
)

class FileTrackerRepository(private val dataSource: DataSource) {

    init {
        dataSource.connection.use { conn ->
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS file_tracker (
                    file_path TEXT PRIMARY KEY,
                    hash TEXT NOT NULL,
                    last_modified TIMESTAMP NOT NULL
                )
            """.trimIndent())
        }
    }

    fun upsert(tracker: FileTracker) {
        val sql = """
            INSERT INTO file_tracker (file_path, hash, last_modified)
            VALUES (?, ?, ?)
            ON CONFLICT (file_path) DO UPDATE
            SET hash = EXCLUDED.hash, last_modified = EXCLUDED.last_modified
        """.trimIndent()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tracker.filePath)
                stmt.setString(2, tracker.hash)
                stmt.setObject(3, tracker.lastModified)
                stmt.executeUpdate()
            }
        }
    }

    fun getByPath(filePath: String): FileTracker? {
        val sql = "SELECT file_path, hash, last_modified FROM file_tracker WHERE file_path = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, filePath)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    fun getAll(): List<FileTracker> {
        val sql = "SELECT file_path, hash, last_modified FROM file_tracker"
        val list = mutableListOf<FileTracker>()
        dataSource.connection.use { conn ->
            conn.createStatement().executeQuery(sql).use { rs ->
                while (rs.next()) {
                    list.add(mapRow(rs))
                }
            }
        }
        return list
    }

    fun delete(filePath: String) {
        val sql = "DELETE FROM file_tracker WHERE file_path = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, filePath)
                stmt.executeUpdate()
            }
        }
    }

    private fun mapRow(rs: ResultSet): FileTracker {
        return FileTracker(
            filePath = rs.getString("file_path"),
            hash = rs.getString("hash"),
            lastModified = rs.getTimestamp("last_modified").toLocalDateTime()
        )
    }
}

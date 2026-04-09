package com.example.rag.db

import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDateTime
import javax.sql.DataSource

class ConversationRepository(private val dataSource: DataSource) {

    fun createConversation(conversation: Conversation): Conversation {
        val sql = """
            INSERT INTO conversations (id, user_id, title, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, conversation.id)
                stmt.setString(2, conversation.userId)
                stmt.setString(3, conversation.title)
                stmt.setObject(4, conversation.createdAt)
                stmt.setObject(5, conversation.updatedAt)
                stmt.executeUpdate()
            }
        }
        return conversation
    }

    fun getConversation(id: String): Conversation? {
        val sql = "SELECT id, user_id, title, created_at, updated_at FROM conversations WHERE id = ?"
        
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRowToConversation(rs) else null
                }
            }
        }
    }

    fun getUserConversations(userId: String): List<Conversation> {
        val sql = """
            SELECT id, user_id, title, created_at, updated_at 
            FROM conversations 
            WHERE user_id = ? 
            ORDER BY updated_at DESC
        """.trimIndent()
        
        val conversations = mutableListOf<Conversation>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        conversations.add(mapRowToConversation(rs))
                    }
                }
            }
        }
        return conversations
    }

    fun updateConversation(conversation: Conversation) {
        val sql = """
            UPDATE conversations 
            SET title = ?, updated_at = ? 
            WHERE id = ?
        """.trimIndent()
        
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, conversation.title)
                stmt.setObject(2, LocalDateTime.now())
                stmt.setString(3, conversation.id)
                stmt.executeUpdate()
            }
        }
    }

    fun deleteConversation(id: String) {
        val sql = "DELETE FROM conversations WHERE id = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.executeUpdate()
            }
        }
    }

    private fun mapRowToConversation(rs: ResultSet): Conversation {
        return Conversation(
            id = rs.getString("id"),
            userId = rs.getString("user_id"),
            title = rs.getString("title"),
            createdAt = rs.getObject("created_at", LocalDateTime::class.java),
            updatedAt = rs.getObject("updated_at", LocalDateTime::class.java)
        )
    }
}
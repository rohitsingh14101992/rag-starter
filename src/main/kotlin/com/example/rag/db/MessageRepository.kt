package com.example.rag.db

import java.sql.ResultSet
import java.time.LocalDateTime
import javax.sql.DataSource

class MessageRepository(private val dataSource: DataSource) {

    fun addMessage(message: Message): Message {
        val sql = """
            INSERT INTO messages (id, conversation_id, role, content, created_at)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, message.id)
                stmt.setString(2, message.conversationId)
                stmt.setString(3, message.role)
                stmt.setString(4, message.content)
                stmt.setObject(5, message.createdAt)
                stmt.executeUpdate()
            }
        }
        return message
    }

    fun getConversationMessages(conversationId: String): List<Message> {
        val sql = """
            SELECT id, conversation_id, role, content, created_at 
            FROM messages 
            WHERE conversation_id = ? 
            ORDER BY created_at ASC
        """.trimIndent()
        
        val messages = mutableListOf<Message>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, conversationId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        messages.add(mapRowToMessage(rs))
                    }
                }
            }
        }
        return messages
    }

    private fun mapRowToMessage(rs: ResultSet): Message {
        return Message(
            id = rs.getString("id"),
            conversationId = rs.getString("conversation_id"),
            role = rs.getString("role"),
            content = rs.getString("content"),
            createdAt = rs.getObject("created_at", LocalDateTime::class.java)
        )
    }
}
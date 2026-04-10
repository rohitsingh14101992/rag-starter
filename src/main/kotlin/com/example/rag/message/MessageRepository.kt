package com.example.rag.message

import com.example.rag.conversation.ConversationRepository
import java.sql.ResultSet
import java.time.LocalDateTime
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

class MessageRepository(private val dataSource: PGSimpleDataSource) {

    fun createMessage(message: Message): Message {
        val sql = """
            INSERT INTO messages (id, conversation_id, role, content, created_at)
            VALUES (?::uuid, ?::uuid, ?, ?, ?)
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

    fun getMessagesForConversation(conversationId: String, limit: Int = 50, offset: Int = 0): List<Message> {
        val sql = """
            SELECT id, conversation_id, role, content,
                   created_at AT TIME ZONE 'UTC' AS created_at
            FROM messages 
            WHERE conversation_id = ?::uuid 
            ORDER BY created_at ASC
            LIMIT ? OFFSET ?
        """.trimIndent()
        
        val messages = mutableListOf<Message>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, conversationId)
                stmt.setInt(2, limit)
                stmt.setInt(3, offset)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        messages.add(mapRowToMessage(rs))
                    }
                }
            }
        }
        return messages
    }

    fun countMessagesForConversation(conversationId: String): Int {
        val sql = "SELECT COUNT(*) FROM messages WHERE conversation_id = ?::uuid"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, conversationId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    private fun mapRowToMessage(rs: ResultSet): Message {
        return Message(
            id = rs.getString("id"),
            conversationId = rs.getString("conversation_id"),
            role = rs.getString("role"),
            content = rs.getString("content"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime()
        )
    }
}

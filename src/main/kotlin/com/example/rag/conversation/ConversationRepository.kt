package com.example.rag.conversation

import java.sql.ResultSet
import java.time.LocalDateTime
import org.postgresql.ds.PGSimpleDataSource

class ConversationRepository(private val dataSource: PGSimpleDataSource) {

    fun createConversation(conversation: Conversation): Conversation {
        val sql = """
            INSERT INTO conversations (id, user_id, title, created_at, updated_at)
            VALUES (?::uuid, ?::uuid, ?, ?, ?)
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
        val sql = """
            SELECT id, user_id, title,
                   created_at AT TIME ZONE 'UTC' AS created_at,
                   updated_at AT TIME ZONE 'UTC' AS updated_at
            FROM conversations WHERE id = ?::uuid
        """.trimIndent()
        
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRowToConversation(rs) else null
                }
            }
        }
    }

    fun getUserConversations(userId: String, limit: Int = 20, offset: Int = 0): List<Conversation> {
        val sql = """
            SELECT id, user_id, title,
                   created_at AT TIME ZONE 'UTC' AS created_at,
                   updated_at AT TIME ZONE 'UTC' AS updated_at
            FROM conversations 
            WHERE user_id = ?::uuid 
            ORDER BY updated_at DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        
        val conversations = mutableListOf<Conversation>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.setInt(2, limit)
                stmt.setInt(3, offset)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        conversations.add(mapRowToConversation(rs))
                    }
                }
            }
        }
        return conversations
    }

    fun countUserConversations(userId: String): Int {
        val sql = "SELECT COUNT(*) FROM conversations WHERE user_id = ?::uuid"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    fun updateConversation(conversation: Conversation) {
        val sql = """
            UPDATE conversations 
            SET title = ?, updated_at = ? 
            WHERE id = ?::uuid
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
        val sql = "DELETE FROM conversations WHERE id = ?::uuid"
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
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
        )
    }
}

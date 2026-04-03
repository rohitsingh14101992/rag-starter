package com.example.rag.auth

import org.postgresql.ds.PGSimpleDataSource
import java.util.UUID

data class User(
    val id: String,
    val email: String,
    val passwordHash: String,
    val isActive: Boolean
)

class UserRepository(private val dataSource: PGSimpleDataSource) {

    fun findByEmail(email: String): User? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT id, email, password_hash, is_active FROM users WHERE email = ?"
            ).use { stmt ->
                stmt.setString(1, email)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return User(
                            id           = rs.getObject("id", UUID::class.java).toString(),
                            email        = rs.getString("email"),
                            passwordHash = rs.getString("password_hash"),
                            isActive     = rs.getBoolean("is_active")
                        )
                    }
                }
            }
        }
        return null
    }

    fun updateLastLogin(userId: String) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE users SET last_login_at = NOW() WHERE id = ?::uuid"
            ).use { stmt ->
                stmt.setString(1, userId)
                stmt.executeUpdate()
            }
        }
    }
}

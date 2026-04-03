package com.example.rag.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.Properties

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val expiryMillis: Long
) {
    companion object {
        fun from(properties: Properties) = JwtConfig(
            secret      = properties.getProperty("jwt.secret",
                          "change-me-before-production-use-at-least-32-chars"),
            issuer      = properties.getProperty("jwt.issuer", "rag-starter"),
            expiryMillis = properties.getProperty("jwt.expiry.hours", "24").toLong() * 3_600_000
        )
    }
}

sealed class AuthResult {
    data class Success(val token: String) : AuthResult()
    data class Failure(val reason: String) : AuthResult()
}

class AuthService(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig
) {
    fun login(email: String, password: String): AuthResult {
        val user = userRepository.findByEmail(email)
            ?: return AuthResult.Failure("Invalid credentials")

        if (!user.isActive)
            return AuthResult.Failure("Account is disabled")

        val passwordMatch = BCrypt.verifyer()
            .verify(password.toCharArray(), user.passwordHash)
            .verified

        if (!passwordMatch)
            return AuthResult.Failure("Invalid credentials")

        userRepository.updateLastLogin(user.id)

        val token = JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withClaim("userId", user.id)
            .withClaim("email", user.email)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtConfig.expiryMillis))
            .sign(Algorithm.HMAC256(jwtConfig.secret))

        return AuthResult.Success(token)
    }
}

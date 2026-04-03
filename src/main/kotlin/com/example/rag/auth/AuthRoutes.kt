package com.example.rag.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

@Serializable
data class ErrorResponse(val error: String)

fun Application.authRoutes(authService: AuthService) {
    routing {
        post("/api/login") {
            val req = runCatching { call.receive<LoginRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                return@post
            }

            when (val result = authService.login(req.email.trim(), req.password)) {
                is AuthResult.Success ->
                    call.respond(HttpStatusCode.OK, LoginResponse(token = result.token))

                is AuthResult.Failure ->
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(result.reason))
            }
        }
    }
}

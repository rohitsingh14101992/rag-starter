package com.example.rag.server

import com.example.rag.auth.AuthService
import com.example.rag.auth.ErrorResponse
import com.example.rag.auth.authRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun createKtorServer(port: Int = 8080, authService: AuthService) =
    embeddedServer(Netty, port = port) {
        // JSON serialisation
        install(ContentNegotiation) { json() }

        // Allow the Compose Web frontend (localhost:3000 / any origin in dev) to call the API
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Get)
        }

        // Global error handler
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.application.log.error("Unhandled exception", cause)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Internal server error")
                )
            }
        }

        authRoutes(authService)
    }

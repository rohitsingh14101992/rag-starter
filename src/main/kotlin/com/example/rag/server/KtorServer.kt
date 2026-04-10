package com.example.rag.server

import com.example.rag.conversation.ConversationService
import com.example.rag.conversation.conversationRoutes
import com.example.rag.message.MessageService
import com.example.rag.message.messageRoutes
import com.example.rag.auth.AuthService
import com.example.rag.auth.ErrorResponse
import com.example.rag.auth.authRoutes
import com.example.rag.auth.JwtConfig
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun createKtorServer(
    port: Int = 8080,
    authService: AuthService,
    jwtConfig: JwtConfig,
    conversationService: ConversationService,
    messageService: MessageService,
    wsService: WebSocketResponseService
) =
    embeddedServer(Netty, port = port) {
        install(WebSockets)
        install(Authentication) {
            jwt("auth-jwt") {
                realm = "rag-starter"
                verifier(
                    com.auth0.jwt.JWT.require(com.auth0.jwt.algorithms.Algorithm.HMAC256(jwtConfig.secret))
                        .withIssuer(jwtConfig.issuer)
                        .build()
                )
                validate { credential ->
                    if (credential.payload.getClaim("userId").asString() != "") {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }

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
        routing {
            conversationRoutes(conversationService)
            messageRoutes(messageService)

            webSocket("/ws/responses/{conversationId}") {
                val conversationId = call.parameters["conversationId"] ?: return@webSocket close(
                    CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing conversationId")
                )
                
                try {
                    wsService.addSession(conversationId, this)
                    println("WebSocket session opened for conversation: $conversationId")
                    
                    // Keep the connection open until closed by client or error
                    for (frame in incoming) {
                        // We don't expect messages from client, but we need to consume to keep session alive
                    }
                } finally {
                    wsService.removeSession(conversationId, this)
                    println("WebSocket session closed for conversation: $conversationId")
                }
            }
        }
    }

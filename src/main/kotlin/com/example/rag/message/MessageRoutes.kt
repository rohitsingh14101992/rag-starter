package com.example.rag.message

import com.example.rag.api.ApiResponse
import com.example.rag.api.PaginationMeta
import com.example.rag.api.getLimit
import com.example.rag.api.offset
import com.example.rag.api.requireUserId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.messageRoutes(messageService: MessageService) {
    authenticate("auth-jwt") {
        route("/api/conversations/{id}/messages") {

            // GET /api/conversations/{id}/messages?limit=50&offset=0
            get {
                requireUserId { userId ->
                    val conversationId = call.parameters["id"]
                        ?: return@requireUserId call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Missing conversation id"))

                    val limit = call.getLimit(default = 50)
                    val offset = call.offset

                    when (val result = messageService.getMessagesForConversation(userId, conversationId, limit, offset)) {
                        is MessageResult.Success -> call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.ok(result.messages, PaginationMeta(limit, offset, result.total))
                        )
                        is MessageResult.NotFound -> call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<List<Message>>("Conversation not found")
                        )
                        is MessageResult.Forbidden -> call.respond(
                            HttpStatusCode.Forbidden,
                            ApiResponse.error<List<Message>>("Access denied")
                        )
                        is MessageResult.Failure -> call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse.error<List<Message>>(result.reason)
                        )
                        else -> Unit
                    }
                }
            }

            // POST /api/conversations/{id}/messages
            post {
                requireUserId { userId ->
                    val conversationId = call.parameters["id"]
                        ?: return@requireUserId call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Missing conversation id"))

                    val req = runCatching { call.receive<CreateMessageRequest>() }.getOrElse {
                        return@requireUserId call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))
                    }

                    if (req.role !in setOf("user", "assistant", "system")) {
                        return@requireUserId call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.error<Unit>("Role must be 'user', 'assistant', or 'system'")
                        )
                    }

                    when (val result = messageService.createMessage(userId, conversationId, req.role, req.content)) {
                        is MessageResult.SingleSuccess -> call.respond(
                            HttpStatusCode.Created,
                            ApiResponse.ok(result.message)
                        )
                        is MessageResult.NotFound -> call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse.error<Message>("Conversation not found")
                        )
                        is MessageResult.Forbidden -> call.respond(
                            HttpStatusCode.Forbidden,
                            ApiResponse.error<Message>("Access denied")
                        )
                        is MessageResult.Failure -> call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse.error<Message>(result.reason)
                        )
                        else -> Unit
                    }
                }
            }
        }
    }
}

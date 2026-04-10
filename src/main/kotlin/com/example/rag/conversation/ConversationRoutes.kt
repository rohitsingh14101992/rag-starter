package com.example.rag.conversation

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

fun Route.conversationRoutes(conversationService: ConversationService) {
    authenticate("auth-jwt") {
        route("/api/conversations") {

            // GET /api/conversations?limit=20&offset=0
            get {
                requireUserId { userId ->
                    val limit = call.getLimit(default = 20)
                    val offset = call.offset

                    when (val result = conversationService.getUserConversations(userId, limit, offset)) {
                        is ConversationResult.Success -> call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.ok(result.conversations, PaginationMeta(limit, offset, result.total))
                        )
                        is ConversationResult.Failure -> call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse.error<List<Conversation>>(result.reason)
                        )
                        else -> Unit
                    }
                }
            }

            // POST /api/conversations
            post {
                requireUserId { userId ->
                    val req = runCatching { call.receive<CreateConversationRequest>() }.getOrElse {
                        return@requireUserId call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))
                    }

                    when (val result = conversationService.createConversation(userId, req.title)) {
                        is ConversationResult.SingleSuccess -> call.respond(
                            HttpStatusCode.Created,
                            ApiResponse.ok(result.conversation)
                        )
                        is ConversationResult.Failure -> call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse.error<Conversation>(result.reason)
                        )
                        else -> Unit
                    }
                }
            }

            route("/{id}") {

                // PATCH /api/conversations/{id}
                patch {
                    requireUserId { userId ->
                        val conversationId = call.parameters["id"]
                            ?: return@requireUserId call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Missing conversation id"))

                        val req = runCatching { call.receive<UpdateConversationRequest>() }.getOrElse {
                            return@requireUserId call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Invalid request body"))
                        }

                        when (val result = conversationService.updateConversation(userId, conversationId, req.title)) {
                            is ConversationResult.SingleSuccess -> call.respond(
                                HttpStatusCode.OK,
                                ApiResponse.ok(result.conversation)
                            )
                            is ConversationResult.NotFound -> call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse.error<Conversation>("Conversation not found")
                            )
                            is ConversationResult.Forbidden -> call.respond(
                                HttpStatusCode.Forbidden,
                                ApiResponse.error<Conversation>("Access denied")
                            )
                            is ConversationResult.Failure -> call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse.error<Conversation>(result.reason)
                            )
                            else -> Unit
                        }
                    }
                }

                // DELETE /api/conversations/{id}
                delete {
                    requireUserId { userId ->
                        val conversationId = call.parameters["id"]
                            ?: return@requireUserId call.respond(HttpStatusCode.BadRequest, ApiResponse.error<Unit>("Missing conversation id"))

                        when (val result = conversationService.deleteConversation(userId, conversationId)) {
                            is ConversationResult.Deleted -> call.respond(
                                HttpStatusCode.NoContent
                            )
                            is ConversationResult.NotFound -> call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse.error<Unit>("Conversation not found")
                            )
                            is ConversationResult.Forbidden -> call.respond(
                                HttpStatusCode.Forbidden,
                                ApiResponse.error<Unit>("Access denied")
                            )
                            is ConversationResult.Failure -> call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse.error<Unit>(result.reason)
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

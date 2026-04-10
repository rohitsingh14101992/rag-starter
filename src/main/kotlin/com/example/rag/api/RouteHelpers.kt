package com.example.rag.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// ── JWT helpers ─────────────────────────────────────────────────────────────────

val ApplicationCall.userId: String?
    get() = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

/**
 * Executes [block] with the authenticated userId.
 * Automatically responds with 401 if the token is missing or invalid.
 */
suspend inline fun RoutingContext.requireUserId(block: (String) -> Unit) {
    val userId = call.userId
    if (userId == null) {
        call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<Unit>("Invalid or missing token"))
        return
    }
    block(userId)
}

// ── Pagination helpers ──────────────────────────────────────────────────────────

fun ApplicationCall.getLimit(default: Int = 20, max: Int = 100): Int =
    (request.queryParameters["limit"]?.toIntOrNull() ?: default).coerceIn(1, max)

val ApplicationCall.offset: Int
    get() = (request.queryParameters["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)

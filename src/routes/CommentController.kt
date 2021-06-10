package xyz.olympusblog.routes

import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import xyz.olympusblog.config.param
import xyz.olympusblog.config.userId
import xyz.olympusblog.models.CommentDTO
import xyz.olympusblog.service.CommentService
import xyz.olympusblog.utils.formatErrors
import xyz.olympusblog.validation.createCommentValidator

fun Route.comment(commentService: CommentService) {

    authenticate {

        post("/articles/{slug}/comments") {
            val slug = call.param("slug")
            val postComment = call.receive<CommentDTO>()
            val violations = createCommentValidator.validate(postComment)
            if (!violations.isValid) {
                call.respond(HttpStatusCode.BadRequest, formatErrors(violations))
            } else {
                val comment = commentService.addComment(call.userId(), slug, postComment)
                call.respond(comment)
            }
        }

        delete("/articles/{slug}/comments/{id}") {
            val slug = call.param("slug")
            val id = call.param("id").toInt()
            val comment = commentService.deleteComment(call.userId(), slug, id)
            call.respond(comment)
        }
    }

    authenticate(optional = true) {
        get("/articles/{slug}/comments") {
            val slug = call.param("slug")
            val userId = call.principal<UserIdPrincipal>()?.name?.toInt()
            val comments = commentService.getComments(userId, slug)
            call.respond(comments)
        }
    }
}
package xyz.olympusblog.config

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.principal
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import xyz.olympusblog.routes.article
import xyz.olympusblog.routes.auth
import xyz.olympusblog.routes.comment
import xyz.olympusblog.routes.profile
import xyz.olympusblog.service.*
import xyz.olympusblog.utils.ValidationException
import javax.naming.AuthenticationException

fun Route.api() {

    val authService = AuthService()
    val profileService = ProfileService()
    val articleService = ArticleService()
    val commentService = CommentService()

    route("/api") {
        auth(authService)
        profile(profileService)
        article(articleService)
        comment(commentService)
    }

    get("/drop") {
        DatabaseFactory.drop()
        call.respond("OK")
    }

}

fun ApplicationCall.userId() = principal<UserIdPrincipal>()?.name?.toInt() ?: throw AuthenticationException()

fun ApplicationCall.param(param: String) =
    parameters[param] ?: throw ValidationException(mapOf("param" to listOf("can't be empty")))
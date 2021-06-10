package xyz.olympusblog.config

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import xyz.olympusblog.utils.*

fun StatusPages.Configuration.statusPages() {
    exception<AuthenticationException> {
        call.respond(HttpStatusCode.Unauthorized)
    }
    exception<AuthorizationException> {
        call.respond(HttpStatusCode.Forbidden)
    }
    exception<ValidationException> { cause ->
        call.respond(
            HttpStatusCode.UnprocessableEntity,
            Errors(listOf(Errors.FormError(cause.params.toString(), cause.params.toString())))
        )
    }
    exception<UserExists> {
        call.respond(
            HttpStatusCode.UnprocessableEntity,
            Errors(listOf(Errors.FormError("user", "A user with that username or email already exists")))
        )
    }
    exception<UserDoesNotExists> {
        call.respond(HttpStatusCode.NotFound)
    }
    exception<ArticleDoesNotExist> {
        call.respond(HttpStatusCode.NotFound)
    }
    exception<MissingKotlinParameterException> { cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            Errors(listOf(Errors.FormError(cause.parameter.name.toString(), "${cause.parameter.name} cannot be empty")))
        )
    }
    exception<CommentNotFound> {
        call.respond(HttpStatusCode.NotFound)
    }
    exception<BadRequestException> { e ->
        call.respond(HttpStatusCode.BadRequest, ErrorDto(e.message.toString(), HttpStatusCode.BadRequest.value))
        throw e
    }
}

data class ErrorDto(val message: String, val errorCode: Int)
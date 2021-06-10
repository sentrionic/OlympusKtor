package xyz.olympusblog.routes

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import xyz.olympusblog.auth.CookieSession
import xyz.olympusblog.config.userId
import xyz.olympusblog.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.request.receiveMultipart
import io.ktor.routing.put
import io.ktor.sessions.clear
import xyz.olympusblog.models.*
import xyz.olympusblog.utils.formatErrors
import xyz.olympusblog.utils.handleMultipartFile
import xyz.olympusblog.validation.changePasswordValidator
import xyz.olympusblog.validation.registerValidator
import xyz.olympusblog.validation.resetPasswordValidator
import xyz.olympusblog.validation.updateUserValidator
import java.io.File

fun Route.auth(authService: AuthService) {

    post("/users") {
        val credentials = call.receive<RegisterDTO>()
        val violations = registerValidator.validate(credentials)
        if (!violations.isValid) {
            call.respond(HttpStatusCode.BadRequest, formatErrors(violations))
        } else {
            val newUser = authService.register(credentials)
            call.sessions.set(CookieSession(newUser.id.value))
            call.respond(UserResponse.fromUser(newUser))
        }
    }

    post("/users/login") {
        val credentials = call.receive<LoginDTO>()
        val user = authService.login(credentials)
        call.sessions.set(CookieSession(user.id.value))
        call.respond(UserResponse.fromUser(user))
    }

    post("/users/logout") {
        call.sessions.clear<CookieSession>()
        call.respond(HttpStatusCode.OK)
    }

    post("users/forgot-password") {
        val input = call.receive<ForgotPasswordDTO>()
        authService.forgotPassword(input.email)
        call.respond(HttpStatusCode.OK)
    }

    post("users/reset-password") {
        val input = call.receive<ResetPasswordDTO>()
        val violations = resetPasswordValidator.validate(input)
        if (!violations.isValid) {
            call.respond(HttpStatusCode.BadRequest, formatErrors(violations))
        } else {
            val user = authService.resetPassword(input)
            call.sessions.set(CookieSession(user.id.value))
            call.respond(UserResponse.fromUser(user))
        }
    }

    authenticate {
        get("/user") {
            val user = authService.getUserById(call.userId())
            call.respond(UserResponse.fromUser(user))
        }

        put("/user") {
            val multipart = call.receiveMultipart()
            var username: String? = null
            var email: String? = null
            var bio: String? = null
            var image: File? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "username" -> username = part.value
                            "email" -> email = part.value
                            "bio" -> bio = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        image = handleMultipartFile(part)
                    }
                }
                part.dispose
            }

            val updateUser = UpdateUser(
                email = email,
                username = username,
                bio = bio,
                image = image
            )

            val violations = updateUserValidator.validate(updateUser)
            if (!violations.isValid) {
                call.respond(HttpStatusCode.BadRequest, formatErrors(violations))
                image?.delete()
            } else {
                val user = authService.updateUser(call.userId(), updateUser)
                call.sessions.set(CookieSession(user.id.value))
                call.respond(UserResponse.fromUser(user))
            }
        }

        put("/users/change-password") {
            val input = call.receive<ChangePasswordDTO>()

            val violations = changePasswordValidator.validate(input)
            if (!violations.isValid) {
                call.respond(HttpStatusCode.BadRequest, formatErrors(violations))
            } else {
                val user = authService.changePassword(call.userId(), input)
                call.sessions.set(CookieSession(user.id.value))
                call.respond(UserResponse.fromUser(user))
            }
        }

    }
}
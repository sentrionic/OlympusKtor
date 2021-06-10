package xyz.olympusblog.routes

import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import xyz.olympusblog.config.param
import xyz.olympusblog.config.userId
import xyz.olympusblog.service.ProfileService

fun Route.profile(profileService: ProfileService) {

    authenticate(optional = true) {
        get("/profiles/{username}") {
            val username = call.param("username")
            val currentUserId = call.principal<UserIdPrincipal>()?.name?.toInt()
            val profile = profileService.getProfile(username, currentUserId)
            call.respond(profile)
        }

        get("/profiles") {
            val search = call.parameters["search"]
            val currentUserId = call.principal<UserIdPrincipal>()?.name?.toInt()
            val profiles = profileService.getProfiles(search, currentUserId)
            call.respond(profiles)
        }
    }

    authenticate {
        post("/profiles/{username}/follow") {
            val username = call.param("username")
            val currentUserId = call.userId()
            val profile = profileService.followProfile(username, currentUserId)
            call.respond(profile)
        }

        delete("/profiles/{username}/follow") {
            val username = call.param("username")
            val currentUserId = call.userId()
            val profile = profileService.unfollowProfile(username, currentUserId)
            call.respond(profile)
        }

    }
}
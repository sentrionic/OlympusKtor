package xyz.olympusblog

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.session
import io.ktor.config.HoconApplicationConfig
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.sessions.*
import io.ktor.features.*
import io.ktor.jackson.jackson
import io.ktor.util.hex
import org.slf4j.event.Level
import xyz.olympusblog.auth.CookieSession
import xyz.olympusblog.config.api
import xyz.olympusblog.config.cors
import xyz.olympusblog.config.statusPages
import xyz.olympusblog.service.DatabaseFactory
import xyz.olympusblog.utils.FileUpload

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {

    DatabaseFactory.init()
    FileUpload.init()

    install(DefaultHeaders)

    install(Locations)
    install(ConditionalHeaders)
    install(PartialContent)
    install(Compression) {
        default()
        excludeContentType(ContentType.Video.Any)
    }

    install(CORS) {
        cors()
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(Sessions) {
        val appConfig = HoconApplicationConfig(ConfigFactory.load())
        val secret = appConfig.property("ktor.deployment.secret_key").getString()
        val secretHashKey = hex(secret)
        cookie<CookieSession>("oBlog") {
            cookie.path = "/"
            cookie.extensions["SameSite"] = "lax"
            transform(SessionTransportTransformerMessageAuthentication(secretHashKey, "HmacSHA256"))
        }
    }

    install(Authentication) {
        session<CookieSession> {
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
            validate { session ->
                UserIdPrincipal(session.userId.toString())
            }
        }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    DatabaseFactory.init()

    routing {
        install(StatusPages) {
            statusPages()
        }
        api()
    }
}
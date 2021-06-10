package xyz.olympusblog.routes

import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.*
import xyz.olympusblog.config.param
import xyz.olympusblog.config.userId
import xyz.olympusblog.models.NewArticle
import xyz.olympusblog.models.UpdateArticle
import xyz.olympusblog.service.ArticleService
import xyz.olympusblog.utils.formatErrors
import xyz.olympusblog.utils.handleMultipartFile
import xyz.olympusblog.validation.createArticleValidator
import xyz.olympusblog.validation.updateArticleValidator
import java.io.File

fun Route.article(articleService: ArticleService) {

    suspend fun handleNewArticleMultipartRequest(multipart: MultiPartData): NewArticle {
        var title = ""
        var description = ""
        var body = ""
        val tagList = mutableListOf<String>()
        var image: File? = null

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    val value = part.value

                    when (part.name) {
                        "title" -> title = part.value
                        "description" -> description = part.value
                        "body" -> body = part.value
                        "tagList" -> tagList.add(value)
                    }
                }
                is PartData.FileItem -> {
                    image = handleMultipartFile(part)
                }
            }
            part.dispose
        }

        return NewArticle(
            title = title,
            description = description,
            body = body,
            image = image,
            tagList = tagList
        )
    }

    suspend fun handleUpdateArticleMultipartRequest(multipart: MultiPartData): UpdateArticle {
        var title: String? = null
        var description: String? = null
        var body: String? = null
        val tagList = mutableListOf<String>()
        var image: File? = null

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    val value = part.value

                    when (part.name) {
                        "title" -> title = part.value
                        "description" -> description = part.value
                        "body" -> body = part.value
                        "tagList" -> tagList.add(value)
                    }
                }
                is PartData.FileItem -> {
                    image = handleMultipartFile(part)
                }
            }
            part.dispose
        }

        return UpdateArticle(
            title = title,
            description = description,
            body = body,
            image = image,
            tagList = if (tagList.isEmpty()) null else tagList
        )
    }

    authenticate {
        post("/articles") {
            val multipart = call.receiveMultipart()
            val newArticle = handleNewArticleMultipartRequest(multipart)

            val violations = createArticleValidator.validate(newArticle)
            if (!violations.isValid) {
                call.respond(HttpStatusCode.BadRequest, formatErrors(violations))
                newArticle.image?.delete()
            } else {
                val article = articleService.createArticle(call.userId(), newArticle)
                call.respond(article)
            }
        }

        get("/articles/feed") {
            val params = call.parameters
            val filter = mapOf(
                "limit" to params["limit"],
                "p" to params["p"],
                "cursor" to params["cursor"]
            )
            call.respond(articleService.getFeed(call.userId(), filter))
        }

        get("/articles/bookmarked") {
            val params = call.parameters
            val filter = mapOf(
                "limit" to params["limit"],
                "p" to params["p"],
                "cursor" to params["cursor"]
            )
            call.respond(articleService.getBookmarked(call.userId(), filter))
        }

        put("/articles/{slug}") {
            val slug = call.param("slug")
            val multipart = call.receiveMultipart()
            val updateArticle = handleUpdateArticleMultipartRequest(multipart)
            val violations = updateArticleValidator.validate(updateArticle)
            if (!violations.isValid) {
                call.respond(HttpStatusCode.BadRequest, formatErrors(violations))
                updateArticle.image?.delete()
            } else {
                val article = articleService.updateArticle(call.userId(), slug, updateArticle)
                call.respond(article)
            }
        }

        delete("/articles/{slug}") {
            val slug = call.param("slug")
            val article = articleService.deleteArticle(call.userId(), slug)
            call.respond(HttpStatusCode.OK, article)
        }

        post("/articles/{slug}/favorite") {
            val slug = call.param("slug")
            val article = articleService.favoriteArticle(call.userId(), slug)
            call.respond(article)
        }

        delete("/articles/{slug}/favorite") {
            val slug = call.param("slug")
            val article = articleService.unfavoriteArticle(call.userId(), slug)
            call.respond(article)
        }

        post("/articles/{slug}/bookmark") {
            val slug = call.param("slug")
            val article = articleService.addBookmark(call.userId(), slug)
            call.respond(article)
        }

        delete("/articles/{slug}/bookmark") {
            val slug = call.param("slug")
            val article = articleService.removeBookmark(call.userId(), slug)
            call.respond(article)
        }
    }

    authenticate(optional = true) {

        get("/articles") {
            val userId = call.principal<UserIdPrincipal>()?.name?.toInt()
            val params = call.parameters
            val filter = mapOf(
                "tag" to params["tag"],
                "author" to params["author"],
                "favorited" to params["favorited"],
                "limit" to params["limit"],
                "p" to params["p"],
                "cursor" to params["cursor"],
                "search" to params["search"]
            )
            call.respond(articleService.getArticles(userId, filter))
        }

        get("/articles/{slug}") {
            val slug = call.param("slug")
            val currentUserId = call.principal<UserIdPrincipal>()?.name?.toInt()
            val article = articleService.getArticle(slug, currentUserId)
            call.respond(article)
        }

        get("/articles/tags") {
            call.respond(articleService.getAllTags())
        }
    }

}
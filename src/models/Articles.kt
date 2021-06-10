package xyz.olympusblog.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime
import java.io.File

object Articles : IntIdTable() {
    val slug = varchar("slug", 255).uniqueIndex()
    var title = varchar("title", 255)
    val description = varchar("description", 255)
    val body = text("body")
    val image = varchar("image", 255)
    val author = reference("author", Users)
    val createdAt = datetime("createdAt").clientDefault { DateTime.now() }
    val updatedAt = datetime("updatedAt").default(DateTime.now())
}

object Tags : IntIdTable() {
    val tagName = varchar("tagName", 255).uniqueIndex()
}

object ArticleTags : Table() {
    val article = reference(
        "article",
        Articles,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE,
    ).primaryKey(0)
    val tag = reference(
        "tag",
        Tags,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    ).primaryKey(1)
}

object FavoriteArticle : Table() {
    val article = reference("article", Articles).primaryKey(0)
    val user = reference("user", Users).primaryKey(1)
}

object BookmarkArticle : Table() {
    val article = reference("article", Articles).primaryKey(0)
    val user = reference("user", Users).primaryKey(1)
}

class Tag(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Tag>(Tags)

    var tag by Tags.tagName
}

class Article(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Article>(Articles)

    var slug by Articles.slug
    var title by Articles.title
    var description by Articles.description
    var body by Articles.body
    var image by Articles.image
    var tags by Tag via ArticleTags
    var author by Articles.author
    var favorited by User via FavoriteArticle
    var bookmarked by User via BookmarkArticle
    var createdAt by Articles.createdAt
    var updatedAt by Articles.updatedAt
    var comments by Comment via ArticleComment
}

data class NewArticle(
    val title: String,
    val description: String,
    val body: String,
    val image: File?,
    val tagList: List<String>
)

data class UpdateArticle(
    val title: String? = null,
    val description: String? = null,
    val body: String? = null,
    val image: File? = null,
    val tagList: List<String>? = null,
)

data class ArticleResponse(
    val id: Int,
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val image: String,
    val tagList: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val favorited: Boolean = false,
    val bookmarked: Boolean = false,
    val favoritesCount: Int = 0,
    val author: Profile
)

data class ArticleListResponse(val articles: List<ArticleResponse>, val hasMore: Boolean)

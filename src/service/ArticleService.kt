package xyz.olympusblog.service

import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import xyz.olympusblog.models.*
import xyz.olympusblog.service.DatabaseFactory.dbQuery
import xyz.olympusblog.utils.ArticleDoesNotExist
import xyz.olympusblog.utils.AuthorizationException
import xyz.olympusblog.utils.FileUpload
import java.util.*
import kotlin.random.Random

class ArticleService {

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    suspend fun createArticle(userId: Int, input: NewArticle): ArticleResponse {
        return dbQuery {
            val user = getUser(userId)
            var url = "https://picsum.photos/seed/${getRandomString(12)}/1080"

            if (input.image != null) {
                val directory = "ktor/${userId}/${getRandomString(16)}"
                url = FileUpload.uploadArticleImage(input.image, directory)
            }

            val article = Article.new {
                title = input.title
                slug = generateSlug(input.title)
                description = input.description
                body = input.body
                image = url
                author = user.id
            }

            val tags = input.tagList.map { tag -> getOrCreateTag(tag) }
            article.tags = SizedCollection(tags)

            getArticleResponse(article, user)
        }
    }

    suspend fun updateArticle(userId: Int, slug: String, input: UpdateArticle): ArticleResponse {
        return dbQuery {
            val user = getUser(id = userId)
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)
            if (!isArticleAuthor(article, user)) throw AuthorizationException()

            article.apply {
                title = input.title ?: title
                description = input.description ?: description
                body = input.body ?: body
                updatedAt = DateTime.now()
            }

            if (input.image != null) {
                val directory = "ktor/${userId}/${getRandomString(16)}"
                article.image = FileUpload.uploadArticleImage(input.image, directory)
            }

            if (input.tagList != null) {
                val tags = input.tagList.map { tag -> getOrCreateTag(tag) }
                article.tags = SizedCollection(tags)
            }

            getArticleResponse(article, user)
        }
    }

    suspend fun deleteArticle(userId: Int, slug: String): ArticleResponse {
        return dbQuery {
            val user = getUser(id = userId)
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)
            if (!isArticleAuthor(article, user)) throw AuthorizationException()
            article.delete()
            getArticleResponse(article, user)
        }
    }

    suspend fun getArticle(slug: String, userId: Int?): ArticleResponse {
        return dbQuery {
            val user = if (userId != null) getUser(userId) else null
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)
            getArticleResponse(article, user)
        }
    }

    suspend fun getAllTags(): List<String> {
        return dbQuery {
            Tag.all().limit(20).map { it.tag }
        }
    }

    suspend fun getArticles(userId: Int?, filter: Map<String, String?>): ArticleListResponse {
        return dbQuery {
            val user = if (userId != null) getUser(userId) else null
            getAllArticles(
                currentUser = user,
                tag = filter["tag"],
                authorUserName = filter["author"],
                favoritedByUserName = filter["favorited"],
                cursor = filter["cursor"],
                search = filter["search"],
                limit = filter["limit"]?.toInt() ?: 10,
                page = filter["p"]?.toInt() ?: 0
            )
        }
    }

    suspend fun getFeed(userId: Int, filter: Map<String, String?>): ArticleListResponse {
        return dbQuery {
            val user = getUser(userId)
            getAllArticles(
                currentUser = user,
                limit = filter["limit"]?.toInt() ?: 10,
                page = filter["p"]?.toInt() ?: 0,
                cursor = filter["cursor"],
                follows = true
            )
        }
    }

    private fun getAllArticles(
        currentUser: User? = null,
        tag: String? = null,
        authorUserName: String? = null,
        favoritedByUserName: String? = null,
        search: String? = null,
        limit: Int = 10,
        page: Int = 0,
        cursor: String? = null,
        order: String? = "DESC",
        follows: Boolean = false,
        bookmarked: Boolean = false
    ): ArticleListResponse {

        val realLimitPlusOne = limit + 1

        val query = Articles.innerJoin(ArticleTags).leftJoin(FavoriteArticle)
            .slice(Articles.columns)
            .selectAll()
            .withDistinct()

        if (authorUserName != null) {
            val author = findByUsername(authorUserName) ?: return ArticleListResponse(
                emptyList(),
                false
            )
            query.andWhere { Articles.author eq author.id }
        }

        if (favoritedByUserName != null) {
            val favoritedBy = findByUsername(favoritedByUserName) ?: return ArticleListResponse(emptyList(), false)
            query.andWhere { FavoriteArticle.user eq favoritedBy.id }
        }

        if (bookmarked && currentUser != null) {
            query.andWhere { BookmarkArticle.user eq currentUser.id }
        }

        if (tag != null) {
            val tagName = Tag.find { Tags.tagName like tag.toLowerCase() }.firstOrNull() ?: return ArticleListResponse(
                emptyList(),
                false
            )
            query.andWhere { ArticleTags.tag eq tagName.id }
        }

        if (follows && currentUser != null) {
            val following = currentUser.followee.map { it.id.value }
            query.andWhere { Articles.author.inList(following) }
        }

        if (cursor != null) {
            query.andWhere { Articles.createdAt lessEq Date(cursor) }
        }

        if (search != null) {
            val term = search.toLowerCase()
            query.andWhere { (Articles.title like term) or (Articles.description like term) }
        }

        when (order) {
            "ASC" -> query.orderBy(Articles.createdAt to SortOrder.ASC)
            "TOP" -> query.orderBy(FavoriteArticle.article.count() to SortOrder.DESC)
            else -> query.orderBy(Articles.createdAt to SortOrder.DESC)
        }

        val skip = (page - 1).coerceAtLeast(0)

        query.limit(realLimitPlusOne, (limit * skip).toLong())

        val articles = Article.wrapRows(query).toList()

        return ArticleListResponse(
            articles.take(limit).map { getArticleResponse(it, currentUser) },
            articles.size == realLimitPlusOne
        )
    }

    suspend fun favoriteArticle(userId: Int, slug: String): ArticleResponse {
        return dbQuery {
            val user = getUser(userId)
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)

            if (!isArticleFavorited(article, user)) {
                article.favorited = SizedCollection(article.favorited.plus(user))
            }

            getArticleResponse(article, user)
        }
    }

    suspend fun unfavoriteArticle(userId: Int, slug: String): ArticleResponse {
        return dbQuery {
            val user = getUser(userId)
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)

            if (isArticleFavorited(article, user)) {
                article.favorited = SizedCollection(article.favorited.minus(user))
            }

            getArticleResponse(article, user)
        }
    }

    suspend fun getBookmarked(userId: Int, filter: Map<String, String?>): ArticleListResponse {
        return dbQuery {
            val user = getUser(userId)
            getAllArticles(
                currentUser = user,
                limit = filter["limit"]?.toInt() ?: 10,
                page = filter["p"]?.toInt() ?: 0,
                cursor = filter["cursor"],
                follows = true
            )
        }
    }

    suspend fun addBookmark(userId: Int, slug: String): ArticleResponse {
        return dbQuery {
            val user = getUser(userId)
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)

            if (!isArticleBookmarked(article, user)) {
                article.bookmarked = SizedCollection(article.bookmarked.plus(user))
            }

            getArticleResponse(article, user)
        }
    }

    suspend fun removeBookmark(userId: Int, slug: String): ArticleResponse {
        return dbQuery {
            val user = getUser(userId)
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)

            if (isArticleBookmarked(article, user)) {
                article.bookmarked = SizedCollection(article.bookmarked.minus(user))
            }

            getArticleResponse(article, user)
        }
    }

    private fun getOrCreateTag(tagName: String) =
        Tag.find { Tags.tagName eq tagName }.firstOrNull() ?: Tag.new { this.tag = tagName }

    private fun getArticleResponse(article: Article, currentUser: User? = null): ArticleResponse {
        val author = getUser(article.author.value)
        val tagList = article.tags.map { it.tag }
        val favoritesCount = article.favorited.count().toInt()
        val favorited = isArticleFavorited(article, currentUser)
        val bookmarked = isArticleBookmarked(article, currentUser)
        val following = isFollower(author, currentUser)
        val authorProfile = getProfileByUser(getUser(article.author.value), following)
        return ArticleResponse(
            id = article.id.value,
            slug = article.slug,
            title = article.title,
            description = article.description,
            body = article.body,
            tagList = tagList,
            image = article.image,
            createdAt = article.createdAt.toString(),
            updatedAt = article.updatedAt.toString(),
            favorited = favorited,
            favoritesCount = favoritesCount,
            bookmarked = bookmarked,
            author = authorProfile
        )
    }

    private fun generateSlug(title: String): String {
        return title.toLowerCase()
            .replace("\n", " ")
            .replace("[^a-z\\d\\s]".toRegex(), " ")
            .split(" ")
            .joinToString("-")
            .replace("-+".toRegex(), "-")
            .plus("-")
            .plus(getRandomString(6))
    }

    private fun getRandomString(size: Int): String {
        return (1..size)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun isArticleAuthor(article: Article, user: User) = article.author == user.id
}

fun isArticleFavorited(article: Article, user: User?) =
    if (user != null) article.favorited.any { it == user } else false

fun isArticleBookmarked(article: Article, user: User?) =
    if (user != null) article.bookmarked.any { it == user } else false
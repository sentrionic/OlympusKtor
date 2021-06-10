package xyz.olympusblog.service

import org.jetbrains.exposed.sql.SizedCollection
import xyz.olympusblog.models.*
import xyz.olympusblog.service.DatabaseFactory.dbQuery
import xyz.olympusblog.utils.ArticleDoesNotExist
import xyz.olympusblog.utils.AuthorizationException
import xyz.olympusblog.utils.CommentNotFound

class CommentService {

    suspend fun addComment(userId: Int, slug: String, input: CommentDTO): CommentResponse {
        return dbQuery {
            val user = getUser(userId)
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)
            val comment = Comment.new {
                body = input.body
                author = user.id
            }
            article.comments = SizedCollection(article.comments.plus(comment))
            getCommentResponse(comment, userId)
        }
    }

    suspend fun getComments(userId: Int?, slug: String): List<CommentResponse> {
        return dbQuery {
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)
            article.comments.map { comment -> getCommentResponse(comment, userId) }
        }
    }

    suspend fun deleteComment(userId: Int, slug: String, commentId: Int) {
        return dbQuery {
            val user = getUser(userId)
            val article = Article.find { Articles.slug eq slug }.firstOrNull() ?: throw ArticleDoesNotExist(slug)
            val comment = Comment.findById(commentId) ?: throw CommentNotFound()
            article.comments = SizedCollection(article.comments.plus(comment))
            if (comment.author != user.id || article.comments.none { it == comment }) throw AuthorizationException()
            comment.delete()
            getCommentResponse(comment, userId)
        }
    }

}

fun getCommentResponse(comment: Comment, userId: Int?): CommentResponse {
    val author = getUser(comment.author.value)
    val currentUser = if (userId != null) getUser(userId) else null
    val following = isFollower(author, currentUser)
    val authorProfile = getProfileByUser(author, following)
    return CommentResponse(
        id = comment.id.value,
        createdAt = comment.createdAt.toString(),
        updatedAt = comment.updatedAt.toString(),
        body = comment.body,
        author = authorProfile
    )
}
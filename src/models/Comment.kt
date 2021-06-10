package xyz.olympusblog.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime

object Comments : IntIdTable() {
    val body = text("body")
    val author = reference("author", Users, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("createdAt").default(DateTime.now())
    val updatedAt = datetime("updatedAt").default(DateTime.now())
}

object ArticleComment : Table() {
    val article = reference(
        "article", Articles,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    ).primaryKey(0)
    val comment = reference(
        "comment", Comments,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    ).primaryKey(1)
}

class Comment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Comment>(Comments)

    var body by Comments.body
    var author by Comments.author
    var createdAt by Comments.createdAt
    var updatedAt by Comments.updatedAt
}

data class CommentResponse(
    val id: Int,
    val createdAt: String,
    val updatedAt: String,
    val body: String,
    val author: Profile
)

data class CommentDTO(val body: String)
package xyz.olympusblog.models

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime
import java.io.File

object Users : IntIdTable() {
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 255).uniqueIndex()
    val bio = text("bio").default("")
    val image = varchar("image", 255).default("")
    val password = varchar("password", 255)
    val createdAt = datetime("createdAt").default(DateTime.now())
    val updatedAt = datetime("updatedAt").default(DateTime.now())
}

object Followings : IntIdTable() {
    val followeeId = reference("followeeId", Users)
    val followerId = reference("followerId", Users)
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var email by Users.email
    var username by Users.username
    var bio by Users.bio
    var image by Users.image
    var password by Users.password
    var createdAt by Users.createdAt
    var updatedAt by Users.updatedAt
    var followers by User.via(Followings.followeeId, Followings.followerId)
    var followee by User.via(Followings.followerId, Followings.followeeId)
}

data class RegisterDTO(
    val email: String,
    val username: String,
    val password: String
)

data class LoginDTO(val email: String, val password: String)

data class UpdateUser(
    val email: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val image: File? = null
)

data class ChangePasswordDTO(
    val currentPassword: String,
    val newPassword: String,
    val confirmNewPassword: String
)

data class ForgotPasswordDTO(
    val email: String
)

data class ResetPasswordDTO(
    val token: String,
    val newPassword: String,
    val confirmNewPassword: String
)

data class UserResponse(
    val id: Int,
    val email: String,
    val username: String,
    val bio: String,
    val image: String?,
    val createdAt: String,
    val updatedAt: String,
) {

    companion object {
        fun fromUser(user: User): UserResponse = UserResponse(
            id = user.id.value,
            email = user.email,
            username = user.username,
            bio = user.bio,
            image = user.image,
            createdAt = user.createdAt.toString(),
            updatedAt = user.updatedAt.toString(),
        )
    }
}
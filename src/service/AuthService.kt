package xyz.olympusblog.service

import io.ktor.features.BadRequestException
import io.ktor.features.NotFoundException
import io.lettuce.core.SetArgs
import org.jetbrains.exposed.sql.or
import xyz.olympusblog.auth.hash
import xyz.olympusblog.config.redis
import xyz.olympusblog.models.*
import xyz.olympusblog.service.DatabaseFactory.dbQuery
import xyz.olympusblog.utils.*
import java.security.MessageDigest
import java.util.*
import kotlin.text.Charsets.UTF_8

class AuthService {

    val hashFunction = { s: String -> hash(s) }

    suspend fun register(credentials: RegisterDTO): User {
        return dbQuery {
            val user = User.find { (Users.username eq credentials.username) or (Users.email eq credentials.email) }.firstOrNull()

            if (user != null) throw UserExists()

            val gravatar = "https://gravatar.com/avatar/${md5(credentials.email).toHex()}?d=identicon";

            User.new {
                username = credentials.username.trim()
                email = credentials.email.toLowerCase()
                password = hashFunction(credentials.password)
                image = gravatar
            }
        }
    }

    suspend fun login(credentials: LoginDTO): User {
        return dbQuery {
            val user = User.find { (Users.email eq credentials.email) }.firstOrNull()?: throw NotFoundException()

            if (user.password != hashFunction(credentials.password)) {
                throw UserDoesNotExists()
            }

            user
        }
    }

    suspend fun updateUser(userId: Int, body: UpdateUser): User {

        return dbQuery {
            val user = getUser(userId)

            if (user.email != body.email) {
                val exists = User.find { Users.email eq body.email!! }.firstOrNull()
                if (exists != null) throw UserExists()
            }

            if (user.username != body.username) {
                val exists = User.find { Users.username eq body.username!! }.firstOrNull()
                if (exists != null) throw UserExists()
            }

            var url: String? = null
            if (body.image != null) {
                val directory = "ktor/${userId}/avatar";
                url = FileUpload.uploadAvatarImage(body.image, directory)
            }

            user.apply {
                email = body.email ?: email
                username = body.username ?: username
                image = url ?: image
                bio = body.bio ?: bio
            }
        }
    }

    suspend fun getUserById(id: Int): User {
        return dbQuery {
            getUser(id)
        }
    }

    suspend fun changePassword(userId: Int, input: ChangePasswordDTO): User {

        return dbQuery {
            val user = getUser(userId)
            user.apply {
                password = hashFunction(input.newPassword)
            }
        }
    }

    suspend fun forgotPassword(email: String): Boolean {
        return dbQuery {
            val user = User.find { Users.email eq email }.firstOrNull()?: throw UserDoesNotExists()

            val token = UUID.randomUUID()
            redis.set("forget-password:${token}", user.id.value.toString(), SetArgs().ex(1000 * 60 * 60 * 24 * 3))
            Email.sendEmail(user.email, "<a href=\"localhost:3000/reset-password/${token}\">Reset Password</a>")
            true
        }
    }

    suspend fun resetPassword(input: ResetPasswordDTO): User {
        return dbQuery {
            val key = "forget-password:${input.token}"
            val value = redis.get(key) ?: throw BadRequestException("Token Expired")
            val userId = value.toInt()

            val user = User.find { Users.id eq userId }.firstOrNull() ?: throw UserDoesNotExists()

            user.apply {
                password = hashFunction(input.newPassword)
            }

            redis.del(key)

            user
        }
    }
}

fun getUser(id: Int) = User.findById(id) ?: throw UserDoesNotExists()

fun md5(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))
fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
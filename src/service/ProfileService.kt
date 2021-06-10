package xyz.olympusblog.service

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.or
import xyz.olympusblog.models.Profile
import xyz.olympusblog.models.User
import xyz.olympusblog.models.Users
import xyz.olympusblog.service.DatabaseFactory.dbQuery
import xyz.olympusblog.utils.UserDoesNotExists

class ProfileService {

    suspend fun getProfile(username: String, currentUserId: Int? = null): Profile {
        return dbQuery {
            val profile = findByUsername(username) ?: throw UserDoesNotExists()
            currentUserId ?: return@dbQuery getProfileByUser(profile)
            val currentUser = getUser(currentUserId)
            val isFollowing = isFollower(profile, currentUser)
            getProfileByUser(profile, isFollowing)
        }
    }

    suspend fun getProfiles(search: String?, currentUserId: Int? = null): List<Profile> {
        return dbQuery {
            val profiles = User.find {
                if (search != null) (Users.username like search.toLowerCase()) or (Users.bio like search.toLowerCase())
                else Op.TRUE
            }
            .limit(20)

            currentUserId ?: return@dbQuery profiles.map { getProfileByUser(it) }
            val currentUser = getUser(currentUserId)
            profiles.map {
                val isFollowing = isFollower(it, currentUser)
                getProfileByUser(it, isFollowing)
            }
        }
    }

    suspend fun followProfile(username: String, currentUserId: Int): Profile {
        dbQuery {
            val profile = findByUsername(username)?: throw UserDoesNotExists()
            val currentUser = getUser(currentUserId)
            if (!isFollower(profile, currentUser)) {
                profile.followers = SizedCollection(profile.followers.plus(currentUser))
                currentUser.followee = SizedCollection(currentUser.followee.plus(currentUser))
            }
        }
        return getProfile(username, currentUserId)
    }

    suspend fun unfollowProfile(username: String, currentUserId: Int): Profile {
        dbQuery {
            val profile = findByUsername(username)?: throw UserDoesNotExists()
            val currentUser = getUser(currentUserId)
            if (isFollower(profile, currentUser)) {
                profile.followers = SizedCollection(profile.followers.minus(currentUser))
                currentUser.followee = SizedCollection(currentUser.followee.minus(currentUser))
            }
        }
        return getProfile(username, currentUserId)
    }
}

fun findByUsername(username: String) =
    User.find { Users.username eq username }.firstOrNull()

fun getProfileByUser(user: User, following: Boolean = false) =
    user.run {
        Profile(
            id.value,
            username,
            bio,
            image,
            following,
            user.followers.count().toInt(),
            user.followee.count().toInt()
        )
    }

fun isFollower(target: User, currentUser: User?) = if (currentUser != null) target.followers.any { it == currentUser } else false
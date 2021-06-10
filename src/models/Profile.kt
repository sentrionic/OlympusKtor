package xyz.olympusblog.models

data class Profile(
    val id: Int,
    val username: String,
    val bio: String,
    val image: String,
    val following: Boolean = false,
    val followers: Int = 0,
    val followee: Int = 0
)
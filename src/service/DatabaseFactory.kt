package xyz.olympusblog.service

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.HoconApplicationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import xyz.olympusblog.models.*

object DatabaseFactory {

    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val dbUrl = appConfig.property("ktor.db.jdbcUrl").getString()
    private val dbDriver = appConfig.property("ktor.db.dbDriver").getString()
    private val dbUser = appConfig.property("ktor.db.dbUser").getString()
    private val dbPassword = appConfig.property("ktor.db.dbPassword").getString()

    fun init() {
        Database.connect(hikari())
        transaction {
            create(Users, Followings, Articles, Tags, FavoriteArticle, ArticleTags, Comments, ArticleComment, BookmarkArticle)
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = dbDriver
        config.jdbcUrl = dbUrl
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.username = dbUser
        config.password = dbPassword
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction { block() }
    }

    suspend fun drop() {
        dbQuery { drop(Users, Followings, Articles, Tags, ArticleTags, FavoriteArticle, Comments, ArticleComment, BookmarkArticle) }
    }

}
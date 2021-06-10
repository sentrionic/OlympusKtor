package xyz.olympusblog.config

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.lettuce.core.RedisClient

private val url = HoconApplicationConfig(ConfigFactory.load()).property("ktor.redis.url").getString()
val redis = RedisClient.create(url).connect().sync()
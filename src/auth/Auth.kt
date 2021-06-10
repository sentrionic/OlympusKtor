package xyz.olympusblog.auth

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val appConfig = HoconApplicationConfig(ConfigFactory.load())
private val key = appConfig.property("ktor.deployment.secret_key").getString()

@KtorExperimentalAPI
val hashKey = hex(key)

@KtorExperimentalAPI
val hmacKey = SecretKeySpec(hashKey, "HmacSHA1")

@KtorExperimentalAPI
fun hash(password: String): String { // 4
    val hmac = Mac.getInstance("HmacSHA1")
    hmac.init(hmacKey)
    return hex(hmac.doFinal(password.toByteArray(Charsets.UTF_8)))
}
package xyz.olympusblog.config

import io.ktor.features.CORS
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun CORS.Configuration.cors() {
    method(HttpMethod.Options)
    header(HttpHeaders.XForwardedProto)
    anyHost()
    allowCredentials = true
    allowNonSimpleContentTypes = true
}
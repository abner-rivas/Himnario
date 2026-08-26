package com.himnario.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(allowedHosts: List<String>) {
    install(CORS) {
        allowedHosts.forEach { host -> allowHost(host) }
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.ContentType)
        maxAgeInSeconds = 3_600
    }
}


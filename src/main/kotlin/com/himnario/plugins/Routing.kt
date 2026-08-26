package com.himnario.plugins

import com.himnario.common.health.HealthController
import com.himnario.common.health.healthRoutes
import com.himnario.features.hymns.HymnController
import com.himnario.features.hymns.hymnRoutes
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(
    healthController: HealthController,
    hymnController: HymnController,
) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        route("/api/v1") {
            healthRoutes(healthController)
            hymnRoutes(hymnController)
        }
    }
}


package com.himnario.common.health

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.healthRoutes(controller: HealthController) {
    route("/health") {
        get { controller.live(call) }
        get("/ready") { controller.ready(call) }
    }
}


package com.himnario.features.hymns

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.hymnRoutes(controller: HymnController) {
    route("/hymns") {
        get { controller.list(call) }
        get("/{id}") { controller.get(call) }

        // Provisional public mutations. Move under /api/v1/admin/hymns when authentication is added.
        post { controller.create(call) }
        put("/{id}") { controller.update(call) }
        patch("/{id}/archive") { controller.archive(call) }
    }
}


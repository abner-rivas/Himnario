package com.himnario

import com.himnario.common.health.HealthCheck
import com.himnario.common.health.HealthController
import com.himnario.config.AppConfig
import com.himnario.config.DatabaseConfig
import com.himnario.features.hymns.ExposedHymnRepository
import com.himnario.features.hymns.HymnController
import com.himnario.features.hymns.HymnRepository
import com.himnario.features.hymns.HymnService
import com.himnario.plugins.configureCors
import com.himnario.plugins.configureMonitoring
import com.himnario.plugins.configureRouting
import com.himnario.plugins.configureSerialization
import com.himnario.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped

fun Application.module() {
    val appConfig = AppConfig.fromEnvironment()
    val databaseManager = DatabaseConfig.initialize(appConfig.database)
    monitor.subscribe(ApplicationStopped) { databaseManager.close() }

    configureApplication(
        appConfig = appConfig,
        hymnRepository = ExposedHymnRepository(databaseManager.database),
        databaseHealthCheck = databaseManager,
    )
}

fun Application.configureApplication(
    appConfig: AppConfig,
    hymnRepository: HymnRepository,
    databaseHealthCheck: HealthCheck,
) {
    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureCors(appConfig.corsAllowedHosts)

    configureRouting(
        healthController = HealthController(databaseHealthCheck),
        hymnController = HymnController(HymnService(hymnRepository)),
    )
}

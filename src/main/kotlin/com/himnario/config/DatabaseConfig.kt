package com.himnario.config

import com.himnario.common.health.HealthCheck
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

class DatabaseManager internal constructor(
    val database: Database,
    private val dataSource: HikariDataSource,
) : HealthCheck, AutoCloseable {
    override suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dataSource.connection.use { connection -> connection.isValid(2) }
        }.getOrDefault(false)
    }

    override fun close() {
        dataSource.close()
    }
}

object DatabaseConfig {
    fun initialize(settings: DatabaseSettings): DatabaseManager {
        val hikariConfig = HikariConfig().apply {
            poolName = "himnario-api-pool"
            jdbcUrl = settings.jdbcUrl
            driverClassName = "org.postgresql.Driver"
            username = settings.user
            password = settings.password
            maximumPoolSize = settings.maxPoolSize
            minimumIdle = minOf(2, settings.maxPoolSize)
            connectionTimeout = 10_000
            validationTimeout = 3_000
            idleTimeout = 600_000
            maxLifetime = 1_800_000
            initializationFailTimeout = 10_000
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            addDataSourceProperty("tcpKeepAlive", "true")
        }

        val dataSource = HikariDataSource(hikariConfig)
        return try {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()

            DatabaseManager(
                database = Database.connect(datasource = dataSource),
                dataSource = dataSource,
            )
        } catch (cause: Exception) {
            dataSource.close()
            throw cause
        }
    }
}


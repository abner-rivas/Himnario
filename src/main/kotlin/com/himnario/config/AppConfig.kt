package com.himnario.config

private const val DEFAULT_DB_PORT = 5434
private const val DEFAULT_MAX_POOL_SIZE = 10

class DatabaseSettings(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
) {
    val jdbcUrl: String = "jdbc:postgresql://$host:$port/$name"

    override fun toString(): String =
        "DatabaseSettings(host=$host, port=$port, name=$name, user=$user, password=<redacted>, maxPoolSize=$maxPoolSize)"
}

data class AppConfig(
    val database: DatabaseSettings,
    val corsAllowedHosts: List<String>,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): AppConfig {
            val database = DatabaseSettings(
                host = environment.valueOrDefault("DB_HOST", "localhost"),
                port = environment.positiveInt("DB_PORT", DEFAULT_DB_PORT),
                name = environment.valueOrDefault("DB_NAME", "himnario"),
                user = environment.valueOrDefault("DB_USER", "himnario"),
                password = environment.valueOrDefault("DB_PASSWORD", "himnario_dev"),
                maxPoolSize = environment.positiveInt("DB_MAX_POOL_SIZE", DEFAULT_MAX_POOL_SIZE),
            )

            val corsAllowedHosts = environment
                .valueOrDefault(
                    "CORS_ALLOWED_HOSTS",
                    "localhost:19006,localhost:8081,127.0.0.1:19006,127.0.0.1:8081",
                )
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)

            require(corsAllowedHosts.isNotEmpty()) { "CORS_ALLOWED_HOSTS must contain at least one host" }
            require(corsAllowedHosts.none { it == "*" || "://" in it }) {
                "CORS_ALLOWED_HOSTS entries must use host[:port] format and cannot be wildcards"
            }

            return AppConfig(database = database, corsAllowedHosts = corsAllowedHosts)
        }
    }
}

private fun Map<String, String>.valueOrDefault(name: String, default: String): String =
    get(name)?.trim()?.takeIf(String::isNotEmpty) ?: default

private fun Map<String, String>.positiveInt(name: String, default: Int): Int {
    val rawValue = get(name)?.trim()?.takeIf(String::isNotEmpty) ?: return default
    return requireNotNull(rawValue.toIntOrNull()?.takeIf { it > 0 }) {
        "$name must be a positive integer"
    }
}


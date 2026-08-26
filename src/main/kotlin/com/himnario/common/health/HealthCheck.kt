package com.himnario.common.health

fun interface HealthCheck {
    suspend fun isHealthy(): Boolean
}


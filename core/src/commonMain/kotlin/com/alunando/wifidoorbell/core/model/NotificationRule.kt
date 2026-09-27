package com.alunando.wifidoorbell.core.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationRule(
    val mac: String,
    val throttleMinutes: Int,
    val enabled: Boolean,
    val lastNotifiedAt: Long?
)

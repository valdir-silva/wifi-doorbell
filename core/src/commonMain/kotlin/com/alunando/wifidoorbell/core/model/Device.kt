package com.alunando.wifidoorbell.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val id: String,
    val hostname: String,
    val ip: String,
    val customName: String?,
    val isWatched: Boolean,
    val lastSeen: Long,
    val firstSeen: Long
)

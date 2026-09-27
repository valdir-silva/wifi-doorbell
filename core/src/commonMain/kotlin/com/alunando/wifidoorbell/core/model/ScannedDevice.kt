package com.alunando.wifidoorbell.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ScannedDevice(
    val id: String,
    val hostname: String,
    val ip: String
)

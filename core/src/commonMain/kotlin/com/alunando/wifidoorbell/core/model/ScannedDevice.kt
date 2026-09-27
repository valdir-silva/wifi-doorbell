package com.alunando.wifidoorbell.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ScannedDevice(
    val ip: String,
    val mac: String
)

package com.alunando.wifidoorbell.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ScanResult(
    val timestamp: Long,
    val devices: List<ScannedDevice>
)

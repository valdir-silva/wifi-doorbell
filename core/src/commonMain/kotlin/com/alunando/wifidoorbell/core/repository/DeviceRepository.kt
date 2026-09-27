package com.alunando.wifidoorbell.core.repository

import com.alunando.wifidoorbell.core.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getWatchedDevices(): Flow<List<Device>>
    suspend fun saveDevice(device: Device)
}

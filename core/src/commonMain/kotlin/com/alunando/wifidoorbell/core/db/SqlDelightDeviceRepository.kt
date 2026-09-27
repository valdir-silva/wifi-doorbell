package com.alunando.wifidoorbell.core.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.alunando.wifidoorbell.core.model.Device
import com.alunando.wifidoorbell.core.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightDeviceRepository(private val db: WifiDoorbellDb) : DeviceRepository {

    override fun getWatchedDevices(): Flow<List<Device>> {
        return db.deviceEntityQueries.getWatched()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDevice() } }
    }

    fun getAllDevices(): Flow<List<Device>> {
        return db.deviceEntityQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDevice() } }
    }

    override suspend fun saveDevice(device: Device) = withContext(Dispatchers.IO) {
        db.deviceEntityQueries.upsert(
            id = device.id,
            hostname = device.hostname,
            ip = device.ip,
            customName = device.customName,
            isWatched = if (device.isWatched) 1L else 0L,
            lastSeen = device.lastSeen,
            firstSeen = device.firstSeen
        )
    }

    override suspend fun getDevice(id: String): Device? = withContext(Dispatchers.IO) {
        db.deviceEntityQueries.getById(id).executeAsOneOrNull()?.toDevice()
    }

    suspend fun toggleWatched(id: String, isWatched: Boolean) = withContext(Dispatchers.IO) {
        db.deviceEntityQueries.updateWatched(
            isWatched = if (isWatched) 1L else 0L,
            id = id
        )
    }

    private fun DeviceEntity.toDevice(): Device = Device(
        id = id,
        hostname = hostname,
        ip = ip,
        customName = customName,
        isWatched = isWatched == 1L,
        lastSeen = lastSeen,
        firstSeen = firstSeen
    )
}

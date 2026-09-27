package com.alunando.wifidoorbell.core.processor

import com.alunando.wifidoorbell.core.model.Device
import com.alunando.wifidoorbell.core.model.ScanResult
import com.alunando.wifidoorbell.core.repository.DeviceRepository
import com.alunando.wifidoorbell.core.repository.RuleRepository

class ScanResultProcessor(
    private val deviceRepository: DeviceRepository,
    private val ruleRepository: RuleRepository,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun process(
        currentScan: ScanResult,
        previousScan: ScanResult?
    ): List<Device> {
        val prevIds = previousScan?.devices?.map { it.id }?.toSet() ?: emptySet()
        val newDevices = currentScan.devices.filter { it.id !in prevIds }
        
        val notifications = mutableListOf<Device>()
        val now = currentTimeMillis()

        for (scanned in newDevices) {
            val device = deviceRepository.getDevice(scanned.id)
            if (device != null && device.isWatched) {
                val rule = ruleRepository.getRuleFor(scanned.id)
                
                val canNotify = if (rule == null) {
                    true 
                } else if (!rule.enabled) {
                    false
                } else {
                    val lastNotifiedAt = rule.lastNotifiedAt ?: 0L
                    (now - lastNotifiedAt) >= (rule.throttleMinutes * 60000L)
                }

                if (canNotify) {
                    notifications.add(device)
                    if (rule != null) {
                        ruleRepository.updateRule(rule.copy(lastNotifiedAt = now))
                    }
                }
            }
        }
        
        return notifications
    }
}

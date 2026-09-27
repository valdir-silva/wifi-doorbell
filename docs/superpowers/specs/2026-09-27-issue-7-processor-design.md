# Issue 7: ScanResultProcessor Design

## 1. Context and Objective
The `ScanResultProcessor` is the core business logic component responsible for comparing the current network scan against the previous scan to identify newly connected devices, check if they are being monitored (`isWatched = true`), apply notification throttling rules, and output the final list of devices that require a notification to be sent to the user. This logic is pure Kotlin and belongs in the `core` module.

## 2. Interface Changes
To support the processor's need to query the current state of a device, the `DeviceRepository` will be updated:
```kotlin
interface DeviceRepository {
    fun getWatchedDevices(): Flow<List<Device>>
    suspend fun saveDevice(device: Device)
    suspend fun getDevice(id: String): Device? // <-- NEW
}
```

## 3. Processor Design

The processor takes the repositories as dependencies. To make time-dependent throttling testable, we inject a clock function `currentTimeMillis: () -> Long`.

```kotlin
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
                    true // No rule means no throttle limits and enabled by default
                } else if (!rule.enabled) {
                    false
                } else {
                    val lastNotifiedAt = rule.lastNotifiedAt ?: 0L
                    (now - lastNotifiedAt) >= (rule.throttleMinutes * 60000L)
                }

                if (canNotify) {
                    notifications.add(device)
                    // Update rule's lastNotifiedAt to reset the throttle timer
                    if (rule != null) {
                        ruleRepository.updateRule(rule.copy(lastNotifiedAt = now))
                    } else {
                        // Create a default rule to track the throttle timestamp for future
                        // if we want to enforce a global default throttle later.
                        // For now, if no rule is created, it will just notify again next time.
                    }
                }
            }
        }
        
        return notifications
    }
}
```

## 4. Testing Strategy
We will implement a robust test suite `ScanResultProcessorTest` using fake repositories to cover:
1. **First Scan:** `previousScan` is null, all devices are treated as new. Only watched ones notify.
2. **Already Online:** Device was in `previousScan`, so it shouldn't trigger a notification even if watched.
3. **Throttle Active:** Device is new, watched, but the rule says `lastNotifiedAt` was 2 minutes ago (and throttle is 5). Should not notify.
4. **Throttle Expired:** Device is new, watched, and `lastNotifiedAt` is older than the throttle window. Should notify.
5. **No Rule:** Device is new, watched, no rule exists. Should notify.
6. **Rule Disabled:** Device is new, watched, but rule is explicitly `enabled = false`. Should not notify.

# Issue 4: Core Data Models and Repositories Design

## 1. Context and Objective
The `core` module is a Kotlin Multiplatform (KMP) module that serves as the foundation for the WifiDoorbell app. It will contain shared business logic, data models, and repository contracts. This spec defines the initial domain models and the repository interfaces required by the system, ensuring they are decoupled from any specific database (Firestore or SQLDelight) or platform (Android).

## 2. Architecture & Patterns
- **Package structure**: `com.alunando.wifidoorbell.core.model` and `com.alunando.wifidoorbell.core.repository`
- **Serialization**: All data models will be annotated with `@Serializable` (using `kotlinx.serialization`) to facilitate easy local caching and cloud syncing.
- **Asynchrony**: Repositories will expose data via Kotlin `Flow` for reactive updates and use `suspend` functions for one-shot operations.

## 3. Data Models

```kotlin
package com.alunando.wifidoorbell.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val mac: String,
    val ip: String,
    val customName: String?,
    val manufacturer: String?,
    val isWatched: Boolean,
    val lastSeen: Long,
    val firstSeen: Long
)

@Serializable
data class NotificationRule(
    val mac: String,
    val throttleMinutes: Int,
    val enabled: Boolean,
    val lastNotifiedAt: Long?
)

@Serializable
data class ScannedDevice(
    val ip: String,
    val mac: String
)

@Serializable
data class ScanResult(
    val timestamp: Long,
    val devices: List<ScannedDevice>
)
```

## 4. Repository Interfaces

```kotlin
package com.alunando.wifidoorbell.core.repository

import com.alunando.wifidoorbell.core.model.Device
import com.alunando.wifidoorbell.core.model.NotificationRule
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    /** Gets a continuous stream of devices that the user has marked as watched. */
    fun getWatchedDevices(): Flow<List<Device>>
    
    /** Saves or updates a device in the data store. */
    suspend fun saveDevice(device: Device)
}

interface RuleRepository {
    /** Retrieves the active rule for a specific MAC address, if any. */
    suspend fun getRuleFor(mac: String): NotificationRule?
    
    /** Saves or updates a notification rule. */
    suspend fun updateRule(rule: NotificationRule)
}

interface NotificationRepository {
    /** 
     * Abstract contract for sending a notification. 
     * The Android app module will implement this by triggering a Local Notification or FCM.
     */
    suspend fun sendNotification(title: String, body: String)
}
```

## 5. Testing Strategy
- **Unit Tests**: Implement tests in `core/src/commonTest` to verify that `ScanResult` and `NotificationRule` objects can be correctly serialized and deserialized.
- **Comparison Tests**: Add test cases ensuring that equality checks (e.g. comparing two `ScanResult` instances) behave correctly, as this will be critical for the `ScanResultProcessor` later on.

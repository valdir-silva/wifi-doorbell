# MVP Design Spec — Issues 5, 9, 11, 13

> WiFi Doorbell: pipeline completo de scan → processamento → notificação → UI

## 1. Overview

This spec defines the Minimum Viable Product that connects all existing components (models, `WifiScanner`, `ScanResultProcessor`) into a functional, end-to-end network monitoring app. It covers four issues:

| Issue | Scope | Module |
|-------|-------|--------|
| #5 | SQLDelight persistence + concrete repositories | `core` |
| #11 | Local notification sender | `app` |
| #9 | Foreground Service (scan loop every 30s) | `app` |
| #13 | Main screen UI (device list + start/stop) | `app` |

## 2. Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    MainActivity (UI)                      │
│  DeviceListScreen ←── DeviceListViewModel                │
│  [Iniciar/Parar] button controls the Service             │
└───────────────────────────┬──────────────────────────────┘
                            │ startService / stopService
┌───────────────────────────▼──────────────────────────────┐
│              NetworkMonitorService                        │
│  loop every 30s:                                         │
│    1. WifiScanner.scan() → ScanResult                    │
│    2. ScanResultProcessor.process() → List<Device>       │
│    3. LocalNotificationSender.notify(devices)            │
│    4. DeviceRepository.saveDevice() (update lastSeen)    │
│  + Persistent notification: "Monitorando rede · X"       │
└───────────────────────────┬──────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────┐
│                core module (SQLDelight)                   │
│  SqlDelightDeviceRepository │ SqlDelightRuleRepository   │
│         ↕ WifiDoorbellDb (Device, NotificationRule)      │
└──────────────────────────────────────────────────────────┘
```

## 3. Issue 5 — SQLDelight & Repositories

### 3.1 Schema

Two `.sq` files inside `core/src/commonMain/sqldelight/com/alunando/wifidoorbell/core/db/`:

**Device.sq:**
```sql
CREATE TABLE DeviceEntity (
  id TEXT PRIMARY KEY,
  hostname TEXT NOT NULL,
  ip TEXT NOT NULL,
  customName TEXT,
  isWatched INTEGER NOT NULL DEFAULT 0,
  lastSeen INTEGER NOT NULL,
  firstSeen INTEGER NOT NULL
);

getById:
SELECT * FROM DeviceEntity WHERE id = ?;

getWatched:
SELECT * FROM DeviceEntity WHERE isWatched = 1;

getAll:
SELECT * FROM DeviceEntity;

upsert:
INSERT OR REPLACE INTO DeviceEntity(id, hostname, ip, customName, isWatched, lastSeen, firstSeen)
VALUES (?, ?, ?, ?, ?, ?, ?);

updateWatched:
UPDATE DeviceEntity SET isWatched = ? WHERE id = ?;
```

**NotificationRule.sq:**
```sql
CREATE TABLE NotificationRuleEntity (
  id TEXT PRIMARY KEY,
  throttleMinutes INTEGER NOT NULL DEFAULT 30,
  enabled INTEGER NOT NULL DEFAULT 1,
  lastNotifiedAt INTEGER
);

getById:
SELECT * FROM NotificationRuleEntity WHERE id = ?;

upsert:
INSERT OR REPLACE INTO NotificationRuleEntity(id, throttleMinutes, enabled, lastNotifiedAt)
VALUES (?, ?, ?, ?);
```

> **Design decision:** `ScanResult` and `ScannedDevice` are NOT persisted. They are ephemeral — only the current and previous scan matter, held in memory by the Service.

### 3.2 Repository Implementations

**`SqlDelightDeviceRepository`** in `core/src/commonMain`:
```kotlin
class SqlDelightDeviceRepository(private val db: WifiDoorbellDb) : DeviceRepository {
    override fun getWatchedDevices(): Flow<List<Device>>
        // Uses db.deviceEntityQueries.getWatched().asFlow().mapToList()
        // Maps DeviceEntity → Device

    override suspend fun saveDevice(device: Device)
        // Uses db.deviceEntityQueries.upsert(...)

    override suspend fun getDevice(id: String): Device?
        // Uses db.deviceEntityQueries.getById(id).executeAsOneOrNull()
        // Maps DeviceEntity → Device

    fun getAllDevices(): Flow<List<Device>>
        // Uses db.deviceEntityQueries.getAll().asFlow().mapToList()

    fun toggleWatched(id: String, isWatched: Boolean)
        // Uses db.deviceEntityQueries.updateWatched(isWatched, id)
}
```

**`SqlDelightRuleRepository`** in `core/src/commonMain`:
```kotlin
class SqlDelightRuleRepository(private val db: WifiDoorbellDb) : RuleRepository {
    override suspend fun getRuleFor(id: String): NotificationRule?
    override suspend fun updateRule(rule: NotificationRule)
}
```

### 3.3 DatabaseFactory

Android-specific factory in `core/src/androidMain`:
```kotlin
object DatabaseFactory {
    fun create(context: Context): WifiDoorbellDb {
        val driver = AndroidSqliteDriver(WifiDoorbellDb.Schema, context, "wifidoorbell.db")
        return WifiDoorbellDb(driver)
    }
}
```

## 4. Issue 11 — Local Notification Sender

### 4.1 LocalNotificationSender

Located in `app/src/main/java/com/alunando/wifidoorbell/notification/`:

```kotlin
class LocalNotificationSender(private val context: Context) : NotificationRepository {

    companion object {
        const val CHANNEL_ID = "device_alert"
    }

    init {
        createChannel()
    }

    override suspend fun sendNotification(title: String, body: String) {
        // Build notification with NotificationCompat.Builder
        // Use CHANNEL_ID with importance HIGH (sound + vibration)
        // Use device id hashCode as notification ID to avoid duplicates
        // Small icon: R.drawable.ic_notification (or default)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de Dispositivos",
            NotificationManager.IMPORTANCE_HIGH
        )
        // Register with NotificationManager
    }
}
```

### 4.2 Integration with ScanResultProcessor

In the Service loop, after `processor.process()` returns a list of devices to notify:
```kotlin
for (device in devicesToNotify) {
    notificationSender.sendNotification(
        title = "Dispositivo detectado!",
        body = "${device.hostname} (${device.ip}) conectou na rede"
    )
}
```

## 5. Issue 9 — NetworkMonitorService

### 5.1 Service Class

Located in `app/src/main/java/com/alunando/wifidoorbell/service/`:

```kotlin
class NetworkMonitorService : Service() {

    companion object {
        const val PERSISTENT_CHANNEL_ID = "monitoring_persistent"
        const val PERSISTENT_NOTIFICATION_ID = 1
        const val SCAN_INTERVAL_MS = 30_000L
    }

    private lateinit var scanner: WifiScanner
    private lateinit var processor: ScanResultProcessor
    private lateinit var notificationSender: LocalNotificationSender
    private lateinit var deviceRepository: SqlDelightDeviceRepository

    private var previousScan: ScanResult? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createPersistentChannel()
        // Initialize dependencies using DatabaseFactory
        val db = DatabaseFactory.create(applicationContext)
        deviceRepository = SqlDelightDeviceRepository(db)
        val ruleRepository = SqlDelightRuleRepository(db)
        scanner = WifiScanner(applicationContext)
        processor = ScanResultProcessor(deviceRepository, ruleRepository)
        notificationSender = LocalNotificationSender(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(PERSISTENT_NOTIFICATION_ID, buildPersistentNotification(0))
        startScanLoop()
        return START_STICKY
    }

    private fun startScanLoop() {
        serviceScope.launch {
            while (isActive) {
                val currentScan = scanner.scan()
                val devicesToNotify = processor.process(currentScan, previousScan)

                // Save/update all scanned devices
                for (scanned in currentScan.devices) {
                    val existing = deviceRepository.getDevice(scanned.id)
                    val now = System.currentTimeMillis()
                    val device = existing?.copy(
                        ip = scanned.ip,
                        hostname = scanned.hostname,
                        lastSeen = now
                    ) ?: Device(
                        id = scanned.id,
                        hostname = scanned.hostname,
                        ip = scanned.ip,
                        customName = null,
                        isWatched = false,
                        lastSeen = now,
                        firstSeen = now
                    )
                    deviceRepository.saveDevice(device)
                }

                // Send alert notifications
                for (device in devicesToNotify) {
                    notificationSender.sendNotification(
                        title = "Dispositivo detectado!",
                        body = "${device.customName ?: device.hostname} (${device.ip})"
                    )
                }

                // Update persistent notification
                updatePersistentNotification(currentScan.devices.size)

                previousScan = currentScan
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    private fun buildPersistentNotification(deviceCount: Int): Notification {
        // LOW importance channel, no sound
        // Content: "Monitorando rede · $deviceCount dispositivos"
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

### 5.2 Manifest Declaration

```xml
<service
    android:name=".service.NetworkMonitorService"
    android:foregroundServiceType="connectedDevice"
    android:exported="false" />
```

### 5.3 Scope Exclusions (MVP)

- No `BootReceiver` — user restarts manually.
- No "modo simples" action button on persistent notification.
- Fixed 30s interval — no user configuration.

## 6. Issue 13 — Main Screen UI

### 6.1 DeviceListViewModel

```kotlin
class DeviceListViewModel(
    private val deviceRepository: SqlDelightDeviceRepository,
    private val application: Application
) : AndroidViewModel(application) {

    val allDevices: StateFlow<List<Device>>
        // Collects deviceRepository.getAllDevices()

    val isMonitoring: MutableStateFlow<Boolean>
        // Tracks service running state

    fun toggleMonitoring() {
        // Starts or stops NetworkMonitorService
    }

    fun toggleWatched(deviceId: String, isWatched: Boolean) {
        // Calls deviceRepository.toggleWatched(deviceId, isWatched)
    }
}
```

### 6.2 DeviceListScreen

Single-screen Composable:
- **Top section:** Status indicator (green/red dot + "Monitorando" / "Parado") and device count.
- **Toggle button:** "Iniciar Monitoramento" / "Parar Monitoramento".
- **LazyColumn:** Each item shows:
  - Device hostname (bold)
  - IP address (secondary text)
  - Eye icon button to toggle `isWatched` (filled = watched, outlined = not watched)
  - If `isWatched`, a subtle highlight or badge on the item.
- **Empty state:** "Nenhum dispositivo encontrado. Inicie o monitoramento."

### 6.3 Dependency Wiring

For this MVP, we will use a simple manual factory pattern (no Hilt/Koin) to create the ViewModel with its dependencies:
- `DeviceListViewModelFactory` creates the ViewModel by passing the `Application` context and repositories derived from `DatabaseFactory.create(context)`.

## 7. Error Handling

| Scenario | Behavior |
|----------|----------|
| WiFi disconnected during scan | `WifiScanner` returns empty `ScanResult`; Service continues looping |
| Location permission revoked | `WifiScanner` returns empty `ScanResult`; UI shows permission prompt again |
| Database corruption | SQLDelight auto-recreates the DB (destructive migration OK for MVP) |
| Service killed by OS | `START_STICKY` causes Android to restart it automatically |

## 8. Testing Strategy

| Component | Test Type | Approach |
|-----------|-----------|----------|
| `SqlDelightDeviceRepository` | Unit test | In-memory `JdbcSqliteDriver` |
| `SqlDelightRuleRepository` | Unit test | In-memory `JdbcSqliteDriver` |
| `LocalNotificationSender` | Manual | Verify notification appears on device |
| `NetworkMonitorService` | Manual | Start/stop via UI, verify scan loop runs |
| `DeviceListViewModel` | Unit test | Fake repository, verify state changes |
| `DeviceListScreen` | Manual | Visual verification on device |

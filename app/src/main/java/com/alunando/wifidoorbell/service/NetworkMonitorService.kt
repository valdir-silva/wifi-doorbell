package com.alunando.wifidoorbell.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alunando.wifidoorbell.R
import com.alunando.wifidoorbell.core.db.DatabaseFactory
import com.alunando.wifidoorbell.core.db.SqlDelightDeviceRepository
import com.alunando.wifidoorbell.core.db.SqlDelightRuleRepository
import com.alunando.wifidoorbell.core.model.Device
import com.alunando.wifidoorbell.core.model.ScanResult
import com.alunando.wifidoorbell.core.processor.ScanResultProcessor
import com.alunando.wifidoorbell.notification.LocalNotificationSender
import com.alunando.wifidoorbell.scanner.WifiScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetworkMonitorService : Service() {

    companion object {
        const val PERSISTENT_CHANNEL_ID = "monitoring_persistent"
        const val PERSISTENT_NOTIFICATION_ID = 1
        const val SCAN_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java)
            context.stopService(intent)
        }
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
                try {
                    val currentScan = scanner.scan()

                    val devicesToNotify = processor.process(currentScan, previousScan)

                    // Save/update all scanned devices in DB
                    val now = System.currentTimeMillis()
                    for (scanned in currentScan.devices) {
                        val existing = deviceRepository.getDevice(scanned.id)
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

                    // Send alert notifications for watched devices
                    for (device in devicesToNotify) {
                        notificationSender.sendNotification(
                            title = "Dispositivo detectado!",
                            body = "${device.customName ?: device.hostname} (${device.ip})"
                        )
                    }

                    // Update persistent notification
                    updatePersistentNotification(currentScan.devices.size)

                    previousScan = currentScan
                } catch (e: Exception) {
                    // Log and continue - don't crash the service
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    private fun createPersistentChannel() {
        val channel = NotificationChannel(
            PERSISTENT_CHANNEL_ID,
            "Monitoramento de Rede",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificação persistente durante o monitoramento"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildPersistentNotification(deviceCount: Int): Notification {
        return NotificationCompat.Builder(this, PERSISTENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Monitorando rede")
            .setContentText("$deviceCount dispositivos encontrados")
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updatePersistentNotification(deviceCount: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(PERSISTENT_NOTIFICATION_ID, buildPersistentNotification(deviceCount))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

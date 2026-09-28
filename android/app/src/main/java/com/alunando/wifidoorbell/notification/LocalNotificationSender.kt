package com.alunando.wifidoorbell.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.alunando.wifidoorbell.R
import com.alunando.wifidoorbell.core.repository.NotificationRepository

class LocalNotificationSender(private val context: Context) : NotificationRepository {

    companion object {
        const val CHANNEL_ID = "device_alert"
        private var notificationIdCounter = 1000
    }

    init {
        createChannel()
    }

    override suspend fun sendNotification(title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationIdCounter++, notification)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de Dispositivos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificações quando um dispositivo monitorado conecta na rede"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

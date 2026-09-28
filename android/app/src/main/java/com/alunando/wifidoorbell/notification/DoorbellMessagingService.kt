package com.alunando.wifidoorbell.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoorbellMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Novo token gerado: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Pega o título e corpo da notificação (se enviados) ou usa padrão
        val title = message.notification?.title ?: message.data["title"] ?: "Campainha!"
        val body = message.notification?.body ?: message.data["body"] ?: "Alguém conectou na rede."
        
        // Usa o sender local (que já cria o canal e toca o som)
        val sender = LocalNotificationSender(applicationContext)
        CoroutineScope(Dispatchers.Main).launch {
            sender.sendNotification(title, body)
        }
    }
}

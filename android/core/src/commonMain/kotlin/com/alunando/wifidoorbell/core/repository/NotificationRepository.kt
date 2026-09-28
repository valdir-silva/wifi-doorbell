package com.alunando.wifidoorbell.core.repository

interface NotificationRepository {
    suspend fun sendNotification(title: String, body: String)
}

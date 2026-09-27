package com.alunando.wifidoorbell.core.repository

import com.alunando.wifidoorbell.core.model.NotificationRule

interface RuleRepository {
    suspend fun getRuleFor(mac: String): NotificationRule?
    suspend fun updateRule(rule: NotificationRule)
}

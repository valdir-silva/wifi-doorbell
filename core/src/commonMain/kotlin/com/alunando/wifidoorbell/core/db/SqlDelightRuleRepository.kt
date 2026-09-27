package com.alunando.wifidoorbell.core.db

import com.alunando.wifidoorbell.core.model.NotificationRule
import com.alunando.wifidoorbell.core.repository.RuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightRuleRepository(private val db: WifiDoorbellDb) : RuleRepository {

    override suspend fun getRuleFor(id: String): NotificationRule? = withContext(Dispatchers.IO) {
        db.notificationRuleEntityQueries.getById(id).executeAsOneOrNull()?.toRule()
    }

    override suspend fun updateRule(rule: NotificationRule) = withContext(Dispatchers.IO) {
        db.notificationRuleEntityQueries.upsert(
            id = rule.id,
            throttleMinutes = rule.throttleMinutes.toLong(),
            enabled = if (rule.enabled) 1L else 0L,
            lastNotifiedAt = rule.lastNotifiedAt
        )
    }

    private fun NotificationRuleEntity.toRule(): NotificationRule = NotificationRule(
        id = id,
        throttleMinutes = throttleMinutes.toInt(),
        enabled = enabled == 1L,
        lastNotifiedAt = lastNotifiedAt
    )
}

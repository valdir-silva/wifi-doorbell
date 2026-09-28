package com.alunando.wifidoorbell.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationRuleTest {
    @Test
    fun testSerialization() {
        val rule = NotificationRule("device-id-123", 30, true, 123456789L)
        val jsonString = Json.encodeToString(NotificationRule.serializer(), rule)
        val decoded = Json.decodeFromString(NotificationRule.serializer(), jsonString)
        assertEquals(rule, decoded)
    }
}

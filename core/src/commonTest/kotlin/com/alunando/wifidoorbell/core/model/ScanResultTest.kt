package com.alunando.wifidoorbell.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ScanResultTest {
    @Test
    fun testSerializationAndEquality() {
        val dev1 = ScannedDevice("device-id-123", "John-iPhone", "192.168.1.5")
        val result1 = ScanResult(1000L, listOf(dev1))
        val result2 = ScanResult(1000L, listOf(dev1))
        val result3 = ScanResult(2000L, listOf(dev1))

        assertEquals(result1, result2)
        assertNotEquals(result1, result3)

        val jsonString = Json.encodeToString(ScanResult.serializer(), result1)
        val decoded = Json.decodeFromString(ScanResult.serializer(), jsonString)
        assertEquals(result1, decoded)
    }
}

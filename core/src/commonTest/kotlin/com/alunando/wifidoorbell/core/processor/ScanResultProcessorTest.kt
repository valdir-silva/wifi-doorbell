package com.alunando.wifidoorbell.core.processor

import com.alunando.wifidoorbell.core.model.Device
import com.alunando.wifidoorbell.core.model.NotificationRule
import com.alunando.wifidoorbell.core.model.ScanResult
import com.alunando.wifidoorbell.core.model.ScannedDevice
import com.alunando.wifidoorbell.core.repository.DeviceRepository
import com.alunando.wifidoorbell.core.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanResultProcessorTest {

    class FakeDeviceRepository : DeviceRepository {
        val devices = mutableMapOf<String, Device>()
        override fun getWatchedDevices(): Flow<List<Device>> = flowOf(devices.values.filter { it.isWatched })
        override suspend fun saveDevice(device: Device) { devices[device.id] = device }
        override suspend fun getDevice(id: String): Device? = devices[id]
    }

    class FakeRuleRepository : RuleRepository {
        val rules = mutableMapOf<String, NotificationRule>()
        override suspend fun getRuleFor(id: String): NotificationRule? = rules[id]
        override suspend fun updateRule(rule: NotificationRule) { rules[rule.id] = rule }
    }

    @Test
    fun testFirstScan() = runBlocking {
        val devRepo = FakeDeviceRepository()
        val ruleRepo = FakeRuleRepository()
        
        // device1 is watched, device2 is NOT watched
        devRepo.saveDevice(Device("d1", "Host1", "ip", null, true, 0, 0))
        devRepo.saveDevice(Device("d2", "Host2", "ip", null, false, 0, 0))

        val processor = ScanResultProcessor(devRepo, ruleRepo)
        val scan = ScanResult(1000, listOf(ScannedDevice("d1", "Host1", "ip"), ScannedDevice("d2", "Host2", "ip")))
        
        val results = processor.process(scan, null)
        
        assertEquals(1, results.size)
        assertEquals("d1", results.first().id)
    }

    @Test
    fun testAlreadyOnline() = runBlocking {
        val devRepo = FakeDeviceRepository()
        val ruleRepo = FakeRuleRepository()
        devRepo.saveDevice(Device("d1", "Host1", "ip", null, true, 0, 0))

        val processor = ScanResultProcessor(devRepo, ruleRepo)
        val scan = ScanResult(2000, listOf(ScannedDevice("d1", "Host1", "ip")))
        val prevScan = ScanResult(1000, listOf(ScannedDevice("d1", "Host1", "ip")))
        
        val results = processor.process(scan, prevScan)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testThrottleActive() = runBlocking {
        val devRepo = FakeDeviceRepository()
        val ruleRepo = FakeRuleRepository()
        devRepo.saveDevice(Device("d1", "Host1", "ip", null, true, 0, 0))
        
        // Notified at 1000, throttle is 5 mins (300_000 ms)
        ruleRepo.updateRule(NotificationRule("d1", 5, true, 1000L))

        // Current time is 1000 + 2 mins = 121_000 ms -> Should be throttled
        val processor = ScanResultProcessor(devRepo, ruleRepo, currentTimeMillis = { 121_000L })
        val scan = ScanResult(121_000, listOf(ScannedDevice("d1", "Host1", "ip")))
        
        val results = processor.process(scan, null)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testThrottleExpired() = runBlocking {
        val devRepo = FakeDeviceRepository()
        val ruleRepo = FakeRuleRepository()
        devRepo.saveDevice(Device("d1", "Host1", "ip", null, true, 0, 0))
        
        // Notified at 1000, throttle is 5 mins (300_000 ms)
        ruleRepo.updateRule(NotificationRule("d1", 5, true, 1000L))

        // Current time is 1000 + 6 mins = 361_000 ms -> Should NOT be throttled
        val processor = ScanResultProcessor(devRepo, ruleRepo, currentTimeMillis = { 361_000L })
        val scan = ScanResult(361_000, listOf(ScannedDevice("d1", "Host1", "ip")))
        
        val results = processor.process(scan, null)
        assertEquals(1, results.size)
        // Rule lastNotifiedAt should be updated to 361_000
        assertEquals(361_000L, ruleRepo.getRuleFor("d1")?.lastNotifiedAt)
    }

    @Test
    fun testRuleDisabled() = runBlocking {
        val devRepo = FakeDeviceRepository()
        val ruleRepo = FakeRuleRepository()
        devRepo.saveDevice(Device("d1", "Host1", "ip", null, true, 0, 0))
        
        ruleRepo.updateRule(NotificationRule("d1", 5, false, 0L))

        val processor = ScanResultProcessor(devRepo, ruleRepo)
        val scan = ScanResult(1000, listOf(ScannedDevice("d1", "Host1", "ip")))
        
        val results = processor.process(scan, null)
        assertTrue(results.isEmpty())
    }
}

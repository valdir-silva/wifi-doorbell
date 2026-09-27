# Issue 7 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `ScanResultProcessor` to detect newly connected watched devices and apply throttling logic before dispatching notifications.

**Architecture:** 
The processor lives in `core/src/commonMain/kotlin/com/alunando/wifidoorbell/core/processor`. It is pure Kotlin and utilizes the `DeviceRepository` and `RuleRepository` to determine state.

**Spec:** [Issue 7 Spec](file:///c:/Users/valdir/Documents/dev/android/wifi-doorbell/docs/superpowers/specs/2026-09-27-issue-7-processor-design.md)

---

### Task 1: Update DeviceRepository

**Files:**
- Modify: `core/src/commonMain/kotlin/com/alunando/wifidoorbell/core/repository/DeviceRepository.kt`

**Interfaces:**
- `suspend fun getDevice(id: String): Device?`

- [ ] **Step 1: Add method to interface**
Open `DeviceRepository.kt` and add `suspend fun getDevice(id: String): Device?`

- [ ] **Step 2: Commit**
```bash
git add core/src/commonMain/kotlin/com/alunando/wifidoorbell/core/repository/DeviceRepository.kt
git commit -m "feat(core): adiciona getDevice ao DeviceRepository"
```

---

### Task 2: Implement ScanResultProcessor

**Files:**
- Create: `core/src/commonMain/kotlin/com/alunando/wifidoorbell/core/processor/ScanResultProcessor.kt`

**Interfaces:**
- `suspend fun process(currentScan: ScanResult, previousScan: ScanResult?): List<Device>`

- [ ] **Step 1: Create Processor Class**
Create the class as designed in the Spec, taking repositories and a `currentTimeMillis` clock function. Use sets to diff `currentScan` and `previousScan` devices by `id`. Filter devices that exist in the DB and have `isWatched == true`. Check their rules and apply throttle, returning the final list. If a rule exists and throttle passes, update its `lastNotifiedAt`.

- [ ] **Step 2: Commit**
```bash
git add core/src/commonMain/kotlin/com/alunando/wifidoorbell/core/processor/ScanResultProcessor.kt
git commit -m "feat(core): implementa ScanResultProcessor"
```

---

### Task 3: Unit Tests for Processor

**Files:**
- Create: `core/src/commonTest/kotlin/com/alunando/wifidoorbell/core/processor/ScanResultProcessorTest.kt`

**Interfaces:**
- Fake repositories to back the logic.

- [ ] **Step 1: Create Fake Repositories**
Implement a simple in-memory `FakeDeviceRepository` and `FakeRuleRepository`.

- [ ] **Step 2: Write Test Cases**
- `test First Scan (no previous)`
- `test Already Online`
- `test Throttle Active`
- `test Throttle Expired`
- `test No Rule`
- `test Rule Disabled`

- [ ] **Step 3: Verify and Commit**
Run `./gradlew :core:test`.
```bash
git add core/src/commonTest/kotlin/com/alunando/wifidoorbell/core/processor/ScanResultProcessorTest.kt
git commit -m "test(core): adiciona testes para ScanResultProcessor"
```

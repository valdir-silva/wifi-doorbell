# Issue 8: WifiScanner & Model Refactor Design

## 1. Context and Objective
We need to implement the core network scanner for the app. The original plan (Issue 8) involved reading `/proc/net/arp` and performing OUI lookups based on MAC addresses. However, Android 10+ restricts third-party apps from reading the ARP table or accessing MAC addresses. Therefore, the scanner must rely entirely on active network sweeping (ICMP Ping + TCP Fallback) and identify devices using their Hostname/IP rather than their MAC address.

## 2. Model Refactoring
Because MAC addresses are unavailable, the models created in Issue 4 must be updated:
- The `mac` field will be replaced with an `id` field (a unique string hash based on the hostname or IP if hostname is unavailable).
- The `manufacturer` field will be removed from `Device` (no OUI database).
- A `hostname` field will be added to `ScannedDevice` and `Device`.

## 3. WifiScanner Architecture
The `WifiScanner` component resides in the `app` module since it requires Android context (`WifiManager`). It runs in `Dispatchers.IO`.

### 3.1 Flow
1. **Subnet Calculation**: Use `WifiManager.connectionInfo.ipAddress` and `DhcpInfo.netmask` to determine the start and end IP of the current local network (usually a /24 subnet, meaning 254 addresses).
2. **Parallel Sweep**: For each IP in the subnet, launch a coroutine to test reachability.
3. **Reachability Tests**:
   - Primary: `InetAddress.getByAddress(ip).isReachable(200)`
   - Fallback: If `isReachable` is false, attempt to connect a `Socket` to port 80, then port 443 with a 200ms timeout.
4. **Identity Resolution**: For IPs that respond, call `InetAddress.canonicalHostName`. Generate an `id` from this hostname.
5. **Result Compilation**: Return a `ScanResult` containing the list of responsive `ScannedDevice`s.

### 3.2 Interface
```kotlin
package com.alunando.wifidoorbell.scanner

import android.content.Context
import com.alunando.wifidoorbell.core.model.ScanResult

class WifiScanner(private val context: Context) {
    suspend fun scan(): ScanResult
}
```

## 4. Error Handling and Edge Cases
- **Not Connected to WiFi**: If the device is not on WiFi, the scanner should immediately return an empty `ScanResult`.
- **Permission Denied**: If Location permissions are suddenly revoked (needed for SSID/Wifi state on modern Android), return an empty `ScanResult`.
- **No Hostname Found**: If DNS fails to resolve a hostname, the IP address itself will serve as the fallback `hostname` and `id`.

## 5. Testing Strategy
Since `WifiScanner` heavily relies on Android system APIs (Networking, WifiManager), we will write unit tests for the core calculation logic (e.g., subnet masking) and rely on manual/instrumented testing for the actual sweep logic. We will also update the tests for `NotificationRule` and `ScanResult` in the `core` module to reflect the new properties.

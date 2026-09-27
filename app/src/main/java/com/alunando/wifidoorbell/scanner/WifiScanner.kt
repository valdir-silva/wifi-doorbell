package com.alunando.wifidoorbell.scanner

import android.content.Context
import android.net.wifi.WifiManager
import com.alunando.wifidoorbell.core.model.ScanResult
import com.alunando.wifidoorbell.core.model.ScannedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

class WifiScanner(private val context: Context) {

    suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        
        if (dhcpInfo == null || dhcpInfo.ipAddress == 0) {
            return@withContext ScanResult(System.currentTimeMillis(), emptyList())
        }

        val ips = SubnetUtils.getIpsInSubnet(dhcpInfo.ipAddress, dhcpInfo.netmask)
        
        val scannedDevices = ips.map { ip ->
            async { pingAndResolve(ip) }
        }.awaitAll().filterNotNull()

        ScanResult(System.currentTimeMillis(), scannedDevices)
    }

    private fun pingAndResolve(ip: String): ScannedDevice? {
        return try {
            val inetAddress = InetAddress.getByName(ip)
            val isReachable = inetAddress.isReachable(200) || tryTcpConnect(ip, 80) || tryTcpConnect(ip, 443)
            
            if (isReachable) {
                val hostname = inetAddress.canonicalHostName.takeIf { it != ip } ?: "Unknown Device"
                val id = hashString(hostname + ip) // Use IP in hash to avoid collisions if hostname is Unknown
                ScannedDevice(id = id, hostname = hostname, ip = ip)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun tryTcpConnect(ip: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 200)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}

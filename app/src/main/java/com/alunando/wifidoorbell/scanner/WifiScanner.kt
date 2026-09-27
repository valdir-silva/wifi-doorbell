package com.alunando.wifidoorbell.scanner

import android.content.Context
import android.net.wifi.WifiManager
import com.alunando.wifidoorbell.core.model.ScanResult
import com.alunando.wifidoorbell.core.model.ScannedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import android.util.Log

class WifiScanner(private val context: Context) {

    suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        
        Log.d("WifiScanner", "dhcpInfo: ip=${dhcpInfo?.ipAddress}, netmask=${dhcpInfo?.netmask}")
        if (dhcpInfo == null || dhcpInfo.ipAddress == 0) {
            Log.w("WifiScanner", "dhcpInfo is null or ipAddress is 0. Returning empty.")
            return@withContext ScanResult(System.currentTimeMillis(), emptyList())
        }

        val ips = SubnetUtils.getIpsInSubnet(dhcpInfo.ipAddress, dhcpInfo.netmask)
        Log.d("WifiScanner", "Scanning ${ips.size} IPs in subnet...")
        
        val scannedDevices = ips.map { ip ->
            async { pingAndResolve(ip) }
        }.awaitAll().filterNotNull()

        Log.d("WifiScanner", "Scan complete. Found ${scannedDevices.size} devices.")
        ScanResult(System.currentTimeMillis(), scannedDevices)
    }

    private fun pingAndResolve(ip: String): ScannedDevice? {
        return try {
            val inetAddress = InetAddress.getByName(ip)
            val isReachable = inetAddress.isReachable(200) || tryTcpConnect(ip, 80) || tryTcpConnect(ip, 443)
            
            if (isReachable) {
                val hostname = inetAddress.canonicalHostName.takeIf { it != ip } ?: "Unknown Device"
                val macAddress = getMacAddress(ip)
                
                // Se conseguirmos o MAC (pode falhar no Android 10+), usamos ele pro ID. Se não, fallback pro IP.
                val idInput = macAddress ?: (hostname + ip)
                val id = hashString(idInput)
                
                var finalHostname = if (macAddress != null && hostname == "Unknown Device") "Device ($macAddress)" else hostname
                
                // Tenta extrair info do servidor HTTP (Porta 80) para dar mais contexto ao usuario
                if (finalHostname.startsWith("Unknown") || finalHostname.startsWith("Device")) {
                    val httpInfo = getHttpInfo(ip)
                    if (httpInfo != null) {
                        Log.d("WifiScanner", "HTTP Info encontrado para $ip: $httpInfo")
                        finalHostname = "$finalHostname [$httpInfo]"
                    } else {
                        Log.d("WifiScanner", "Nenhuma HTTP Info para $ip")
                    }
                }
                
                Log.d("WifiScanner", "Dispositivo detectado: IP=$ip, Nome=$finalHostname, MAC=$macAddress")
                ScannedDevice(id = id, hostname = finalHostname, ip = ip)
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

    private fun getMacAddress(ip: String): String? {
        try {
            // Tenta ler do arquivo ARP (pode ser bloqueado no Android 10+)
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val parts = line.split(Regex(" +"))
                    if (parts.size >= 4 && parts[0] == ip) {
                        val mac = parts[3]
                        if (mac.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))) {
                            Log.d("WifiScanner", "MAC Address lido do ARP para $ip: $mac")
                            return mac
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e("WifiScanner", "Falha ao ler /proc/net/arp para $ip: ${e.message}")
        }

        try {
            // Tenta via comando ip neigh (pode ser bloqueado pelo SELinux no Android 10+)
            val process = Runtime.getRuntime().exec("ip neigh show $ip")
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                val line = reader.readLine()
                if (line != null && line.contains(" lladdr ")) {
                    val mac = line.substringAfter(" lladdr ").substringBefore(" ")
                    if (mac.isNotBlank()) {
                        Log.d("WifiScanner", "MAC Address lido do ip neigh para $ip: $mac")
                        return mac
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WifiScanner", "Falha ao executar ip neigh para $ip: ${e.message}")
        }

        try {
            // Tenta via ROOT (su) como última esperança para contornar o bloqueio do Android 10+
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "ip neigh show $ip"))
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.contains(ip) && line.contains(" lladdr ")) {
                        val mac = line.substringAfter(" lladdr ").substringBefore(" ")
                        if (mac.isNotBlank()) {
                            Log.d("WifiScanner", "MAC Address lido com SUCESSO via ROOT para $ip: $mac")
                            return mac
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e("WifiScanner", "Falha ao executar ip neigh como root para $ip: ${e.message}")
        }

        try {
            // Outra tentativa ROOT lendo o /proc/net/arp
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /proc/net/arp | grep $ip"))
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                val line = reader.readLine()
                if (line != null) {
                    val parts = line.split(Regex(" +"))
                    if (parts.size >= 4 && parts[0] == ip) {
                        val mac = parts[3]
                        if (mac.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))) {
                            Log.d("WifiScanner", "MAC Address lido do ARP via ROOT para $ip: $mac")
                            return mac
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WifiScanner", "Falha ao ler arp como root para $ip: ${e.message}")
        }

        Log.d("WifiScanner", "Não foi possivel obter o MAC para $ip")
        return null
    }

    private fun getHttpInfo(ip: String): String? {
        try {
            val url = URL("http://$ip")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 300
            conn.readTimeout = 300
            
            // 1. Tenta pegar o header "Server"
            val server = conn.getHeaderField("Server")
            if (!server.isNullOrBlank()) {
                Log.d("WifiScanner", "Header Server retornado para $ip: $server")
                return server
            }
            
            // 2. Tenta ler a tag <title>
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                val html = reader.readText()
                val match = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
                if (match != null) {
                    val title = match.groupValues[1].trim()
                    Log.d("WifiScanner", "Tag title HTML retornada para $ip: $title")
                    return title
                }
            }
        } catch (e: Exception) {
            Log.e("WifiScanner", "Falha HTTP no IP $ip: ${e.message}")
        }
        return null
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}

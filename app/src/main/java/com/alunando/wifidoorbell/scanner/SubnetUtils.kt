package com.alunando.wifidoorbell.scanner

object SubnetUtils {
    /**
     * Given an IP address and a netmask (both in network byte order as returned by DhcpInfo),
     * returns a list of all IP addresses in the subnet (excluding network and broadcast).
     */
    fun getIpsInSubnet(ipAddress: Int, netmask: Int): List<String> {
        val actualNetmask = if (netmask == 0) 0x00FFFFFF else netmask
        
        // Android DhcpInfo uses little-endian. e.g. 255.255.255.0 is 0x00FFFFFF.
        // We need to convert it to big-endian to do normal subnet math, or just count the 0 bits.
        val bigEndianNetmask = Integer.reverseBytes(actualNetmask)
        val invertedNetmask = bigEndianNetmask.inv()
        val numHosts = invertedNetmask - 1
        
        if (numHosts <= 0 || numHosts > 65536) return emptyList() // Limit to /16 to avoid OOM
        
        val ips = mutableListOf<String>()
        val networkAddress = ipAddress and actualNetmask
        
        for (i in 1..numHosts) {
            val hostPartLittleEndian = Integer.reverseBytes(i)
            val hostAddress = networkAddress or hostPartLittleEndian
            ips.add(intToIp(hostAddress))
        }
        return ips
    }

    private fun intToIp(i: Int): String {
        return "${i and 0xFF}.${(i shr 8) and 0xFF}.${(i shr 16) and 0xFF}.${(i shr 24) and 0xFF}"
    }
}

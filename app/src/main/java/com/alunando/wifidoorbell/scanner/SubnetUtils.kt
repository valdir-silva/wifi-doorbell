package com.alunando.wifidoorbell.scanner

object SubnetUtils {
    /**
     * Given an IP address and a netmask (both in network byte order as returned by DhcpInfo),
     * returns a list of all IP addresses in the subnet (excluding network and broadcast).
     */
    fun getIpsInSubnet(ipAddress: Int, netmask: Int): List<String> {
        if (netmask == 0) return emptyList()
        
        val networkAddress = ipAddress and netmask
        val invertedNetmask = netmask.inv()
        val numHosts = invertedNetmask - 1
        
        if (numHosts <= 0) return emptyList()
        
        val ips = mutableListOf<String>()
        // Exclude 0 (network) and invertedNetmask (broadcast)
        for (i in 1..numHosts) {
            val hostAddress = networkAddress or i
            ips.add(intToIp(hostAddress))
        }
        return ips
    }

    private fun intToIp(i: Int): String {
        return "${i and 0xFF}.${(i shr 8) and 0xFF}.${(i shr 16) and 0xFF}.${(i shr 24) and 0xFF}"
    }
}

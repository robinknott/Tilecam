package no.tilecam.app.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import java.net.Inet4Address

/**
 * IP / subnet helpers. Pure functions where possible so they are unit-testable.
 */
object NetworkUtils {

    /** The IPv4 LinkAddress of the active network, if any (e.g. 192.168.1.42/24). */
    fun activeIpv4(context: Context): LinkAddress? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val network = cm.activeNetwork ?: return null
        val props = cm.getLinkProperties(network) ?: return null
        return props.linkAddresses.firstOrNull {
            it.address is Inet4Address && !it.address.isLoopbackAddress
        }
    }

    /** All usable host addresses in the subnet the device sits on (excludes network/broadcast). */
    fun subnetHosts(link: LinkAddress, cap: Int = 1024): List<String> {
        val prefix = link.prefixLength.coerceIn(8, 32)
        val base = ipToLong(link.address.hostAddress ?: return emptyList())
        val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix)) and 0xFFFFFFFFL
        val network = base and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val first = network + 1
        val last = broadcast - 1
        if (last < first) return emptyList()
        val total = (last - first + 1).coerceAtMost(cap.toLong())
        return (0 until total).map { longToIp(first + it) }
    }

    /**
     * Expand a user-entered target into host IPs. Accepts:
     *  - a single IP:            203.0.113.4
     *  - CIDR:                   203.0.113.0/28
     *  - a dash range:           203.0.113.10-203.0.113.40  or  203.0.113.10-40
     */
    fun expandTargets(input: String, cap: Int = 4096): List<String> {
        val s = input.trim()
        return when {
            s.contains("/") -> expandCidr(s, cap)
            s.contains("-") -> expandDash(s, cap)
            isIpv4(s) -> listOf(s)
            else -> emptyList()
        }
    }

    private fun expandCidr(cidr: String, cap: Int): List<String> {
        val (ip, prefixStr) = cidr.split("/", limit = 2)
        if (!isIpv4(ip)) return emptyList()
        val prefix = prefixStr.toIntOrNull()?.coerceIn(0, 32) ?: return emptyList()
        val base = ipToLong(ip)
        val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix)) and 0xFFFFFFFFL
        val network = base and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val first = if (prefix >= 31) network else network + 1
        val last = if (prefix >= 31) broadcast else broadcast - 1
        if (last < first) return emptyList()
        val total = (last - first + 1).coerceAtMost(cap.toLong())
        return (0 until total).map { longToIp(first + it) }
    }

    private fun expandDash(range: String, cap: Int): List<String> {
        val (a, b) = range.split("-", limit = 2).map { it.trim() }
        if (!isIpv4(a)) return emptyList()
        val start = ipToLong(a)
        val end = when {
            isIpv4(b) -> ipToLong(b)
            b.toIntOrNull() != null -> (start and 0xFFFFFF00L) or (b.toInt().toLong() and 0xFF)
            else -> return emptyList()
        }
        if (end < start) return emptyList()
        val total = (end - start + 1).coerceAtMost(cap.toLong())
        return (0 until total).map { longToIp(start + it) }
    }

    fun isIpv4(s: String): Boolean {
        val parts = s.split(".")
        if (parts.size != 4) return false
        return parts.all { p -> p.toIntOrNull()?.let { it in 0..255 } == true }
    }

    fun ipToLong(ip: String): Long {
        val p = ip.split(".")
        return (p[0].toLong() shl 24) or (p[1].toLong() shl 16) or
            (p[2].toLong() shl 8) or p[3].toLong()
    }

    fun longToIp(v: Long): String =
        "${(v shr 24) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 8) and 0xFF}.${v and 0xFF}"
}

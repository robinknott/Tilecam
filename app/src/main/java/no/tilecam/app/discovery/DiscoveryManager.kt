package no.tilecam.app.discovery

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.data.model.DiscoveryVia
import no.tilecam.app.data.model.ScanConfig
import no.tilecam.app.data.vendor.Vendor

/** Live progress emitted during a scan so the UI can show what's happening. */
data class ScanProgress(
    val phase: String,
    val done: Int = 0,
    val total: Int = 0,
    val found: Int = 0,
)

/**
 * Coordinates every discovery method and merges the results by host, keeping
 * the richest signal for each device (ONVIF beats a bare open port).
 */
class DiscoveryManager(private val context: Context) {

    private val onvif = OnvifDiscovery(context)
    private val mdns = MdnsDiscovery(context)

    /** Full LAN discovery: ONVIF WS-Discovery + mDNS + a subnet port sweep. */
    suspend fun scanLan(
        onProgress: (ScanProgress) -> Unit = {},
        onFound: (DiscoveredDevice) -> Unit = {},
    ): List<DiscoveredDevice> {
        val merged = LinkedHashMap<String, DiscoveredDevice>()
        fun absorb(d: DiscoveredDevice) {
            val prev = merged[d.host]
            merged[d.host] = prev?.let { mergeDevice(it, d) } ?: d
            onFound(merged.getValue(d.host))
        }

        onProgress(ScanProgress("ONVIF auto-discovery"))
        onvif.discover(listenMs = 4000) { absorb(it) }
            .forEach { absorb(it) }
        onProgress(ScanProgress("ONVIF auto-discovery", found = merged.size))

        onProgress(ScanProgress("mDNS", found = merged.size))
        withTimeoutOrNull(3500) {
            mdns.discover().take(50).toList()
        }?.forEach { absorb(it) }

        val link = NetworkUtils.activeIpv4(context)
        if (link != null) {
            val hosts = NetworkUtils.subnetHosts(link)
            onProgress(ScanProgress("Scanning subnet", 0, hosts.size, merged.size))
            RangeScanner(timeoutMs = 500, maxConcurrency = 64).scan(
                hosts = hosts,
                ports = Vendor.COMMON_SCAN_PORTS,
                via = DiscoveryVia.LAN,
                onProgress = { done, total ->
                    onProgress(ScanProgress("Scanning subnet", done, total, merged.size))
                },
                onFound = { absorb(it) },
            )
        }

        onProgress(ScanProgress("Done", found = merged.size))
        return merged.values.sortedBy { NetworkUtils.ipSortKey(it.host) }
    }

    /** WAN / arbitrary IP-range scan with port scanning. */
    suspend fun scanWan(
        config: ScanConfig,
        onProgress: (ScanProgress) -> Unit = {},
        onFound: (DiscoveredDevice) -> Unit = {},
    ): List<DiscoveredDevice> {
        val hosts = NetworkUtils.expandTargets(config.cidrOrRange)
        if (hosts.isEmpty()) {
            onProgress(ScanProgress("Invalid range"))
            return emptyList()
        }
        onProgress(ScanProgress("Scanning ${hosts.size} hosts", 0, hosts.size))
        return RangeScanner(config.timeoutMs, config.maxConcurrency).scan(
            hosts = hosts,
            ports = config.ports,
            via = DiscoveryVia.WAN,
            onProgress = { done, total ->
                onProgress(ScanProgress("Scanning", done, total))
            },
            onFound = onFound,
        ).sortedBy { NetworkUtils.ipSortKey(it.host) }
    }

    private fun mergeDevice(a: DiscoveredDevice, b: DiscoveredDevice): DiscoveredDevice {
        // Prefer ONVIF info; union open ports; keep the most specific vendor.
        val vendor = when {
            a.vendorId != Vendor.UNKNOWN && a.vendorId != Vendor.GENERIC_ONVIF -> a.vendorId
            b.vendorId != Vendor.UNKNOWN && b.vendorId != Vendor.GENERIC_ONVIF -> b.vendorId
            a.vendorId != Vendor.UNKNOWN -> a.vendorId
            else -> b.vendorId
        }
        return a.copy(
            openPorts = (a.openPorts + b.openPorts).distinct().sorted(),
            vendorId = vendor,
            onvifXAddr = a.onvifXAddr ?: b.onvifXAddr,
            onvifPort = a.onvifPort ?: b.onvifPort,
            rtspPort = a.rtspPort ?: b.rtspPort,
            hostname = a.hostname ?: b.hostname,
            httpServerHeader = a.httpServerHeader ?: b.httpServerHeader,
            via = if (a.onvifXAddr != null) DiscoveryVia.ONVIF else a.via,
        )
    }
}

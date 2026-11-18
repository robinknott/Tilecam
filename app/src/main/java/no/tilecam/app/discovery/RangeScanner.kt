package no.tilecam.app.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.data.model.DiscoveryVia
import no.tilecam.app.data.vendor.Vendor
import no.tilecam.app.data.vendor.VendorDatabase

/**
 * Scans a set of hosts over a set of ports with bounded concurrency.
 * Shared by the LAN sweep and the WAN/IP-range scan — the only difference is
 * which hosts go in.
 */
class RangeScanner(
    private val timeoutMs: Int = 600,
    private val maxConcurrency: Int = 64,
) {
    /**
     * @param onProgress called as (hostsDone, hostsTotal) for UI feedback.
     * @param onFound called the moment a camera-like host is confirmed.
     */
    suspend fun scan(
        hosts: List<String>,
        ports: List<Int>,
        via: DiscoveryVia,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        onFound: (DiscoveredDevice) -> Unit = {},
    ): List<DiscoveredDevice> = coroutineScope {
        val gate = Semaphore(maxConcurrency)
        var done = 0
        val total = hosts.size
        val lock = Any()

        val jobs = hosts.map { host ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    val device = probeHost(host, ports, via)
                    synchronized(lock) {
                        done++
                        onProgress(done, total)
                    }
                    if (device != null) onFound(device)
                    device
                }
            }
        }
        jobs.awaitAll().filterNotNull()
    }

    private suspend fun probeHost(
        host: String,
        ports: List<Int>,
        via: DiscoveryVia,
    ): DiscoveredDevice? {
        val open = mutableListOf<Int>()
        for (p in ports) {
            if (PortProbe.isOpen(host, p, timeoutMs)) open += p
        }
        if (open.isEmpty()) return null

        // Only report hosts that actually look like cameras, not every web server.
        val signal = open.any { it in DiscoveredDevice.CAMERA_SIGNAL_PORTS }
        val httpPort = open.firstOrNull { it in listOf(80, 8080, 443, 88) }
        val serverHeader = httpPort?.let { PortProbe.httpServerHeader(host, it, timeoutMs) }

        if (!signal && serverHeader == null) return null

        val vendor = VendorDatabase.detect(
            serverHeader = serverHeader,
            openPorts = open,
        )
        // A plain web server with no camera port and an unrecognised vendor is skipped.
        if (!signal && vendor.id == Vendor.GENERIC_ONVIF) return null

        return DiscoveredDevice(
            host = host,
            openPorts = open.sorted(),
            vendorId = vendor.id,
            onvifPort = open.firstOrNull { it in vendor.onvifPorts } ?: httpPort,
            rtspPort = open.firstOrNull { it == 554 || it == 8554 },
            httpServerHeader = serverHeader,
            via = via,
        )
    }
}

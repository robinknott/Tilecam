package no.tilecam.app.data.model

import kotlinx.serialization.Serializable
import no.tilecam.app.data.vendor.Vendor

/**
 * A camera the user has saved and wants on the wall.
 * Credentials are stored separately (encrypted); only a reference is kept here.
 */
@Serializable
data class Camera(
    val id: String,
    val name: String,
    val host: String,
    val vendorId: String = Vendor.GENERIC_ONVIF,
    val onvifPort: Int = 80,
    val rtspPort: Int = 554,
    /** Resolved RTSP URL (without credentials) once we know it. */
    val rtspPath: String? = null,
    /** How the stream URL was obtained. */
    val source: StreamSource = StreamSource.UNKNOWN,
    val model: String? = null,
    val firmware: String? = null,
    val position: Int = 0,
    val muted: Boolean = true,
)

enum class StreamSource { ONVIF, VENDOR_TEMPLATE, MANUAL, UNKNOWN }

/**
 * A device found during a scan, before the user has signed in.
 */
@Serializable
data class DiscoveredDevice(
    val host: String,
    val openPorts: List<Int> = emptyList(),
    val vendorId: String = Vendor.UNKNOWN,
    val onvifXAddr: String? = null,   // from WS-Discovery
    val onvifPort: Int? = null,
    val rtspPort: Int? = null,
    val hostname: String? = null,     // from mDNS / reverse lookup
    val httpServerHeader: String? = null,
    val via: DiscoveryVia = DiscoveryVia.LAN,
) {
    val looksLikeCamera: Boolean
        get() = onvifXAddr != null ||
            rtspPort != null ||
            openPorts.any { it in CAMERA_SIGNAL_PORTS }

    companion object {
        val CAMERA_SIGNAL_PORTS = setOf(554, 8554, 88, 8000, 8899, 34567, 37777, 9000)
    }
}

enum class DiscoveryVia { ONVIF, MDNS, LAN, WAN }

/** One streamable profile reported by an ONVIF device. */
@Serializable
data class StreamProfile(
    val token: String,
    val name: String,
    val rtspUri: String,
    val width: Int = 0,
    val height: Int = 0,
    val isMain: Boolean = false,
)

/** Everything a WAN scan needs. */
data class ScanConfig(
    val cidrOrRange: String,
    val ports: List<Int> = Vendor.COMMON_SCAN_PORTS,
    val timeoutMs: Int = 600,
    val maxConcurrency: Int = 64,
)

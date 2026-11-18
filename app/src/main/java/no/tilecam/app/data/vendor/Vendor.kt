package no.tilecam.app.data.vendor

/**
 * A camera manufacturer profile: how to recognise it, the factory-default
 * login it ships with, and the RTSP URL shape its firmware uses.
 *
 * The default credentials here are the manufacturers' own documented factory
 * defaults, kept so the app can pre-fill the right username (and offer the
 * documented default password) for the user's *own* cameras. Most modern
 * cameras force a password change on first setup — the pre-fill is a
 * convenience, never a guarantee.
 */
data class Vendor(
    val id: String,
    val displayName: String,
    val defaultUsernames: List<String>,
    val defaultPasswords: List<String>,
    val onvifPorts: List<Int> = listOf(80),
    val rtspPort: Int = 554,
    /** RTSP path for the main (high-res) stream. Tokens: {channel} {stream}. */
    val mainPath: String? = null,
    /** RTSP path for the sub (low-res) stream. */
    val subPath: String? = null,
    /** RTSP path for time-ranged SD-card/NVR playback. Tokens: {channel} {start} {end}. */
    val playbackPath: String? = null,
    /** How this vendor formats the start/end timestamps in [playbackPath]. */
    val playbackTimeFormat: PlaybackTimeFormat = PlaybackTimeFormat.NONE,
    // ---- detection signatures ----
    val onvifManufacturerPatterns: List<String> = emptyList(),
    val serverHeaderPatterns: List<String> = emptyList(),
    val hostnamePatterns: List<String> = emptyList(),
    /** Ports that strongly hint at this vendor (e.g. Dahua private port 37777). */
    val signalPorts: List<Int> = emptyList(),
) {
    /** Timestamp format used by a vendor's playback-by-time RTSP URL. */
    enum class PlaybackTimeFormat {
        /** Hikvision: ISO-8601 basic with 'Z' suffix, in the device's local time, e.g. 20261118T200000Z */
        HIK_ISO,
        /** Dahua family: local time, e.g. 2026_11_18_20_00_00 */
        DAHUA_LOCAL,
        NONE,
    }

    val supportsPlayback: Boolean get() = playbackPath != null

    val defaultUsername: String get() = defaultUsernames.firstOrNull() ?: "admin"
    val defaultPassword: String get() = defaultPasswords.firstOrNull() ?: ""

    companion object {
        const val GENERIC_ONVIF = "onvif"
        const val UNKNOWN = "unknown"
        val COMMON_SCAN_PORTS = listOf(554, 8554, 80, 8080, 88, 8000, 8899, 34567, 37777, 443)
    }
}

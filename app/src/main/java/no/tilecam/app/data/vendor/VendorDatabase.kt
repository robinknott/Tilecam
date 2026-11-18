package no.tilecam.app.data.vendor

/**
 * Registry of camera manufacturers Tilecam can recognise and talk to without
 * ONVIF, plus the vendor each discovered device most likely belongs to.
 *
 * ONVIF remains the universal path: any ONVIF-conformant camera (the large
 * majority sold today) is handled generically by querying GetStreamUri. This
 * table adds direct RTSP URLs and correct default usernames for the common
 * brands, so a camera works even when its ONVIF service is off or locked down.
 */
object VendorDatabase {

    val vendors: List<Vendor> = listOf(
        Vendor(
            id = "hikvision",
            displayName = "Hikvision",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf("", "12345"),
            onvifPorts = listOf(80, 8000),
            mainPath = "/Streaming/Channels/{channel}01",
            subPath = "/Streaming/Channels/{channel}02",
            playbackPath = "/Streaming/tracks/{channel}01?starttime={start}&endtime={end}",
            playbackTimeFormat = Vendor.PlaybackTimeFormat.HIK_ISO_UTC,
            onvifManufacturerPatterns = listOf("hikvision"),
            serverHeaderPatterns = listOf("hikvision", "dnvrs-webs", "web server"),
            hostnamePatterns = listOf("hikvision", "ds-", "hik-"),
            signalPorts = listOf(8000),
        ),
        Vendor(
            id = "annke",
            displayName = "Annke",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            onvifPorts = listOf(80, 8000),
            mainPath = "/Streaming/Channels/{channel}01",
            subPath = "/Streaming/Channels/{channel}02",
            playbackPath = "/Streaming/tracks/{channel}01?starttime={start}&endtime={end}",
            playbackTimeFormat = Vendor.PlaybackTimeFormat.HIK_ISO_UTC,
            onvifManufacturerPatterns = listOf("annke"),
            hostnamePatterns = listOf("annke"),
        ),
        Vendor(
            id = "dahua",
            displayName = "Dahua",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf("admin", ""),
            onvifPorts = listOf(80),
            mainPath = "/cam/realmonitor?channel={channel}&subtype=0",
            subPath = "/cam/realmonitor?channel={channel}&subtype=1",
            playbackPath = "/cam/playback?channel={channel}&starttime={start}&endtime={end}",
            playbackTimeFormat = Vendor.PlaybackTimeFormat.DAHUA_LOCAL,
            onvifManufacturerPatterns = listOf("dahua"),
            serverHeaderPatterns = listOf("dahua", "webs"),
            hostnamePatterns = listOf("dahua", "dh-", "ipc-"),
            signalPorts = listOf(37777),
        ),
        Vendor(
            id = "amcrest",
            displayName = "Amcrest",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf("admin", ""),
            mainPath = "/cam/realmonitor?channel={channel}&subtype=0",
            subPath = "/cam/realmonitor?channel={channel}&subtype=1",
            playbackPath = "/cam/playback?channel={channel}&starttime={start}&endtime={end}",
            playbackTimeFormat = Vendor.PlaybackTimeFormat.DAHUA_LOCAL,
            onvifManufacturerPatterns = listOf("amcrest"),
            hostnamePatterns = listOf("amcrest", "amc-"),
            signalPorts = listOf(37777),
        ),
        Vendor(
            id = "lorex",
            displayName = "Lorex",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            mainPath = "/cam/realmonitor?channel={channel}&subtype=0",
            subPath = "/cam/realmonitor?channel={channel}&subtype=1",
            playbackPath = "/cam/playback?channel={channel}&starttime={start}&endtime={end}",
            playbackTimeFormat = Vendor.PlaybackTimeFormat.DAHUA_LOCAL,
            onvifManufacturerPatterns = listOf("lorex"),
            hostnamePatterns = listOf("lorex"),
            signalPorts = listOf(37777),
        ),
        Vendor(
            id = "reolink",
            displayName = "Reolink",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            onvifPorts = listOf(8000),
            mainPath = "/h264Preview_{channel}_main",
            subPath = "/h264Preview_{channel}_sub",
            onvifManufacturerPatterns = listOf("reolink"),
            serverHeaderPatterns = listOf("reolink"),
            hostnamePatterns = listOf("reolink", "rl-", "cam-"),
            signalPorts = listOf(9000),
        ),
        Vendor(
            id = "axis",
            displayName = "Axis",
            defaultUsernames = listOf("root"),
            defaultPasswords = listOf("pass"),
            mainPath = "/axis-media/media.amp",
            subPath = "/axis-media/media.amp?resolution=640x480",
            onvifManufacturerPatterns = listOf("axis"),
            serverHeaderPatterns = listOf("axis"),
            hostnamePatterns = listOf("axis"),
        ),
        Vendor(
            id = "hanwha",
            displayName = "Hanwha / Wisenet",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            mainPath = "/profile2/media.smp",
            subPath = "/profile5/media.smp",
            onvifManufacturerPatterns = listOf("hanwha", "wisenet", "samsung techwin"),
            hostnamePatterns = listOf("wisenet", "hanwha"),
        ),
        Vendor(
            id = "uniview",
            displayName = "Uniview (UNV)",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf("123456"),
            mainPath = "/unicast/c{channel}/s0/live",
            subPath = "/unicast/c{channel}/s1/live",
            onvifManufacturerPatterns = listOf("uniview", "unv"),
            hostnamePatterns = listOf("uniview", "unv"),
        ),
        Vendor(
            id = "vivotek",
            displayName = "Vivotek",
            defaultUsernames = listOf("root"),
            defaultPasswords = listOf(""),
            mainPath = "/live.sdp",
            subPath = "/live2.sdp",
            onvifManufacturerPatterns = listOf("vivotek"),
            hostnamePatterns = listOf("vivotek"),
        ),
        Vendor(
            id = "foscam",
            displayName = "Foscam",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            onvifPorts = listOf(888, 88),
            mainPath = "/videoMain",
            subPath = "/videoSub",
            onvifManufacturerPatterns = listOf("foscam"),
            hostnamePatterns = listOf("foscam"),
            signalPorts = listOf(88),
        ),
        Vendor(
            id = "tplink",
            displayName = "TP-Link (Tapo / VIGI)",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            mainPath = "/stream1",
            subPath = "/stream2",
            onvifManufacturerPatterns = listOf("tp-link", "tplink", "tapo", "vigi"),
            hostnamePatterns = listOf("tapo", "vigi", "tp-link"),
        ),
        Vendor(
            id = "dlink",
            displayName = "D-Link",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            mainPath = "/live1.sdp",
            subPath = "/live2.sdp",
            onvifManufacturerPatterns = listOf("d-link", "dlink"),
            hostnamePatterns = listOf("dlink", "dcs-"),
        ),
        Vendor(
            id = "trendnet",
            displayName = "TRENDnet",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf("admin"),
            mainPath = "/play1.sdp",
            subPath = "/play2.sdp",
            onvifManufacturerPatterns = listOf("trendnet"),
            hostnamePatterns = listOf("trendnet", "tv-ip"),
        ),
        Vendor(
            id = "bosch",
            displayName = "Bosch",
            defaultUsernames = listOf("service"),
            defaultPasswords = listOf(""),
            mainPath = "/rtsp_tunnel",
            subPath = "/rtsp_tunnel?line=1&inst=2",
            onvifManufacturerPatterns = listOf("bosch"),
            hostnamePatterns = listOf("bosch"),
        ),
        Vendor(
            id = "panasonic",
            displayName = "Panasonic / i-PRO",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf("12345"),
            mainPath = "/MediaInput/h264",
            subPath = "/MediaInput/h264/stream_2",
            onvifManufacturerPatterns = listOf("panasonic", "i-pro"),
            hostnamePatterns = listOf("panasonic", "i-pro"),
        ),
        Vendor(
            id = "wyze",
            displayName = "Wyze (RTSP firmware)",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            mainPath = "/live",
            subPath = "/live",
            hostnamePatterns = listOf("wyze"),
        ),
        Vendor(
            id = "hisilicon",
            displayName = "Generic HiSilicon / XMeye",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            mainPath = "/user={user}&password={pass}&channel={channel}&stream=0.sdp",
            subPath = "/user={user}&password={pass}&channel={channel}&stream=1.sdp",
            hostnamePatterns = listOf("xmeye", "ipcam"),
            signalPorts = listOf(34567),
        ),
        Vendor(
            id = Vendor.GENERIC_ONVIF,
            displayName = "ONVIF camera",
            defaultUsernames = listOf("admin"),
            defaultPasswords = listOf(""),
            onvifPorts = listOf(80, 8080, 8000, 2020),
            // No fixed path — resolved live via ONVIF GetStreamUri.
            onvifManufacturerPatterns = emptyList(),
        ),
    )

    private val byId = vendors.associateBy { it.id }

    fun byId(id: String?): Vendor = byId[id] ?: byId.getValue(Vendor.GENERIC_ONVIF)

    /** Best-effort vendor guess from whatever signals a scan collected. */
    fun detect(
        onvifManufacturer: String? = null,
        serverHeader: String? = null,
        hostname: String? = null,
        openPorts: List<Int> = emptyList(),
    ): Vendor {
        val mfr = onvifManufacturer?.lowercase()
        val srv = serverHeader?.lowercase()
        val host = hostname?.lowercase()

        // 1) ONVIF manufacturer string is the most reliable.
        if (mfr != null) {
            vendors.firstOrNull { v -> v.onvifManufacturerPatterns.any { mfr.contains(it) } }
                ?.let { return it }
        }
        // 2) HTTP Server header.
        if (srv != null) {
            vendors.firstOrNull { v -> v.serverHeaderPatterns.any { srv.contains(it) } }
                ?.let { return it }
        }
        // 3) Hostname / mDNS name.
        if (host != null) {
            vendors.firstOrNull { v -> v.hostnamePatterns.any { host.contains(it) } }
                ?.let { return it }
        }
        // 4) Distinctive private ports.
        vendors.firstOrNull { v -> v.signalPorts.isNotEmpty() && openPorts.any { it in v.signalPorts } }
            ?.let { return it }

        return byId.getValue(Vendor.GENERIC_ONVIF)
    }

    /**
     * Build a credentialed RTSP URL from a vendor template.
     * Returns null when the vendor has no template (use ONVIF instead).
     */
    fun buildRtspUrl(
        vendor: Vendor,
        host: String,
        user: String,
        pass: String,
        port: Int = vendor.rtspPort,
        channel: Int = 1,
        mainStream: Boolean = true,
    ): String? {
        val template = (if (mainStream) vendor.mainPath else vendor.subPath) ?: return null
        val path = template
            .replace("{channel}", channel.toString())
            .replace("{user}", user)
            .replace("{pass}", pass)
        val creds = if (user.isNotEmpty())
            "${urlEncode(user)}:${urlEncode(pass)}@" else ""
        val sep = if (path.startsWith("/")) "" else "/"
        return "rtsp://$creds$host:$port$sep$path"
    }

    /**
     * Build a credentialed RTSP URL that plays back SD-card/NVR footage for a
     * time window. Returns null when the vendor has no playback template.
     */
    fun buildPlaybackUrl(
        vendor: Vendor,
        host: String,
        user: String,
        pass: String,
        startMillis: Long,
        endMillis: Long,
        port: Int = vendor.rtspPort,
        channel: Int = 1,
    ): String? {
        val template = vendor.playbackPath ?: return null
        val start = formatPlaybackTime(startMillis, vendor.playbackTimeFormat) ?: return null
        val end = formatPlaybackTime(endMillis, vendor.playbackTimeFormat) ?: return null
        val path = template
            .replace("{channel}", channel.toString())
            .replace("{start}", start)
            .replace("{end}", end)
        val creds = if (user.isNotEmpty())
            "${urlEncode(user)}:${urlEncode(pass)}@" else ""
        val sep = if (path.startsWith("/")) "" else "/"
        return "rtsp://$creds$host:$port$sep$path"
    }

    private fun formatPlaybackTime(millis: Long, fmt: Vendor.PlaybackTimeFormat): String? {
        val pattern: String
        val tz: java.util.TimeZone
        when (fmt) {
            Vendor.PlaybackTimeFormat.HIK_ISO_UTC -> {
                pattern = "yyyyMMdd'T'HHmmss'Z'"
                tz = java.util.TimeZone.getTimeZone("UTC")
            }
            Vendor.PlaybackTimeFormat.DAHUA_LOCAL -> {
                pattern = "yyyy_MM_dd_HH_mm_ss"
                tz = java.util.TimeZone.getDefault()
            }
            Vendor.PlaybackTimeFormat.NONE -> return null
        }
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
        sdf.timeZone = tz
        return sdf.format(java.util.Date(millis))
    }

    private fun urlEncode(s: String): String =
        s.replace("%", "%25")
            .replace(":", "%3A")
            .replace("@", "%40")
            .replace("/", "%2F")
            .replace(" ", "%20")
            .replace("?", "%3F")
            .replace("#", "%23")
}

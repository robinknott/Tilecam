package no.tilecam.app.data

import no.tilecam.app.data.model.Camera
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.data.model.StreamProfile
import no.tilecam.app.data.model.StreamSource
import no.tilecam.app.data.vendor.Vendor
import no.tilecam.app.data.vendor.VendorDatabase
import no.tilecam.app.onvif.OnvifClient
import java.util.UUID

sealed interface ConnectResult {
    data class Success(
        val camera: Camera,
        val profiles: List<StreamProfile>,
    ) : ConnectResult

    data class Failure(val reason: String) : ConnectResult
}

/**
 * Turns a discovered device + credentials into a playable Camera.
 * Strategy: try ONVIF (works for most brands and gives the exact RTSP URI),
 * then fall back to the vendor's RTSP URL template.
 */
class ConnectionResolver {

    suspend fun connect(
        device: DiscoveredDevice,
        username: String,
        password: String,
        displayName: String? = null,
    ): ConnectResult {
        val vendor = VendorDatabase.byId(device.vendorId)
        val onvifPort = device.onvifPort ?: vendor.onvifPorts.first()

        // 1) ONVIF — the universal path.
        val onvif = OnvifClient(device.host, onvifPort, username, password)
        val info = onvif.getDeviceInformation()
        val profiles = onvif.getStreamProfiles()
        if (profiles.isNotEmpty()) {
            val detected = info?.manufacturer?.let {
                VendorDatabase.detect(onvifManufacturer = it)
            } ?: vendor
            val main = profiles.first()
            val camera = Camera(
                id = UUID.randomUUID().toString(),
                name = displayName ?: info?.model ?: detected.displayName + " " + device.host,
                host = device.host,
                vendorId = detected.id,
                onvifPort = onvifPort,
                rtspPort = device.rtspPort ?: vendor.rtspPort,
                rtspPath = main.rtspUri,
                source = StreamSource.ONVIF,
                model = info?.model,
                firmware = info?.firmware,
            )
            return ConnectResult.Success(camera, profiles)
        }

        // 2) Vendor RTSP template.
        val rtspPort = device.rtspPort ?: vendor.rtspPort
        val url = VendorDatabase.buildRtspUrl(
            vendor = vendor,
            host = device.host,
            user = username,
            pass = password,
            port = rtspPort,
        )
        if (url != null) {
            val camera = Camera(
                id = UUID.randomUUID().toString(),
                name = displayName ?: "${vendor.displayName} ${device.host}",
                host = device.host,
                vendorId = vendor.id,
                onvifPort = onvifPort,
                rtspPort = rtspPort,
                rtspPath = url,
                source = StreamSource.VENDOR_TEMPLATE,
            )
            return ConnectResult.Success(
                camera,
                listOf(StreamProfile("main", "Main", url, isMain = true)),
            )
        }

        return ConnectResult.Failure(
            "Could not reach the camera over ONVIF, and no RTSP template is known " +
                "for this vendor. Check the username/password, or add the RTSP URL manually.",
        )
    }

    /** Re-resolve a stored camera's playable URL with saved credentials. */
    fun playableUrl(camera: Camera, creds: Credentials?): String? {
        camera.rtspPath?.let { path ->
            if (path.contains("@") || creds == null) return path
            // Stored path without creds (e.g. manual) — inject now.
            return path.replaceFirst(
                "rtsp://",
                "rtsp://${creds.username}:${creds.password}@",
            )
        }
        if (creds == null) return null
        val vendor = VendorDatabase.byId(camera.vendorId)
        return VendorDatabase.buildRtspUrl(
            vendor, camera.host, creds.username, creds.password, camera.rtspPort,
        )
    }
}

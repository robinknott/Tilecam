package no.tilecam.app.data

import no.tilecam.app.data.model.Camera
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.data.model.StreamProfile
import no.tilecam.app.data.model.StreamSource
import no.tilecam.app.data.vendor.Vendor
import no.tilecam.app.data.vendor.VendorDatabase
import no.tilecam.app.discovery.RtspProbe
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
        val rtspPort = device.rtspPort ?: vendor.rtspPort

        // 1) When the vendor's RTSP path is known, verify the login over RTSP first.
        //    This fails fast and clearly on a wrong username/password.
        val cleanTemplate = VendorDatabase.buildRtspUrl(vendor, device.host, "", "", rtspPort)
        if (cleanTemplate != null) {
            when (RtspProbe.check(cleanTemplate, username, password)) {
                RtspProbe.Auth.UNAUTHORIZED ->
                    return ConnectResult.Failure("Feil brukernavn eller passord.")
                RtspProbe.Auth.OK ->
                    return onvifOrTemplate(device, vendor, onvifPort, rtspPort, username, password, displayName)
                RtspProbe.Auth.UNKNOWN -> { /* RTSP closed/unknown — try ONVIF below */ }
            }
        }

        // 2) ONVIF path (generic ONVIF cameras, or when the RTSP probe was inconclusive).
        val result = onvifOrTemplate(device, vendor, onvifPort, rtspPort, username, password, displayName)
        if (result is ConnectResult.Success) return result

        // 3) Classify the failure: is it bad credentials, or unreachable?
        val onvif = OnvifClient(device.host, onvifPort, username, password)
        return when (onvif.checkAuth()) {
            OnvifClient.AuthStatus.UNAUTHORIZED ->
                ConnectResult.Failure("Feil brukernavn eller passord.")
            OnvifClient.AuthStatus.UNREACHABLE ->
                ConnectResult.Failure(
                    "Fikk ikke kontakt med kameraet. Sjekk at IP-adressen stemmer, at " +
                        "ONVIF/RTSP er på, og at TV-en er på samme nett.",
                )
            OnvifClient.AuthStatus.OK -> result // reachable + authorized but no stream found
        }
    }

    /** Try ONVIF for the exact stream URL; fall back to the vendor RTSP template. */
    private suspend fun onvifOrTemplate(
        device: DiscoveredDevice,
        vendor: Vendor,
        onvifPort: Int,
        rtspPort: Int,
        username: String,
        password: String,
        displayName: String?,
    ): ConnectResult {
        val onvif = OnvifClient(device.host, onvifPort, username, password)
        val info = onvif.getDeviceInformation()
        val profiles = onvif.getStreamProfiles()
        if (profiles.isNotEmpty()) {
            val detected = info?.manufacturer?.let { VendorDatabase.detect(onvifManufacturer = it) } ?: vendor
            val main = profiles.first()
            val camera = Camera(
                id = UUID.randomUUID().toString(),
                name = displayName ?: info?.model ?: "${detected.displayName} ${device.host}",
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

        val url = VendorDatabase.buildRtspUrl(vendor, device.host, username, password, rtspPort)
            ?: return ConnectResult.Failure("no-stream")
        val camera = Camera(
            id = UUID.randomUUID().toString(),
            name = displayName ?: "${vendor.displayName} ${device.host}",
            host = device.host,
            vendorId = vendor.id,
            onvifPort = onvifPort,
            rtspPort = rtspPort,
            rtspPath = url,
            source = StreamSource.VENDOR_TEMPLATE,
            model = info?.model,
            firmware = info?.firmware,
        )
        return ConnectResult.Success(camera, listOf(StreamProfile("main", "Main", url, isMain = true)))
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

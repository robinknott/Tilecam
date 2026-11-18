package no.tilecam.app.data

import android.content.Context
import no.tilecam.app.discovery.DiscoveryManager

/** Simple manual dependency container (no DI framework needed for this scope). */
class AppContainer(context: Context) {
    val cameraStore = CameraStore(context)
    val credentialStore = CredentialStore(context)
    val discoveryManager = DiscoveryManager(context)
    val connectionResolver = ConnectionResolver()
}

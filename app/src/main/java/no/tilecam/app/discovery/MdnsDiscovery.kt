package no.tilecam.app.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.data.model.DiscoveryVia
import no.tilecam.app.data.vendor.VendorDatabase

/**
 * mDNS / DNS-SD discovery via Android's NsdManager. Many cameras advertise
 * _rtsp._tcp or _onvif._tcp; the service name usually carries the brand.
 */
class MdnsDiscovery(context: Context) {

    private val nsd = context.applicationContext
        .getSystemService(Context.NSD_SERVICE) as NsdManager

    private val serviceTypes = listOf("_rtsp._tcp.", "_onvif._tcp.", "_http._tcp.")

    fun discover(): Flow<DiscoveredDevice> = callbackFlow {
        val listeners = mutableListOf<Pair<String, NsdManager.DiscoveryListener>>()

        serviceTypes.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onServiceFound(info: NsdServiceInfo) {
                    nsd.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            val host = resolved.host?.hostAddress ?: return
                            val name = resolved.serviceName
                            val vendor = VendorDatabase.detect(hostname = name)
                            trySend(
                                DiscoveredDevice(
                                    host = host,
                                    openPorts = listOf(resolved.port),
                                    vendorId = vendor.id,
                                    rtspPort = if (type.startsWith("_rtsp")) resolved.port else null,
                                    onvifPort = if (type.startsWith("_onvif")) resolved.port else null,
                                    hostname = name,
                                    via = DiscoveryVia.MDNS,
                                )
                            )
                        }

                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                    })
                }

                override fun onServiceLost(info: NsdServiceInfo) {}
                override fun onDiscoveryStarted(type: String) {}
                override fun onDiscoveryStopped(type: String) {}
                override fun onStartDiscoveryFailed(type: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(type: String, errorCode: Int) {}
            }
            runCatching {
                nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                listeners += type to listener
            }
        }

        awaitClose {
            listeners.forEach { (_, l) -> runCatching { nsd.stopServiceDiscovery(l) } }
        }
    }
}

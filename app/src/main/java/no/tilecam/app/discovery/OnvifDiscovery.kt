package no.tilecam.app.discovery

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.data.model.DiscoveryVia
import no.tilecam.app.data.vendor.VendorDatabase
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.URI
import java.util.UUID

/**
 * ONVIF WS-Discovery: the universal way to find cameras on the LAN regardless
 * of brand. Sends a multicast Probe for NetworkVideoTransmitter devices and
 * collects ProbeMatch replies, pulling out each device's service address
 * (XAddrs) and scopes (which carry the manufacturer/model).
 */
class OnvifDiscovery(private val context: Context) {

    private val group = InetAddress.getByName("239.255.255.250")
    private val port = 3702

    suspend fun discover(
        listenMs: Int = 4000,
        onFound: (DiscoveredDevice) -> Unit = {},
    ): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val results = LinkedHashMap<String, DiscoveredDevice>()
        val lock = acquireMulticastLock()
        var socket: MulticastSocket? = null
        try {
            socket = MulticastSocket().apply {
                reuseAddress = true
                soTimeout = 500
                timeToLive = 2
            }
            val probe = probeEnvelope()
            val data = probe.toByteArray()
            // Send the probe a few times; multicast is lossy.
            repeat(2) {
                socket.send(DatagramPacket(data, data.size, InetSocketAddress(group, port)))
            }

            val buf = ByteArray(16 * 1024)
            val deadline = System.currentTimeMillis() + listenMs
            while (System.currentTimeMillis() < deadline) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(packet)
                } catch (_: Exception) {
                    continue // socket timeout → keep looping until deadline
                }
                val xml = String(packet.data, 0, packet.length)
                parseProbeMatch(xml)?.let { device ->
                    if (results.put(device.host, device) == null) onFound(device)
                }
            }
        } catch (_: Exception) {
            // Discovery is best-effort; fall back to port scanning.
        } finally {
            runCatching { socket?.close() }
            runCatching { lock?.release() }
        }
        results.values.toList()
    }

    private fun acquireMulticastLock(): WifiManager.MulticastLock? = try {
        val wifi = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifi?.createMulticastLock("tilecam-onvif")?.apply {
            setReferenceCounted(true)
            acquire()
        }
    } catch (_: Exception) {
        null
    }

    private fun probeEnvelope(): String {
        val id = "uuid:" + UUID.randomUUID()
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <e:Envelope xmlns:e="http://www.w3.org/2003/05/soap-envelope"
                xmlns:w="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery"
                xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
              <e:Header>
                <w:MessageID>$id</w:MessageID>
                <w:To e:mustUnderstand="true">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>
                <w:Action e:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>
              </e:Header>
              <e:Body>
                <d:Probe>
                  <d:Types>dn:NetworkVideoTransmitter</d:Types>
                </d:Probe>
              </e:Body>
            </e:Envelope>
        """.trimIndent()
    }

    private fun parseProbeMatch(xml: String): DiscoveredDevice? {
        val xaddrs = Regex("<[^>]*XAddrs>(.*?)</[^>]*XAddrs>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim() ?: return null
        val firstAddr = xaddrs.split(Regex("\\s+")).firstOrNull { it.startsWith("http") }
            ?: return null
        val uri = runCatching { URI(firstAddr) }.getOrNull() ?: return null
        val host = uri.host ?: return null

        val scopes = Regex("<[^>]*Scopes>(.*?)</[^>]*Scopes>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim().orEmpty()
        val manufacturer = scopeValue(scopes, "name") ?: scopeValue(scopes, "hardware")
        val vendor = VendorDatabase.detect(
            onvifManufacturer = manufacturer ?: scopes,
            hostname = host,
        )

        return DiscoveredDevice(
            host = host,
            openPorts = listOfNotNull(if (uri.port > 0) uri.port else 80),
            vendorId = vendor.id,
            onvifXAddr = firstAddr,
            onvifPort = if (uri.port > 0) uri.port else 80,
            via = DiscoveryVia.ONVIF,
        )
    }

    /** ONVIF scopes look like onvif://www.onvif.org/name/Acme%20Cam — pull a segment. */
    private fun scopeValue(scopes: String, key: String): String? {
        val m = Regex("onvif://www\\.onvif\\.org/$key/([^\\s]+)").find(scopes) ?: return null
        return java.net.URLDecoder.decode(m.groupValues[1], "UTF-8")
    }
}

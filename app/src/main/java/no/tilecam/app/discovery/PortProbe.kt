package no.tilecam.app.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * A single TCP connect probe. Returns true if the port accepts a connection
 * within [timeoutMs]. Also tries to read an HTTP Server header when the port
 * looks like a web port, which helps vendor detection.
 */
object PortProbe {

    suspend fun isOpen(host: String, port: Int, timeoutMs: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (_: IOException) {
                false
            } catch (_: Exception) {
                false
            }
        }

    /** Fetch the HTTP Server header (best effort) for vendor fingerprinting. */
    suspend fun httpServerHeader(host: String, port: Int, timeoutMs: Int): String? =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), timeoutMs)
                    s.soTimeout = timeoutMs
                    val req = "HEAD / HTTP/1.0\r\nHost: $host\r\nConnection: close\r\n\r\n"
                    s.getOutputStream().write(req.toByteArray())
                    s.getOutputStream().flush()
                    val text = s.getInputStream().bufferedReader().use { r ->
                        buildString {
                            var line = r.readLine()
                            var n = 0
                            while (line != null && line.isNotEmpty() && n < 40) {
                                append(line).append('\n'); n++
                                line = r.readLine()
                            }
                        }
                    }
                    text.lineSequence()
                        .firstOrNull { it.startsWith("Server:", ignoreCase = true) }
                        ?.substringAfter(":")?.trim()
                }
            } catch (_: Exception) {
                null
            }
        }
}

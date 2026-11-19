package no.tilecam.app.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Validates camera credentials by doing an RTSP DESCRIBE and checking whether
 * the server accepts the login (200) or rejects it (401). Handles both Basic
 * and Digest auth. This tests the exact stream the app will play, so it works
 * across vendors (Reolink, Hikvision, Dahua, …) without ONVIF.
 */
object RtspProbe {

    enum class Auth { OK, UNAUTHORIZED, UNKNOWN }

    suspend fun check(
        cleanRtspUrl: String,
        user: String,
        pass: String,
        timeoutMs: Int = 4000,
    ): Auth = withContext(Dispatchers.IO) {
        try {
            val uri = URI(cleanRtspUrl)
            val host = uri.host ?: return@withContext Auth.UNKNOWN
            val port = if (uri.port > 0) uri.port else 554
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), timeoutMs)
                s.soTimeout = timeoutMs
                val out = s.getOutputStream()
                val reader = s.getInputStream().bufferedReader()

                out.write(describe(cleanRtspUrl, 1, null).toByteArray()); out.flush()
                val first = readResponse(reader)
                when (first.status) {
                    200 -> return@withContext Auth.OK
                    401 -> {}
                    else -> return@withContext Auth.UNKNOWN
                }

                val authHeader = buildAuth(first.wwwAuth, user, pass, cleanRtspUrl)
                    ?: return@withContext Auth.UNKNOWN
                out.write(describe(cleanRtspUrl, 2, authHeader).toByteArray()); out.flush()
                val second = readResponse(reader)
                when (second.status) {
                    200 -> Auth.OK
                    401 -> Auth.UNAUTHORIZED
                    else -> Auth.UNKNOWN
                }
            }
        } catch (_: Exception) {
            Auth.UNKNOWN
        }
    }

    private data class RtspResponse(val status: Int, val wwwAuth: String?)

    private fun describe(url: String, cseq: Int, auth: String?): String = buildString {
        append("DESCRIBE ").append(url).append(" RTSP/1.0\r\n")
        append("CSeq: ").append(cseq).append("\r\n")
        append("User-Agent: Tilecam\r\n")
        append("Accept: application/sdp\r\n")
        if (auth != null) append("Authorization: ").append(auth).append("\r\n")
        append("\r\n")
    }

    private fun readResponse(reader: java.io.BufferedReader): RtspResponse {
        var status = -1
        var wwwAuth: String? = null
        var line = reader.readLine()
        var n = 0
        while (line != null && n < 60) {
            if (line.isEmpty()) break
            if (line.startsWith("RTSP/", ignoreCase = true)) {
                status = line.split(" ").getOrNull(1)?.toIntOrNull() ?: -1
            } else if (line.startsWith("WWW-Authenticate:", ignoreCase = true)) {
                wwwAuth = line.substringAfter(":").trim()
            }
            line = reader.readLine(); n++
        }
        return RtspResponse(status, wwwAuth)
    }

    private fun buildAuth(challenge: String?, user: String, pass: String, uri: String): String? {
        if (challenge == null) return null
        if (!challenge.trim().startsWith("Digest", ignoreCase = true)) {
            val token = android.util.Base64.encodeToString(
                "$user:$pass".toByteArray(), android.util.Base64.NO_WRAP,
            )
            return "Basic $token"
        }
        val p = parseChallenge(challenge)
        val realm = p["realm"].orEmpty()
        val nonce = p["nonce"].orEmpty()
        val qop = p["qop"]
        val ha1 = md5("$user:$realm:$pass")
        val ha2 = md5("DESCRIBE:$uri")
        return if (qop != null) {
            val cnonce = randomHex(8)
            val nc = "00000001"
            val response = md5("$ha1:$nonce:$nc:$cnonce:${qop.substringBefore(",").trim()}:$ha2")
            "Digest username=\"$user\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$uri\", " +
                "response=\"$response\", qop=auth, nc=$nc, cnonce=\"$cnonce\""
        } else {
            val response = md5("$ha1:$nonce:$ha2")
            "Digest username=\"$user\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$uri\", response=\"$response\""
        }
    }

    private fun parseChallenge(header: String): Map<String, String> {
        val body = header.trim().removePrefix("Digest").removePrefix("DIGEST").trim()
        val map = mutableMapOf<String, String>()
        Regex("(\\w+)=(\"([^\"]*)\"|([^,]*))").findAll(body).forEach { m ->
            map[m.groupValues[1].trim()] = (m.groupValues[3].ifEmpty { m.groupValues[4] }).trim()
        }
        return map
    }

    private fun md5(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8))
        return BigInteger(1, d).toString(16).padStart(32, '0')
    }

    private fun randomHex(bytes: Int): String {
        val b = ByteArray(bytes).also { SecureRandom().nextBytes(it) }
        return b.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }
}

package no.tilecam.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Minimal HTTP Digest (and Basic fallback) client for the camera CGI/ISAPI
 * endpoints that don't speak ONVIF SOAP — e.g. Hikvision's ISAPI recording
 * search. Does the 401-challenge/response handshake by hand so we don't need
 * an extra HTTP library.
 */
object HttpDigest {

    data class Response(val code: Int, val body: String?)

    suspend fun post(
        urlStr: String,
        user: String,
        pass: String,
        body: String,
        contentType: String = "application/xml",
        timeoutMs: Int = 7000,
    ): Response = request("POST", urlStr, user, pass, body, contentType, timeoutMs)

    suspend fun get(
        urlStr: String,
        user: String,
        pass: String,
        timeoutMs: Int = 7000,
    ): Response = request("GET", urlStr, user, pass, null, "application/xml", timeoutMs)

    private suspend fun request(
        method: String,
        urlStr: String,
        user: String,
        pass: String,
        body: String?,
        contentType: String,
        timeoutMs: Int,
    ): Response = withContext(Dispatchers.IO) {
        // First attempt (unauthenticated) to collect the challenge.
        val first = send(method, urlStr, null, body, contentType, timeoutMs)
        if (first.code != 401 || first.challenge == null) {
            return@withContext Response(first.code, first.body)
        }
        val auth = buildAuthHeader(method, URL(urlStr).path, user, pass, first.challenge)
        val second = send(method, urlStr, auth, body, contentType, timeoutMs)
        Response(second.code, second.body)
    }

    private data class Raw(val code: Int, val body: String?, val challenge: String?)

    private fun send(
        method: String,
        urlStr: String,
        authHeader: String?,
        body: String?,
        contentType: String,
        timeoutMs: Int,
    ): Raw {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                if (authHeader != null) setRequestProperty("Authorization", authHeader)
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", contentType)
                }
            }
            if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val challenge = conn.getHeaderField("WWW-Authenticate")
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }
            Raw(code, text, challenge)
        } catch (_: Exception) {
            Raw(-1, null, null)
        } finally {
            conn?.disconnect()
        }
    }

    private fun buildAuthHeader(
        method: String,
        uri: String,
        user: String,
        pass: String,
        challenge: String,
    ): String {
        if (!challenge.trim().startsWith("Digest", ignoreCase = true)) {
            // Basic fallback.
            val token = android.util.Base64.encodeToString(
                "$user:$pass".toByteArray(), android.util.Base64.NO_WRAP,
            )
            return "Basic $token"
        }
        val params = parseChallenge(challenge)
        val realm = params["realm"].orEmpty()
        val nonce = params["nonce"].orEmpty()
        val qop = params["qop"]
        val opaque = params["opaque"]
        val cnonce = randomHex(16)
        val nc = "00000001"
        val ha1 = md5("$user:$realm:$pass")
        val ha2 = md5("$method:$uri")
        val response = if (qop != null)
            md5("$ha1:$nonce:$nc:$cnonce:${qop.substringBefore(",").trim()}:$ha2")
        else
            md5("$ha1:$nonce:$ha2")

        return buildString {
            append("Digest ")
            append("username=\"$user\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$uri\", ")
            append("algorithm=MD5, response=\"$response\"")
            if (qop != null) append(", qop=auth, nc=$nc, cnonce=\"$cnonce\"")
            if (opaque != null) append(", opaque=\"$opaque\"")
        }
    }

    private fun parseChallenge(header: String): Map<String, String> {
        val body = header.trim().removePrefix("Digest").removePrefix("DIGEST").trim()
        val map = mutableMapOf<String, String>()
        Regex("(\\w+)=(\"([^\"]*)\"|([^,]*))").findAll(body).forEach { m ->
            val key = m.groupValues[1].trim()
            val value = (m.groupValues[3].ifEmpty { m.groupValues[4] }).trim()
            map[key] = value
        }
        return map
    }

    private fun md5(s: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8))
        return BigInteger(1, digest).toString(16).padStart(32, '0')
    }

    private fun randomHex(bytes: Int): String {
        val b = ByteArray(bytes).also { SecureRandom().nextBytes(it) }
        return b.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }
}

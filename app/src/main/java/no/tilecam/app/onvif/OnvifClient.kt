package no.tilecam.app.onvif

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.tilecam.app.data.model.StreamProfile
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Device identity reported over ONVIF. */
data class OnvifDeviceInfo(
    val manufacturer: String?,
    val model: String?,
    val firmware: String?,
)

/**
 * Minimal ONVIF Media client. Authenticates with WS-UsernameToken (digest)
 * and resolves the real RTSP URIs for every media profile on the device.
 * This is what lets Tilecam work with essentially any ONVIF-conformant camera
 * without a per-model driver.
 */
class OnvifClient(
    private val host: String,
    private val port: Int,
    private val user: String,
    private val pass: String,
    private val timeoutMs: Int = 4000,
) {
    private val deviceUrl = "http://$host:$port/onvif/device_service"

    enum class AuthStatus { OK, UNAUTHORIZED, UNREACHABLE }

    /** Quick credential check via GetDeviceInformation. */
    suspend fun checkAuth(): AuthStatus = withContext(Dispatchers.IO) {
        val body = soap(
            "http://www.onvif.org/ver10/device/wsdl", "<tds:GetDeviceInformation/>", nsTds = true,
        )
        val (code, resp) = postRaw(
            deviceUrl, body, "http://www.onvif.org/ver10/device/wsdl/GetDeviceInformation",
        )
        when {
            code == 401 -> AuthStatus.UNAUTHORIZED
            resp != null && resp.contains("NotAuthorized", ignoreCase = true) -> AuthStatus.UNAUTHORIZED
            code in 200..299 && resp != null && resp.contains("Manufacturer", ignoreCase = true) -> AuthStatus.OK
            code == -1 -> AuthStatus.UNREACHABLE
            else -> AuthStatus.UNREACHABLE
        }
    }

    suspend fun getDeviceInformation(): OnvifDeviceInfo? = withContext(Dispatchers.IO) {
        val body = soap(
            "http://www.onvif.org/ver10/device/wsdl",
            "<tds:GetDeviceInformation/>",
            nsTds = true,
        )
        val resp = post(deviceUrl, body, "http://www.onvif.org/ver10/device/wsdl/GetDeviceInformation")
            ?: return@withContext null
        OnvifDeviceInfo(
            manufacturer = tag(resp, "Manufacturer"),
            model = tag(resp, "Model"),
            firmware = tag(resp, "FirmwareVersion"),
        )
    }

    /** Resolve RTSP URIs for all media profiles. */
    suspend fun getStreamProfiles(): List<StreamProfile> = withContext(Dispatchers.IO) {
        val mediaUrl = resolveMediaEndpoint()
        val profilesResp = post(
            mediaUrl,
            soap("http://www.onvif.org/ver10/media/wsdl", "<trt:GetProfiles/>", nsTrt = true),
            "http://www.onvif.org/ver10/media/wsdl/GetProfiles",
        ) ?: return@withContext emptyList()

        val tokens = Regex("token=\"([^\"]+)\"").findAll(profilesResp).map { it.groupValues[1] }.toList()
        val result = mutableListOf<StreamProfile>()
        tokens.forEachIndexed { index, token ->
            val uriResp = post(
                mediaUrl,
                getStreamUriBody(token),
                "http://www.onvif.org/ver10/media/wsdl/GetStreamUri",
            )
            val rtsp = uriResp?.let { tag(it, "Uri") } ?: return@forEachIndexed
            result += StreamProfile(
                token = token,
                name = "Profile ${index + 1}",
                rtspUri = injectCredentials(rtsp),
                isMain = index == 0,
            )
        }
        result
    }

    /** Find the Media XAddr via GetCapabilities; fall back to common paths. */
    private fun resolveMediaEndpoint(): String {
        val caps = post(
            deviceUrl,
            soap(
                "http://www.onvif.org/ver10/device/wsdl",
                "<tds:GetCapabilities><tds:Category>Media</tds:Category></tds:GetCapabilities>",
                nsTds = true,
            ),
            "http://www.onvif.org/ver10/device/wsdl/GetCapabilities",
        )
        val xaddr = caps?.let {
            Regex("<[^>]*XAddr>(http[^<]+)</[^>]*XAddr>").find(it)?.groupValues?.get(1)
        }
        return xaddr ?: "http://$host:$port/onvif/media_service"
    }

    private fun getStreamUriBody(profileToken: String): String {
        val inner = """
            <trt:GetStreamUri>
              <trt:StreamSetup>
                <tt:Stream xmlns:tt="http://www.onvif.org/ver10/schema">RTP-Unicast</tt:Stream>
                <tt:Transport xmlns:tt="http://www.onvif.org/ver10/schema">
                  <tt:Protocol>RTSP</tt:Protocol>
                </tt:Transport>
              </trt:StreamSetup>
              <trt:ProfileToken>$profileToken</trt:ProfileToken>
            </trt:GetStreamUri>
        """.trimIndent()
        return soap("http://www.onvif.org/ver10/media/wsdl", inner, nsTrt = true)
    }

    // ---- SOAP plumbing ----

    private fun soap(
        bodyNs: String,
        innerBody: String,
        nsTds: Boolean = false,
        nsTrt: Boolean = false,
    ): String {
        val extraNs = buildString {
            if (nsTds) append(" xmlns:tds=\"$bodyNs\"")
            if (nsTrt) append(" xmlns:trt=\"$bodyNs\"")
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"$extraNs>
              <s:Header>${securityHeader()}</s:Header>
              <s:Body>$innerBody</s:Body>
            </s:Envelope>
        """.trimIndent()
    }

    private fun securityHeader(): String {
        val nonceBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val nonceB64 = Base64.encodeToString(nonceBytes, Base64.NO_WRAP)
        val created = utcNow()
        val digestInput = nonceBytes + created.toByteArray(Charsets.UTF_8) +
            pass.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-1").digest(digestInput)
        val digestB64 = Base64.encodeToString(digest, Base64.NO_WRAP)
        return """
            <wsse:Security s:mustUnderstand="1"
              xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
              xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
              <wsse:UsernameToken>
                <wsse:Username>$user</wsse:Username>
                <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">$digestB64</wsse:Password>
                <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">$nonceB64</wsse:Nonce>
                <wsu:Created>$created</wsu:Created>
              </wsse:UsernameToken>
            </wsse:Security>
        """.trimIndent()
    }

    /** Like post(), but returns the HTTP status code alongside the body (-1 on connect failure). */
    private fun postRaw(urlStr: String, body: String, soapAction: String): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/soap+xml; charset=utf-8; action=\"$soapAction\"",
                )
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            code to stream?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            -1 to null
        } finally {
            conn?.disconnect()
        }
    }

    private fun post(urlStr: String, body: String, soapAction: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/soap+xml; charset=utf-8; action=\"$soapAction\"",
                )
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun injectCredentials(rtsp: String): String {
        if (user.isEmpty()) return rtsp
        return try {
            val u = URI(rtsp)
            val creds = "${enc(user)}:${enc(pass)}@"
            val portPart = if (u.port > 0) ":${u.port}" else ""
            val query = if (u.query != null) "?${u.query}" else ""
            "rtsp://$creds${u.host}$portPart${u.path}$query"
        } catch (_: Exception) {
            rtsp
        }
    }

    private fun enc(s: String) = s
        .replace("%", "%25").replace(":", "%3A").replace("@", "%40").replace("/", "%2F")

    private fun tag(xml: String, name: String): String? =
        Regex("<[^>]*$name>(.*?)</[^>]*$name>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    private fun utcNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}

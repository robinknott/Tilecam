package no.tilecam.app.discovery

import no.tilecam.app.data.model.Camera
import no.tilecam.app.data.model.RecordingSegment
import no.tilecam.app.util.HttpDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Finds the actual recorded time-spans on a camera, so the UI can show a
 * timeline instead of making the user guess. Hikvision/Annke use the ISAPI
 * ContentMgmt search (HTTP Digest). Other vendors return null (unsupported);
 * ONVIF Profile G search can be added here later as a universal fallback.
 */
object RecordingSearch {

    /** @return list of segments, or null if this vendor has no timeline support. */
    suspend fun search(
        camera: Camera,
        user: String,
        pass: String,
        dayStartMillis: Long,
        dayEndMillis: Long,
        channel: Int = 1,
    ): List<RecordingSegment>? {
        return when (camera.vendorId) {
            "hikvision", "annke" -> hikvision(
                camera.host, camera.onvifPort, user, pass, channel, dayStartMillis, dayEndMillis,
            )
            else -> null
        }
    }

    private suspend fun hikvision(
        host: String,
        httpPort: Int,
        user: String,
        pass: String,
        channel: Int,
        dayStart: Long,
        dayEnd: Long,
    ): List<RecordingSegment> {
        val url = "http://$host:$httpPort/ISAPI/ContentMgmt/search"
        val trackId = channel * 100 + 1 // cam 1 main = 101
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <CMSearchDescription>
              <searchID>${UUID.randomUUID()}</searchID>
              <trackIDList><trackID>$trackId</trackID></trackIDList>
              <timeSpanList>
                <timeSpan>
                  <startTime>${iso(dayStart)}</startTime>
                  <endTime>${iso(dayEnd)}</endTime>
                </timeSpan>
              </timeSpanList>
              <maxResults>240</maxResults>
              <searchResultPostion>0</searchResultPostion>
              <metadataList>
                <metadataDescriptor>//recordType.meta.std-cgi.com/CMR</metadataDescriptor>
              </metadataList>
            </CMSearchDescription>
        """.trimIndent()

        val resp = HttpDigest.post(url, user, pass, body)
        val xml = resp.body ?: return emptyList()
        if (resp.code !in 200..299) return emptyList()

        return Regex("<timeSpan>(.*?)</timeSpan>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .mapNotNull { m ->
                val span = m.groupValues[1]
                val start = tag(span, "startTime")?.let(::parseIso)
                val end = tag(span, "endTime")?.let(::parseIso)
                if (start != null && end != null && end > start)
                    RecordingSegment(start, end) else null
            }
            .sortedBy { it.startMillis }
            .toList()
    }

    private fun tag(xml: String, name: String): String? =
        Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim()

    private fun iso(millis: Long): String {
        // Device local time with a literal 'Z' suffix (Hikvision convention).
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        f.timeZone = TimeZone.getDefault()
        return f.format(Date(millis))
    }

    /** Parse the camera's timestamps as device-local time so playback round-trips consistently. */
    private fun parseIso(s: String): Long? {
        val cleaned = s.trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
        )
        for (p in patterns) {
            try {
                val f = SimpleDateFormat(p, Locale.US)
                if (!p.contains("XXX")) f.timeZone = TimeZone.getDefault()
                return f.parse(cleaned)?.time
            } catch (_: Exception) {
                // try next
            }
        }
        return null
    }
}

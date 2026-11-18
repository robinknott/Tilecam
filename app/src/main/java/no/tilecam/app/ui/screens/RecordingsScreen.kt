package no.tilecam.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import no.tilecam.app.data.model.Camera
import no.tilecam.app.data.model.RecordingSegment
import no.tilecam.app.ui.RecordingSearchState
import no.tilecam.app.ui.components.TvTextField
import no.tilecam.app.ui.theme.Accent
import no.tilecam.app.ui.theme.Ink700
import no.tilecam.app.ui.theme.Ink800
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pick a day to see the camera's actual recorded segments on a timeline, or
 * enter an exact time window manually. Plays footage straight from the SD card
 * (or NVR) over RTSP.
 */
@Composable
fun RecordingsScreen(
    camera: Camera,
    supported: Boolean,
    timelineSupported: Boolean,
    searchState: RecordingSearchState,
    onSearchDay: (dayStart: Long, dayEnd: Long) -> Unit,
    onPlay: (startMillis: Long, endMillis: Long) -> Unit,
    onBack: () -> Unit,
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.US) }

    var date by remember { mutableStateOf(dateFmt.format(Calendar.getInstance().time)) }
    var time by remember {
        val c = Calendar.getInstance().apply { add(Calendar.MINUTE, -10) }
        mutableStateOf(timeFmt.format(c.time))
    }
    var durationMin by remember { mutableStateOf("10") }
    var error by remember { mutableStateOf<String?>(null) }

    fun dayBounds(): Pair<Long, Long>? {
        val start = parseLocal(date, "00:00") ?: return null
        return start to (start + 24 * 60 * 60_000L)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Opptak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "${camera.name}  •  ${camera.host}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onBack) { Text("Back") }
        }

        if (!supported) {
            Spacer(Modifier.padding(10.dp))
            Text(
                "Avspilling av opptak er ikke kjent for denne modellen ennå. " +
                    "Hikvision, Dahua, Amcrest, Lorex og Annke er støttet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }

        Spacer(Modifier.padding(8.dp))

        // Day picker + find button (timeline) ----------------------------------
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvTextField("Dato (yyyy-MM-dd)", date, { date = it }, Modifier.widthIn(max = 240.dp))
            if (timelineSupported) {
                Button(
                    onClick = { dayBounds()?.let { onSearchDay(it.first, it.second) } },
                    colors = ButtonDefaults.colors(containerColor = Accent, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) { Text(if (searchState.loading) "Søker…" else "Finn opptak", fontWeight = FontWeight.SemiBold) }
            }
        }

        if (timelineSupported && searchState.searched) {
            Spacer(Modifier.padding(6.dp))
            searchState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (searchState.segments.isNotEmpty()) {
                Timeline(searchState)
                Spacer(Modifier.padding(4.dp))
            } else if (searchState.error == null) {
                Text(
                    "Ingen opptak funnet for denne datoen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (!timelineSupported) {
            Spacer(Modifier.padding(2.dp))
            Text(
                "Automatisk tidslinje finnes foreløpig bare for Hikvision/Annke. " +
                    "Bruk manuelt valg under.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.padding(6.dp))

        // Manual window --------------------------------------------------------
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvTextField("Start (HH:mm)", time, { time = it }, Modifier.widthIn(max = 160.dp))
            TvTextField("Varighet (min)", durationMin, { durationMin = it }, Modifier.widthIn(max = 160.dp))
            Button(onClick = {
                val start = parseLocal(date, time)
                val mins = durationMin.toIntOrNull()
                if (start == null || mins == null || mins <= 0) {
                    error = "Sjekk dato, tid (HH:mm) og varighet."
                } else {
                    error = null
                    onPlay(start, start + mins * 60_000L)
                }
            }) { Text("Spill av valgt tid") }
        }
        error?.let {
            Spacer(Modifier.padding(2.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        // Segment list ---------------------------------------------------------
        if (searchState.segments.isNotEmpty()) {
            Spacer(Modifier.padding(8.dp))
            Text(
                "${searchState.segments.size} segmenter",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.padding(4.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(searchState.segments, key = { it.startMillis }) { seg ->
                    SegmentRow(seg) { onPlay(seg.startMillis, seg.endMillis) }
                }
            }
        }
    }
}

@Composable
private fun Timeline(state: RecordingSearchState) {
    val span = (state.dayEndMillis - state.dayStartMillis).coerceAtLeast(1)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(top = 6.dp),
    ) {
        // track
        drawRect(color = Ink700, size = size)
        // hour ticks
        for (h in 0..24) {
            val x = size.width * h / 24f
            drawRect(
                color = Ink800,
                topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(1f, size.height),
            )
        }
        // recorded segments
        state.segments.forEach { seg ->
            val startFrac = ((seg.startMillis - state.dayStartMillis).toFloat() / span).coerceIn(0f, 1f)
            val endFrac = ((seg.endMillis - state.dayStartMillis).toFloat() / span).coerceIn(0f, 1f)
            val x = size.width * startFrac
            val w = (size.width * (endFrac - startFrac)).coerceAtLeast(2f)
            drawRect(
                color = Accent,
                topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height),
            )
        }
    }
}

@Composable
private fun SegmentRow(seg: RecordingSegment, onClick: () -> Unit) {
    val tf = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val mins = (seg.durationMs / 60_000L)
    val secs = (seg.durationMs / 1000L) % 60
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Ink800, focusedContainerColor = Ink800),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Accent), shape = RoundedCornerShape(10.dp)),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${tf.format(java.util.Date(seg.startMillis))}  –  ${tf.format(java.util.Date(seg.endMillis))}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${mins}m ${secs}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(6.dp))
            Text("Spill av ›", color = Accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun parseLocal(date: String, time: String): Long? {
    val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
    return runCatching { f.parse("${date.trim()} ${time.trim()}")?.time }.getOrNull()
}

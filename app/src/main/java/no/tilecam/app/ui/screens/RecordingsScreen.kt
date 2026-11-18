package no.tilecam.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import no.tilecam.app.data.model.Camera
import no.tilecam.app.ui.components.TvTextField
import no.tilecam.app.ui.theme.Accent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pick a date/time window and play back footage the camera recorded to its own
 * SD card (or an NVR). Builds a time-ranged RTSP URL via the vendor template.
 */
@Composable
fun RecordingsScreen(
    camera: Camera,
    supported: Boolean,
    onPlay: (startMillis: Long, endMillis: Long) -> Unit,
    onBack: () -> Unit,
) {
    val now = remember { Calendar.getInstance() }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.US) }

    var date by remember { mutableStateOf(dateFmt.format(now.time)) }
    var time by remember {
        val c = Calendar.getInstance().apply { add(Calendar.MINUTE, -10) }
        mutableStateOf(timeFmt.format(c.time))
    }
    var durationMin by remember { mutableStateOf("10") }
    var error by remember { mutableStateOf<String?>(null) }

    fun playManual() {
        val start = parseLocal(date, time)
        val mins = durationMin.toIntOrNull()
        if (start == null || mins == null || mins <= 0) {
            error = "Sjekk dato (yyyy-MM-dd), tid (HH:mm) og varighet."
            return
        }
        error = null
        onPlay(start, start + mins * 60_000L)
    }

    fun playLast(minutes: Int) {
        val end = System.currentTimeMillis()
        onPlay(end - minutes * 60_000L, end)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
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

        Spacer(Modifier.padding(10.dp))

        if (!supported) {
            Text(
                "Tidsbasert avspilling er ikke kjent for denne modellen ennå. " +
                    "Hvis kameraet støtter ONVIF Profile G, kan det legges til senere. " +
                    "Hikvision, Dahua, Amcrest, Lorex og Annke er støttet nå.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }

        Text("Hurtigvalg", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.padding(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { playLast(10) }) { Text("Siste 10 min") }
            Button(onClick = { playLast(60) }) { Text("Siste time") }
            Button(onClick = { playLast(180) }) { Text("Siste 3 timer") }
        }

        Spacer(Modifier.padding(12.dp))
        Text("Velg tidspunkt", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.padding(4.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 560.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvTextField("Dato (yyyy-MM-dd)", date, { date = it }, Modifier.weight(1.4f))
                TvTextField("Start (HH:mm)", time, { time = it }, Modifier.weight(1f))
                TvTextField("Varighet (min)", durationMin, { durationMin = it }, Modifier.weight(1f))
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = { playManual() },
                colors = ButtonDefaults.colors(containerColor = Accent, contentColor = MaterialTheme.colorScheme.onPrimary),
            ) { Text("Spill av", fontWeight = FontWeight.SemiBold) }
        }

        Spacer(Modifier.padding(10.dp))
        Text(
            "Avspillingen streames fra kameraets opptak via RTSP. Pause og spoling " +
                "styres med fjernkontrollen. Sørg for at kameraets klokke er riktig stilt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 720.dp),
        )
    }
}

private fun parseLocal(date: String, time: String): Long? {
    val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
    return runCatching { f.parse("${date.trim()} ${time.trim()}")?.time }.getOrNull()
}

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import no.tilecam.app.data.model.Camera
import no.tilecam.app.ui.components.TvTextField
import no.tilecam.app.ui.theme.Accent

@Composable
fun SettingsScreen(
    cameras: List<Camera>,
    onAddManual: (name: String, rtspUrl: String) -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("rtsp://") }

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.padding(10.dp))

        Text(
            "Add a camera manually",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.padding(6.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 680.dp),
        ) {
            TvTextField("Name", name, { name = it }, Modifier.fillMaxWidth())
            TvTextField("RTSP URL (rtsp://user:pass@host:554/path)", url, { url = it }, Modifier.fillMaxWidth())
            Button(
                onClick = { if (url.length > 7) { onAddManual(name, url); name = ""; url = "rtsp://" } },
                colors = ButtonDefaults.colors(containerColor = Accent, contentColor = MaterialTheme.colorScheme.onPrimary),
            ) { Text("Add camera", fontWeight = FontWeight.SemiBold) }
        }

        Spacer(Modifier.padding(14.dp))
        Text(
            "Saved cameras",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.padding(6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(cameras, key = { it.id }) { cam ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(cam.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${cam.host}  •  ${cam.source.name.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onRemove(cam.id) },
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                    ) { Text("Remove") }
                }
            }
        }
    }
}

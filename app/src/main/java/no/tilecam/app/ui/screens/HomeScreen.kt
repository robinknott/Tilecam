package no.tilecam.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import no.tilecam.app.data.model.Camera
import no.tilecam.app.ui.components.CameraTile
import no.tilecam.app.ui.theme.Accent

@Composable
fun HomeScreen(
    cameras: List<Camera>,
    urlFor: (Camera) -> String?,
    bulkStatus: String? = null,
    onScan: () -> Unit,
    onSettings: () -> Unit,
    onOpenCamera: (Camera) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Tilecam",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    bulkStatus ?: if (cameras.isEmpty()) "Your cameras, one wall."
                    else "${cameras.size} camera${if (cameras.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (bulkStatus != null) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onScan,
                colors = ButtonDefaults.colors(containerColor = Accent, contentColor = MaterialTheme.colorScheme.onPrimary),
            ) { Text("Scan network", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onSettings) { Text("Settings") }
        }

        Spacer(Modifier.padding(10.dp))

        if (cameras.isEmpty()) {
            EmptyState(onScan)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(cameras, key = { it.id }) { camera ->
                    CameraTile(
                        camera = camera,
                        url = urlFor(camera),
                        onClick = { onOpenCamera(camera) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onScan: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No cameras yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.padding(6.dp))
            Text(
                "Scan your network to find cameras automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(12.dp))
            Button(
                onClick = onScan,
                colors = ButtonDefaults.colors(containerColor = Accent, contentColor = MaterialTheme.colorScheme.onPrimary),
            ) { Text("Scan network", fontWeight = FontWeight.SemiBold) }
        }
    }
}

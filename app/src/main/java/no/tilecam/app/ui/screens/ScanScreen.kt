package no.tilecam.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
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
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.ui.ScanMode
import no.tilecam.app.ui.ScanState
import no.tilecam.app.ui.components.TvTextField
import no.tilecam.app.ui.theme.Accent
import no.tilecam.app.ui.theme.Ink800

@Composable
fun ScanScreen(
    state: ScanState,
    vendorName: (String) -> String,
    onSetMode: (ScanMode) -> Unit,
    onSetRange: (String) -> Unit,
    onStartLan: () -> Unit,
    onStartWan: () -> Unit,
    onStop: () -> Unit,
    onPick: (DiscoveredDevice) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Find cameras",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.padding(8.dp))

        // Mode selector
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeChip("Local network", state.mode == ScanMode.LAN) { onSetMode(ScanMode.LAN) }
            ModeChip("IP range (WAN)", state.mode == ScanMode.WAN) { onSetMode(ScanMode.WAN) }
        }

        Spacer(Modifier.padding(8.dp))

        if (state.mode == ScanMode.WAN) {
            Row(verticalAlignment = Alignment.Bottom) {
                TvTextField(
                    label = "IP, CIDR or range (e.g. 203.0.113.0/28)",
                    value = state.wanRange,
                    onValueChange = onSetRange,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                ScanActionButton(state.running, onStartWan, onStop)
            }
        } else {
            ScanActionButton(state.running, onStartLan, onStop)
        }

        state.error?.let {
            Spacer(Modifier.padding(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.padding(6.dp))

        // Progress
        val p = state.progress
        if (state.running || p.total > 0) {
            Text(
                buildString {
                    append(p.phase)
                    if (p.total > 0) append("  ${p.done}/${p.total}")
                    append("  •  ${state.devices.size} found")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.padding(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.devices, key = { it.host }) { device ->
                DeviceRow(device, vendorName(device.vendorId), onPick)
            }
        }
    }
}

@Composable
private fun ScanActionButton(running: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    if (running) {
        Button(onClick = onStop) { Text("Stop") }
    } else {
        Button(
            onClick = onStart,
            colors = ButtonDefaults.colors(containerColor = Accent, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) { Text("Start scan", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Accent else Ink800,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = if (selected) Accent else Ink800,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Accent), shape = RoundedCornerShape(24.dp)),
        ),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
    }
}

@Composable
private fun DeviceRow(
    device: DiscoveredDevice,
    vendorName: String,
    onPick: (DiscoveredDevice) -> Unit,
) {
    Surface(
        onClick = { onPick(device) },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Ink800, focusedContainerColor = Ink800),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Accent), shape = RoundedCornerShape(12.dp)),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${device.host}  •  $vendorName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append("via ${device.via.name.lowercase()}")
                        if (device.openPorts.isNotEmpty())
                            append("  •  ports ${device.openPorts.joinToString(",")}")
                        if (device.onvifXAddr != null) append("  •  ONVIF")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.padding(start = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sign in ›", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

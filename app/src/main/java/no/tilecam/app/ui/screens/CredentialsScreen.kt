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
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.ui.ConnectState
import no.tilecam.app.ui.components.TvTextField
import no.tilecam.app.ui.theme.Accent

@Composable
fun CredentialsScreen(
    device: DiscoveredDevice,
    vendorName: String,
    defaultUsername: String,
    defaultPassword: String,
    connectState: ConnectState,
    onConnect: (user: String, pass: String, name: String) -> Unit,
    onBack: () -> Unit,
) {
    var username by remember(device.host) { mutableStateOf(defaultUsername) }
    var password by remember(device.host) { mutableStateOf(defaultPassword) }
    var name by remember(device.host) { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 36.dp),
    ) {
        Text(
            "Sign in to camera",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "${device.host}  •  $vendorName",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(4.dp))
        Text(
            "Default $vendorName username filled in — change it if you set your own.",
            style = MaterialTheme.typography.bodySmall,
            color = Accent,
        )

        Spacer(Modifier.padding(14.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 560.dp),
        ) {
            TvTextField("Camera name (optional)", name, { name = it }, Modifier.fillMaxWidth())
            TvTextField("Username", username, { username = it }, Modifier.fillMaxWidth())
            TvTextField("Password", password, { password = it }, Modifier.fillMaxWidth(), isPassword = true)

            connectState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onConnect(username, password, name) },
                    enabled = !connectState.connecting,
                    colors = ButtonDefaults.colors(containerColor = Accent, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Text(if (connectState.connecting) "Connecting…" else "Connect", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = onBack, enabled = !connectState.connecting) { Text("Back") }
            }
        }
    }
}

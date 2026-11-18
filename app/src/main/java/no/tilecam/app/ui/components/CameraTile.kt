package no.tilecam.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import no.tilecam.app.data.model.Camera
import no.tilecam.app.player.CameraVideo
import no.tilecam.app.player.StreamState
import no.tilecam.app.ui.theme.Accent
import no.tilecam.app.ui.theme.Ink800

@Composable
fun CameraTile(
    camera: Camera,
    url: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var state by remember(url) { mutableStateOf(StreamState.LOADING) }

    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(16f / 9f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Ink800,
            focusedContainerColor = Ink800,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Accent), shape = RoundedCornerShape(14.dp)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
            if (url != null) {
                CameraVideo(
                    url = url,
                    muted = true,
                    modifier = Modifier.fillMaxSize(),
                    onState = { state = it },
                )
            }
            if (state != StreamState.PLAYING) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            url == null -> "No stream"
                            state == StreamState.ERROR -> "Reconnecting…"
                            else -> "Connecting…"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Gradient + label along the bottom.
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to androidx.compose.ui.graphics.Color.Transparent,
                            1f to androidx.compose.ui.graphics.Color(0xCC000000),
                        )
                    )
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        text = camera.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = camera.host,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

package no.tilecam.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import no.tilecam.app.player.CameraVideo
import no.tilecam.app.player.StreamState

/**
 * Plays a recorded time-range stream with transport controls (pause / seek)
 * driven by the remote.
 */
@Composable
fun PlaybackScreen(
    title: String,
    url: String,
) {
    var state by remember(url) { mutableStateOf(StreamState.LOADING) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraVideo(
            url = url,
            muted = false,
            showControls = true,
            modifier = Modifier.fillMaxSize(),
            onState = { state = it },
        )
        if (state != StreamState.PLAYING) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    when (state) {
                        StreamState.ERROR ->
                            "Fant ikke opptak for dette tidsrommet, eller kameraet avviste innlogging."
                        else -> "Laster opptak…"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
        Column(Modifier.align(Alignment.TopStart).padding(28.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "Press Back to return",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xCCFFFFFF),
            )
        }
    }
}

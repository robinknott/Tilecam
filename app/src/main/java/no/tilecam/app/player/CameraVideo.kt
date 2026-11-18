package no.tilecam.app.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

enum class StreamState { LOADING, PLAYING, ERROR }

/**
 * Renders a single live camera stream. Handles RTSP (forced over TCP, which the
 * vast majority of cameras need) as well as plain HTTP/HLS fallbacks, with
 * automatic retry on transient drops.
 */
@Composable
fun CameraVideo(
    url: String,
    modifier: Modifier = Modifier,
    muted: Boolean = true,
    resizeFill: Boolean = false,
    onState: (StreamState) -> Unit = {},
) {
    val context = LocalContext.current
    var retryCount by remember(url) { mutableStateOf(0) }

    val player = remember(url, retryCount) {
        ExoPlayer.Builder(context).build().apply {
            volume = if (muted) 0f else 1f
            repeatMode = Player.REPEAT_MODE_OFF
            setMediaSource(buildSource(context, url))
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> onState(StreamState.PLAYING)
                    Player.STATE_BUFFERING -> onState(StreamState.LOADING)
                    else -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onState(StreamState.ERROR)
                // Exponential-ish backoff, capped, by bumping the remember key.
                if (retryCount < 8) {
                    player.postDelayed({ retryCount++ }, retryDelay(retryCount))
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    resizeMode = if (resizeFill)
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.matchParentSize(),
        )
    }
}

private fun buildSource(
    context: android.content.Context,
    url: String,
): MediaSource {
    val item = MediaItem.fromUri(url)
    return if (url.startsWith("rtsp", ignoreCase = true)) {
        RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setTimeoutMs(8000)
            .createMediaSource(item)
    } else {
        ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
            .createMediaSource(item)
    }
}

private fun retryDelay(attempt: Int): Long =
    (1000L * (attempt + 1)).coerceAtMost(6000L)

private fun ExoPlayer.postDelayed(action: () -> Unit, delayMs: Long) {
    applicationLooper.let {
        android.os.Handler(it).postDelayed(action, delayMs)
    }
}

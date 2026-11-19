package no.tilecam.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import no.tilecam.app.ui.screens.CredentialsScreen
import no.tilecam.app.ui.screens.HomeScreen
import no.tilecam.app.ui.screens.PlaybackScreen
import no.tilecam.app.ui.screens.PlayerScreen
import no.tilecam.app.ui.screens.RecordingsScreen
import no.tilecam.app.ui.screens.ScanScreen
import no.tilecam.app.ui.screens.SettingsScreen

@Composable
fun TilecamRoot(vm: MainViewModel) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val cameras by vm.cameras.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    val connect by vm.connect.collectAsStateWithLifecycle()
    val recSearch by vm.recSearch.collectAsStateWithLifecycle()
    val bulkStatus by vm.bulkStatus.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    ) {
        when (val s = screen) {
            is Screen.Home -> HomeScreen(
                cameras = cameras,
                urlFor = { vm.playableUrl(it) },
                bulkStatus = bulkStatus,
                onScan = { vm.navigate(Screen.Scan) },
                onSettings = { vm.navigate(Screen.Settings) },
                onOpenCamera = { vm.navigate(Screen.Player(it.id)) },
            )

            is Screen.Scan -> {
                BackHandler { vm.back() }
                ScanScreen(
                    state = scan,
                    vendorName = { vm.vendorName(it) },
                    onSetMode = { vm.setScanMode(it) },
                    onSetRange = { vm.setWanRange(it) },
                    onStartLan = { vm.startLanScan() },
                    onStartWan = { vm.startWanScan() },
                    onStop = { vm.stopScan() },
                    onPick = { vm.navigate(Screen.Credentials(it)) },
                    onBack = { vm.back() },
                )
            }

            is Screen.Credentials -> {
                BackHandler { vm.back() }
                CredentialsScreen(
                    device = s.device,
                    vendorName = vm.vendorName(s.device.vendorId),
                    defaultUsername = vm.defaultUsernameFor(s.device),
                    defaultPassword = vm.defaultPasswordFor(s.device),
                    connectState = connect,
                    onConnect = { u, p, n -> vm.connectDevice(s.device, u, p, n.ifBlank { null }) },
                    onBack = { vm.back() },
                )
            }

            is Screen.Player -> {
                BackHandler { vm.back() }
                val camera = vm.cameraById(s.cameraId)
                if (camera != null) {
                    PlayerScreen(
                        camera = camera,
                        url = vm.playableUrl(camera),
                        canRecord = vm.supportsPlayback(camera),
                        onOpenRecordings = { vm.openRecordings(camera.id) },
                    )
                } else {
                    vm.back()
                }
            }

            is Screen.Recordings -> {
                BackHandler { vm.back() }
                val camera = vm.cameraById(s.cameraId)
                if (camera != null) {
                    RecordingsScreen(
                        camera = camera,
                        supported = vm.supportsPlayback(camera),
                        timelineSupported = vm.supportsTimeline(camera),
                        searchState = recSearch,
                        onSearchDay = { start, end -> vm.searchRecordings(camera, start, end) },
                        onPlay = { start, end -> vm.openPlayback(camera, start, end) },
                        onBack = { vm.back() },
                    )
                } else {
                    vm.back()
                }
            }

            is Screen.Playback -> {
                BackHandler { vm.back() }
                PlaybackScreen(title = s.title, url = s.url)
            }

            is Screen.Settings -> {
                BackHandler { vm.back() }
                SettingsScreen(
                    cameras = cameras,
                    onAddManual = { name, url -> vm.addManualCamera(name, url) },
                    onRemove = { vm.removeCamera(it) },
                    onBack = { vm.back() },
                )
            }
        }
    }
}

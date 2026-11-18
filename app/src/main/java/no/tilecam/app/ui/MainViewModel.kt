package no.tilecam.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.tilecam.app.data.AppContainer
import no.tilecam.app.data.ConnectResult
import no.tilecam.app.data.Credentials
import no.tilecam.app.data.model.Camera
import no.tilecam.app.data.model.DiscoveredDevice
import no.tilecam.app.data.model.RecordingSegment
import no.tilecam.app.data.model.ScanConfig
import no.tilecam.app.discovery.RecordingSearch
import no.tilecam.app.data.vendor.Vendor
import no.tilecam.app.data.vendor.VendorDatabase
import no.tilecam.app.discovery.ScanProgress

sealed interface Screen {
    data object Home : Screen
    data object Scan : Screen
    data class Credentials(val device: DiscoveredDevice) : Screen
    data class Player(val cameraId: String) : Screen
    data class Recordings(val cameraId: String) : Screen
    data class Playback(val cameraId: String, val url: String, val title: String) : Screen
    data object Settings : Screen
}

data class ScanState(
    val running: Boolean = false,
    val progress: ScanProgress = ScanProgress(""),
    val devices: List<DiscoveredDevice> = emptyList(),
    val mode: ScanMode = ScanMode.LAN,
    val wanRange: String = "",
    val error: String? = null,
)

enum class ScanMode { LAN, WAN }

data class ConnectState(
    val connecting: Boolean = false,
    val error: String? = null,
)

data class RecordingSearchState(
    val loading: Boolean = false,
    val segments: List<RecordingSegment> = emptyList(),
    val dayStartMillis: Long = 0,
    val dayEndMillis: Long = 0,
    val searched: Boolean = false,
    val error: String? = null,
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    val cameras: StateFlow<List<Camera>> = container.cameraStore.cameras
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _scan = MutableStateFlow(ScanState())
    val scan: StateFlow<ScanState> = _scan.asStateFlow()

    private val _connect = MutableStateFlow(ConnectState())
    val connect: StateFlow<ConnectState> = _connect.asStateFlow()

    private val _recSearch = MutableStateFlow(RecordingSearchState())
    val recSearch: StateFlow<RecordingSearchState> = _recSearch.asStateFlow()

    private var scanJob: Job? = null

    fun navigate(screen: Screen) {
        _screen.value = screen
    }

    fun back() {
        val current = _screen.value
        if (current is Screen.Scan) stopScan()
        _screen.value = when (current) {
            is Screen.Credentials -> Screen.Scan
            is Screen.Scan -> Screen.Home
            is Screen.Player -> Screen.Home
            is Screen.Recordings -> Screen.Player(current.cameraId)
            is Screen.Playback -> Screen.Recordings(current.cameraId)
            is Screen.Settings -> Screen.Home
            else -> Screen.Home
        }
    }

    // ---- scanning ----

    fun setScanMode(mode: ScanMode) {
        _scan.value = _scan.value.copy(mode = mode)
    }

    fun setWanRange(range: String) {
        _scan.value = _scan.value.copy(wanRange = range)
    }

    fun startLanScan() {
        scanJob?.cancel()
        _scan.value = _scan.value.copy(running = true, devices = emptyList(), error = null)
        scanJob = viewModelScope.launch {
            container.discoveryManager.scanLan(
                onProgress = { p -> _scan.value = _scan.value.copy(progress = p) },
                onFound = { d -> addDevice(d) },
            )
            _scan.value = _scan.value.copy(running = false)
        }
    }

    fun startWanScan() {
        val range = _scan.value.wanRange.trim()
        if (range.isEmpty()) {
            _scan.value = _scan.value.copy(error = "Enter an IP, CIDR (203.0.113.0/28) or range.")
            return
        }
        scanJob?.cancel()
        _scan.value = _scan.value.copy(running = true, devices = emptyList(), error = null)
        scanJob = viewModelScope.launch {
            container.discoveryManager.scanWan(
                config = ScanConfig(cidrOrRange = range),
                onProgress = { p -> _scan.value = _scan.value.copy(progress = p) },
                onFound = { d -> addDevice(d) },
            )
            _scan.value = _scan.value.copy(running = false)
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _scan.value = _scan.value.copy(running = false)
    }

    private fun addDevice(device: DiscoveredDevice) {
        val current = _scan.value.devices
        val idx = current.indexOfFirst { it.host == device.host }
        val next = if (idx >= 0) current.toMutableList().apply { this[idx] = device }
        else current + device
        _scan.value = _scan.value.copy(devices = next, progress = _scan.value.progress.copy(found = next.size))
    }

    // ---- connecting ----

    fun defaultUsernameFor(device: DiscoveredDevice): String =
        VendorDatabase.byId(device.vendorId).defaultUsername

    fun defaultPasswordFor(device: DiscoveredDevice): String =
        VendorDatabase.byId(device.vendorId).defaultPassword

    fun vendorName(vendorId: String): String = VendorDatabase.byId(vendorId).displayName

    fun connectDevice(device: DiscoveredDevice, username: String, password: String, name: String?) {
        _connect.value = ConnectState(connecting = true)
        viewModelScope.launch {
            when (val result = container.connectionResolver.connect(device, username, password, name)) {
                is ConnectResult.Success -> {
                    container.credentialStore.save(result.camera.id, Credentials(username, password))
                    container.cameraStore.upsert(cameras.value, result.camera)
                    _connect.value = ConnectState(connecting = false)
                    _screen.value = Screen.Home
                }
                is ConnectResult.Failure -> {
                    _connect.value = ConnectState(connecting = false, error = result.reason)
                }
            }
        }
    }

    fun addManualCamera(name: String, rtspUrl: String) {
        viewModelScope.launch {
            val cam = Camera(
                id = java.util.UUID.randomUUID().toString(),
                name = name.ifBlank { "Camera" },
                host = rtspUrl.substringAfter("@").substringBefore(":").substringBefore("/"),
                vendorId = Vendor.GENERIC_ONVIF,
                rtspPath = rtspUrl,
                source = no.tilecam.app.data.model.StreamSource.MANUAL,
            )
            container.cameraStore.upsert(cameras.value, cam)
            _screen.value = Screen.Home
        }
    }

    fun removeCamera(cameraId: String) {
        viewModelScope.launch {
            container.credentialStore.delete(cameraId)
            container.cameraStore.remove(cameras.value, cameraId)
        }
    }

    /** Playable RTSP URL (with credentials) for a saved camera. */
    fun playableUrl(camera: Camera): String? {
        val creds = container.credentialStore.get(camera.id)
        return container.connectionResolver.playableUrl(camera, creds)
    }

    fun cameraById(id: String): Camera? = cameras.value.firstOrNull { it.id == id }

    /** Whether this camera's vendor exposes time-ranged SD-card/NVR playback. */
    fun supportsPlayback(camera: Camera): Boolean =
        VendorDatabase.byId(camera.vendorId).supportsPlayback

    /** Build a playback RTSP URL for a time window, or null if unsupported. */
    fun buildPlayback(camera: Camera, startMillis: Long, endMillis: Long): String? {
        val creds = container.credentialStore.get(camera.id) ?: return null
        val vendor = VendorDatabase.byId(camera.vendorId)
        return VendorDatabase.buildPlaybackUrl(
            vendor, camera.host, creds.username, creds.password,
            startMillis, endMillis, camera.rtspPort,
        )
    }

    fun openPlayback(camera: Camera, startMillis: Long, endMillis: Long) {
        val url = buildPlayback(camera, startMillis, endMillis) ?: return
        val label = "${camera.name} — opptak"
        navigate(Screen.Playback(camera.id, url, label))
    }

    /** Whether this vendor supports a real recordings timeline (not just manual time entry). */
    fun supportsTimeline(camera: Camera): Boolean =
        camera.vendorId == "hikvision" || camera.vendorId == "annke"

    fun openRecordings(cameraId: String) {
        _recSearch.value = RecordingSearchState()
        navigate(Screen.Recordings(cameraId))
    }

    /** Query the camera for the recorded segments on a given day. */
    fun searchRecordings(camera: Camera, dayStartMillis: Long, dayEndMillis: Long) {
        val creds = container.credentialStore.get(camera.id)
        if (creds == null) {
            _recSearch.value = RecordingSearchState(error = "Mangler innlogging for dette kameraet.")
            return
        }
        _recSearch.value = RecordingSearchState(
            loading = true, dayStartMillis = dayStartMillis, dayEndMillis = dayEndMillis,
        )
        viewModelScope.launch {
            val segs = RecordingSearch.search(
                camera, creds.username, creds.password, dayStartMillis, dayEndMillis,
            )
            _recSearch.value = if (segs == null) {
                RecordingSearchState(
                    searched = true,
                    error = "Tidslinje støttes ikke for denne modellen ennå — bruk manuelt valg.",
                    dayStartMillis = dayStartMillis, dayEndMillis = dayEndMillis,
                )
            } else {
                RecordingSearchState(
                    segments = segs, searched = true,
                    dayStartMillis = dayStartMillis, dayEndMillis = dayEndMillis,
                )
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(container) as T
    }
}

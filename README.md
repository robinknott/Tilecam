# Tilecam

**Your cameras, one wall.** A native Android TV / Google TV app that finds the
IP cameras on your network and shows them as a live tile wall on the big screen,
controlled with the remote.

Built with Kotlin, Jetpack **Compose for TV**, and **AndroidX Media3
(ExoPlayer)** with RTSP over TCP.

---

## What it does

- **Automatic discovery** — finds cameras on the local network three ways at once:
  - **ONVIF WS-Discovery** (the universal camera standard — works across brands)
  - **mDNS / DNS-SD** (`_rtsp._tcp`, `_onvif._tcp`)
  - **Subnet port sweep** for the well-known camera ports
- **IP-range scan (WAN)** — point it at a single IP, a CIDR block
  (`203.0.113.0/28`) or a dash range (`203.0.113.10-40`) and it port-scans for
  cameras. **Only scan networks and cameras you own or are authorised to test.**
- **Works with essentially any model** — ONVIF-conformant cameras are handled
  generically by querying `GetStreamUri`. On top of that, a built-in
  **manufacturer database** adds direct RTSP URLs and the correct default
  **username** for the major brands (Hikvision, Dahua, Reolink, Axis, Amcrest,
  Hanwha/Wisenet, Uniview, Vivotek, Foscam, TP-Link Tapo/VIGI, Bosch, Panasonic,
  D-Link, TRENDnet, Lorex, Annke, Wyze, generic HiSilicon/XMeye, and more).
- **Smart sign-in** — when a camera is found, the vendor's factory-default
  username is pre-filled; you just confirm or type the password.
- **Live tile wall** — focusable tiles with D-pad navigation; click a tile for
  fullscreen. Streams auto-reconnect on drops.
- **Secure storage** — camera passwords are stored with Jetpack Security
  (AES-256, `EncryptedSharedPreferences`); they never leave the device.

> On default credentials: the values in the vendor database are the
> manufacturers' own **documented factory defaults**, kept purely to pre-fill
> the right username for *your own* cameras. Modern cameras force a password
> change at setup — the pre-fill is a convenience, not a way in.

---

## Get the app (downloadable APK)

You don't need Android Studio to get an installable APK — GitHub builds it for you:

1. Push this repo to GitHub (see below). The **Build Tilecam APK** workflow runs
   automatically.
2. Open the **Actions** tab → the latest run → **Artifacts** →
   download **`Tilecam-release`**. Unzip to get `Tilecam-release.apk`.
3. To publish a versioned download, push a tag:
   ```bash
   git tag v1.0.0 && git push origin v1.0.0
   ```
   The APK is then attached to a **GitHub Release** on the repo's Releases page.

### Install on an Android TV / Google TV

Sideload the APK (the app is not on Google Play):

- **Downloader app** (easiest): on the TV, install *Downloader* from the Play
  Store, enable unknown sources, and open the APK's URL (e.g. the Release asset
  link).
- **adb** (from a computer on the same network):
  ```bash
  adb connect <tv-ip>:5555
  adb install Tilecam-release.apk
  ```

---

## Build locally

Requires JDK 17 and the Android SDK (Android Studio Ladybug or newer).

```bash
# Android Studio generates the Gradle wrapper on first open; or from CLI:
gradle wrapper --gradle-version 8.11.1
./gradlew assembleRelease
# → app/build/outputs/apk/release/Tilecam-release.apk
```

Open the folder in Android Studio and press **Run** to deploy to an Android TV
emulator or a connected device.

---

## How it fits together

```
discovery/            ONVIF WS-Discovery, mDNS, LAN sweep, WAN range scan
  DiscoveryManager    orchestrates + merges results per host
onvif/OnvifClient     SOAP + WS-UsernameToken digest → real RTSP URIs
data/vendor/          manufacturer database: defaults, RTSP templates, detection
data/                 persistence (DataStore) + encrypted credentials
player/CameraVideo    Media3 ExoPlayer, RTSP forced over TCP, auto-retry
ui/                   Compose-for-TV screens: Home wall, Scan, Sign-in, Player, Settings
```

**Stream resolution order** for each camera: try ONVIF `GetStreamUri`
(exact URL, any brand) → fall back to the vendor's RTSP URL template → or a
manually entered `rtsp://` URL.

---

## Requirements

- Android TV / Google TV 7.0+ (minSdk 24; ONVIF discovery best on 8.0+)
- Cameras reachable from the TV's network (or a routed/VPN path for WAN)

---

## Roadmap ideas

- Multi-profile switching (main/sub) and per-tile layout sizes
- Motion/event overlays and ONVIF PTZ control
- Saved layouts and auto-cycle (kiosk) mode
- H.265/HEVC hardware-decode tuning per device

---

## Legal & responsible use

Tilecam is a viewer for cameras **you own or are authorised to access**. The
IP-range/port-scan feature must only be used on networks you own or have
explicit permission to test. You are responsible for complying with local law
and the terms of your camera vendors.

## License

MIT — see [LICENSE](LICENSE).

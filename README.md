# Native Android TV IPTV Player

A high-performance, native Android TV player built specifically for **Xiaomi TV Box** (Mi Box S / Xiaomi TV Box S, Android TV / Google TV 9–14+) and other Android TV devices. 

Designed strictly as a **player-only application**: the app ships with **no preloaded channels, playlists, or provider URLs**. Users connect to their own Xtream Codes provider using standard server credentials.

---

## Features

- **10-Foot UI with Dual TV Remote & Mouse/Air-Mouse Support**: Built with Jetpack Compose for TV (`androidx.tv:tv-material`), featuring dark themes, high-contrast focus rings, hover states, and full click/scroll support for both standard TV remotes (D-pad) and wireless mice/air-mice.
- **Xtream Codes API Integration**:
  - Connect via `{server}/player_api.php?username=U&password=P`.
  - Detailed account diagnostics: Account status, subscription expiry date, and active/maximum connection limits.
  - Multi-profile management: Switch between or delete multiple provider accounts with ease.
  - Encrypted credential storage backed by Android Keystore (`EncryptedSharedPreferences`).
- **Channel & Category Browsing + Category Manager**:
  - Category list on the left with pinned **★ Favorites** and **⟳ Recent** (last 20 watched channels) at the top.
  - **Manage Categories Screen**: Show or hide individual categories (with search and Show All / Hide All) in Settings, persisted across playlist refreshes.
  - **Modern Guide Polish**: Top header bar with active category title, playable channel count, debounced search bar, and live `HH:mm` clock; automatic section dividers for provider separator rows (`#### ... ####`); sequential `1..N` channel numbering; color-coded quality pills (`4K`, `RAW`, `FHD`, `HEVC`, `HD`); and async channel logos (`stream_icon`) loaded via Coil with RGB_565 downsampling.
  - Offline Room database caching with WAL mode and composite indexing: 50,000+ channels open and filter instantaneously.
  - Preserves exact channel scroll position and remote focus when returning from fullscreen playback.
- **Full-Featured TV Playback**:
  - AndroidX Media3 (ExoPlayer) with hardware-accelerated HLS (`.m3u8`) and MPEG-TS (`.ts`) decoding.
  - **In-Player Quick Channel Drawer**: Press **D-pad Right (`▶`)** or click **Channels** in the floating player bar to browse categories and channels in a translucent overlay while live video keeps playing uninterrupted.
  - **Auto-Reconnect & Stream Fallback**: Automatically reconnects on live stream `EOS`/resolution switches, recovers from live window drift, retries up to 3 times with backoff and `.m3u8` ↔ `.ts` fallback on network drops or stalls, and includes a manual **Retry Stream (OK)** button.
  - Immediate stream socket release on Back/Home/Power off so 1-connection IPTV accounts are never locked out when switching between TV Boxes.
  - Screen wake lock (`FLAG_KEEP_SCREEN_ON`) so the Android TV screensaver never interrupts live playback.
  - Quick channel zapping with D-pad Up/Down or remote Channel +/- keys (automatically skipping non-playable section dividers).
  - Direct numeric channel jump (0–9 key buffer with auto-commit).
  - "Last Channel" quick-switch shortcut.
  - Optional setting to resume the last watched channel immediately on app launch.
- **Robust Favorites System**:
  - Add/remove channels from favorites with a **long-press on OK/Select** in the channel list, or via the interactive star button in the video overlay.
  - Pinned "Favorites" category always accessible at the top of the category list.
  - Favorites are keyed per account by `stream_id` in a dedicated Room table, ensuring they survive playlist refreshes and app restarts.

---

## Architecture Overview

```
com.antigravity.tvplayer/
├── MainActivity.kt               # Single activity hosting TV NavHost & key interceptors
├── TvPlayerApplication.kt        # Application class with @HiltAndroidApp
├── data/
│   ├── api/                      # Retrofit 2 + OkHttp + kotlinx.serialization
│   │   ├── XtreamApiService.kt   # Xtream Codes player_api.php endpoints
│   │   └── XtreamUrlBuilder.kt   # URL normalization & stream URI generation
│   ├── local/                    # Room Database (Accounts, Categories, Channels, Favorites)
│   │   ├── dao/                  # AccountDao, CategoryDao, ChannelDao, FavoriteDao
│   │   ├── entity/               # Indexed SQLite tables with cascade rules
│   │   └── TvDatabase.kt
│   ├── repository/               # AccountRepository, ChannelRepository, FavoritesRepository
│   ├── security/                 # SecureCredentialStore (EncryptedSharedPreferences + Keystore)
│   └── settings/                 # SettingsDataStore (Preferences DataStore)
├── domain/                       # Pure Kotlin domain entities & repository interfaces
├── player/                       # AndroidX Media3 ExoPlayer wrapper & load control
│   └── PlayerManager.kt          # Hardware decoding, HTTP data source, and auto-fallback
└── ui/
    ├── navigation/               # Jetpack Navigation Compose (Login, LiveTv, Player, Settings)
    ├── screens/
    │   ├── live/                 # Category/Channel lists, D-pad selection, Favorites
    │   ├── login/                # Server URL, Username, Password, Profile switcher
    │   ├── player/               # Fullscreen SurfaceView + translucent 4s auto-hide overlay
    │   └── settings/             # Output format, Auto-resume, Custom User-Agent, Sync
    └── theme/                    # 10-foot TV color scheme, typography & focus rings
```

---

## Remote Control Guide

| Remote Key | In Channel List | In Fullscreen Player |
|---|---|---|
| **D-pad Up / Down** | Navigate categories & channels | Switch to previous / next channel |
| **D-pad Left** | Move from Channels to Categories | Quick-switch to last channel |
| **D-pad Right** | Move from Categories to Channels | Open In-Player Quick Channel Drawer |
| **OK / Center Click** | Open channel & play | Toggle player overlay |
| **OK Long-Press** | **Toggle Favorite (★)** | — |
| **Number Keys (0–9)** | — | Direct channel jump (e.g. `1`, `4` → Channel 14) |
| **Channel +/-** | Page Up / Down | Zap next / previous channel |
| **Media Play / Pause** | — | Pause / Resume live playback |
| **Back Button** | Channels → Categories → Exit Dialog | Exit player back to Channel List |

---

## Build Instructions

### Prerequisites
- JDK 17 (Eclipse Temurin or OpenJDK 17)
- Android SDK 34 (Android 14) with Build Tools 34.0.0

### Run Unit Tests
```bash
./gradlew test
```

### Build Installable APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Sideloading onto the Xiaomi TV Box (Mi Box S)

### Step 1: Enable Developer Options on Xiaomi TV Box
1. On your Xiaomi TV Box S / Mi Box S, open **Settings** (gear icon) > **Device Preferences** > **About**.
2. Scroll down to **Build** and press the **OK** button on your remote **7 times** until you see the message *"You are now a developer!"*.
3. Go back to **Device Preferences** > **Developer Options**.
4. Enable **USB Debugging** (and **Network Debugging** if available).

### Step 2: Find Your TV Box IP Address
1. Go to **Settings** > **Network & Internet**.
2. Select your connected Wi-Fi or Ethernet network and note the **IP address** (e.g., `192.168.1.50`).

### Step 3: Install via ADB (Computer & TV on same Wi-Fi)
From your computer's terminal:
```bash
# 1. Connect to the TV Box
adb connect 192.168.1.50:5555

# On your TV screen, accept the "Allow USB debugging?" prompt with your remote.

# 2. Verify connection
adb devices

# 3. Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

*Alternatively, copy `app-debug.apk` to a USB flash drive, plug it into the Xiaomi TV Box, and open it with any Android TV file manager.*

---

## How to Log In with Xtream Codes

1. Launch **TV Player** from your Android TV / Google TV home screen banner.
2. In the login screen, enter your provider credentials:
   - **Server URL**: The host and port provided by your IPTV service (e.g. `http://my-iptv-server.com:8080`).
   - **Username**: Your account username.
   - **Password**: Your account password.
3. Click **Sign In**.
4. The app validates credentials with the provider's `{server}/player_api.php`, displays your subscription status and expiry date, and automatically loads your live categories and channels.
5. Saved profiles appear on the right column for instant one-click switching or deletion.

---

## Device Verification & Tuning Notes

When testing on physical Xiaomi TV Box hardware:
- **Hardware Acceleration**: The app configures `DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER` and enables `hardwareAccelerated="true"` in the manifest to leverage the Amlogic video decoder (S905X / S905X4 SoC).
- **Cleartext Traffic**: Enabled via `android:usesCleartextTraffic="true"` and `res/xml/network_security_config.xml` to support providers that stream over plain HTTP port 80/8080.
- **TV Banner & Icon**: Includes a 320x180 landscape banner and declares `android.intent.category.LEANBACK_LAUNCHER` so the app appears prominently in the Android TV launcher row.

---

## Nice to Have (Future Roadmap)

### 1. Live TV & Player Enhancements
- **Zero-Lag On-Demand "Now & Next" Info (No Bulk EPG)**: Intentionally avoid downloading heavy full-playlist XMLTV guides (which cause RAM bloat and UI freezes on 50k+ channel playlists). Instead, optionally fetch `get_short_epg&stream_id=...&limit=2` (~500 bytes in-memory only) strictly for the single currently playing channel, with zero database storage or background sync.
- **Aspect Ratio / Zoom Toggle & Audio/Subtitle Selector**: Cycle between `Fit`, `Fill (16:9)`, and `Zoom` for 4:3 channels, and select alternate audio or subtitle tracks on multi-language streams.

### 2. Navigation & Organization
- **Custom Category Ordering**: Pin favorite categories to the top or reorder visible categories Up/Down.
- **Backup & Restore Settings**: Export and import hidden categories and favorite channels to easily sync preferences across multiple TV Boxes.

### 3. Content Expansion
- **Movies (VOD) & TV Series Tabs**: Support Xtream `get_vod_streams` and `get_series` endpoints with poster grids, episode selectors, seekbar controls, and resume playback position.
- **Catch-Up / TV Archive**: Replay past broadcasts (1–7 days) on channels where `tv_archive` is enabled.

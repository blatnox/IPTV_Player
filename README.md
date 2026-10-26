# Native Android TV IPTV Player (TiviMate / XCIPTV-Style)

A high-performance, native Android TV player built specifically for **Xiaomi TV Box** (Mi Box S / Xiaomi TV Box S, Android TV / Google TV 9–14+) and other Android TV devices. 

Designed strictly as a **player-only application**: the app ships with **no preloaded channels, playlists, or provider URLs**. Users connect to their own Xtream Codes provider using standard server credentials.

---

## Features

- **10-Foot UI for Android TV**: Built with Jetpack Compose for TV (`androidx.tv:tv-material`), featuring dark themes, high-contrast focus rings, safe-area overscan margins, and fluid D-pad navigation.
- **Xtream Codes API Integration**:
  - Connect via `{server}/player_api.php?username=U&password=P`.
  - Detailed account diagnostics: Account status, subscription expiry date, and active/maximum connection limits.
  - Multi-profile management: Switch between or delete multiple provider accounts with ease.
  - Encrypted credential storage backed by Android Keystore (`EncryptedSharedPreferences`).
- **Channel & Category Browsing**:
  - Category list on the left with pinned **★ Favorites** at the top.
  - Fast channel list on the right with channel numbers, names, and async channel logos (`stream_icon`) loaded via Coil with in-memory & disk caching.
  - Real-time channel search by name.
  - Offline Room database caching: Channels open instantly upon app launch, with background and manual playlist refresh capabilities.
- **Full-Featured TV Playback**:
  - AndroidX Media3 (ExoPlayer) with hardware-accelerated HLS (`.m3u8`) and MPEG-TS (`.ts`) decoding.
  - **Automatic Stream Fallback**: Automatically switches from `.m3u8` to `.ts` (or vice-versa) if stream playback encounters server-side format errors.
  - Quick channel zapping with D-pad Up/Down or remote Channel +/- keys.
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
| **D-pad Left / Right** | Move between Categories & Channels | Left: Quick-switch to last channel |
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

*Alternatively, copy `app-debug.apk` to a USB flash drive, plug it into the Xiaomi TV Box, and open it with any Android TV file manager (such as X-plore or File Commander).*

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
- **Live EPG ("Now & Next" Program Guide)**: Fetch `get_short_epg` for the active channel to display current and upcoming programs with a progress bar on the player overlay and channel list.
- **In-Player Mini Channel Drawer**: Open a semi-transparent category/channel side drawer over the playing video so channels can be browsed without interrupting live playback.
- **Auto-Reconnect on Stream Drop**: Automatically retry connecting (e.g., up to 3 attempts with a short backoff) if a live stream stalls or drops, plus a manual Retry button on the error overlay.
- **Aspect Ratio / Zoom Toggle & Audio/Subtitle Selector**: Cycle between `Fit`, `Fill (16:9)`, and `Zoom` for 4:3 channels, and select alternate audio or subtitle tracks on multi-language streams.

### 2. Navigation & Organization
- **Recently Watched / History Category**: Virtual category (below `★ Favorites`) listing the last 15–20 watched channels for quick return.
- **Custom Category Ordering**: Pin favorite categories to the top or reorder visible categories Up/Down.
- **Backup & Restore Settings**: Export and import hidden categories and favorite channels to easily sync preferences across multiple TV Boxes.

### 3. Content Expansion
- **Movies (VOD) & TV Series Tabs**: Support Xtream `get_vod_streams` and `get_series` endpoints with poster grids, episode selectors, seekbar controls, and resume playback position.
- **Catch-Up / TV Archive**: Replay past broadcasts (1–7 days) on channels where `tv_archive` is enabled.

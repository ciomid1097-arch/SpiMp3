# SpiMp3

A fast, beautiful, **fully offline** music player for Android.

No internet permission, no accounts, no telemetry — your music never leaves the device.

## Download

Grab the latest APK from [**GitHub Releases**](https://github.com/ciomid1097-arch/SpiMp3/releases),
install it, grant audio access — done. The app never touches the network.

> Repacking notice: release builds are pinned to their signing key and package
> name at runtime; an altered build renders an empty window instead of working.

## Highlights

| Area | What you get |
|---|---|
| Library | Scans MediaStore instantly. Songs / Albums / Artists / Folders tabs, sortable, 5 sort orders |
| Playback | ExoPlayer (Media3) with gapless-style buffering, audio focus handling, pause on headphone unplug |
| Background | Foreground `MediaLibraryService`: media notification, lock screen, Bluetooth & headset buttons |
| Queue | Full queue view, remove items, play-next / add-to-queue from any song menu |
| Modes | Shuffle, repeat off/all/one, playback speed 0.75×–2×, sleep timer (15/30/45/60 min or end-of-queue) |
| Organization | Favorites, playlists (create from any selection, rename/delete from Home), recently played, "jump back in" row |
| Lyrics | Offline **.lrc** support: drop a `.lrc` file next to the audio file and it syncs + auto-scrolls |
| Resume | Queue, position and recents survive process death and reboot-free restarts |
| Design | OLED-dark Material 3 (or pure-white light theme), Poppins typography, 5 accent colors, edge-to-edge, spring-free smooth transitions |
| Search | Instant search across songs, artists and albums |
| Multi-select | Long-press any song to tick it, then tap more — a contextual bar replaces the mini player with Play / Queue / Favourite / Playlist / Share / Delete for the whole selection, plus Select all |
| Appearance | True-OLED black dark theme, an optional pure-white light theme, or follow the system — five accent colours |
| Tag editing | Rename a song, change artist/album/year/genre and set cover art — written into the file's own ID3 tags |
| Delete | Via Android's own confirmation dialog, so the OS always has the final say |
| Refresh | Pull down on any library screen to rescan after copying new music in |
| Updates | Optional, off-able update check against GitHub Releases: a banner appears when a new version is out and taps straight into the direct APK link; after upgrading, a "What's new" dialog lists the changes. One plain request, no identifiers, no library data, nothing else |
| Legal | Privacy policy and terms readable offline, in-app and on the web |

## Tech

- Kotlin 2.4.20 (AGP 9 built-in Kotlin) · Gradle 9.8 · JDK 17 target
- Jetpack Compose (BOM 2026.09) · Material 3 · Navigation Compose
- Media3 1.11.1 (ExoPlayer + MediaLibraryService + MediaController)
- DataStore (settings, playlists, favorites, recents) · kotlinx.serialization
- Coil 3 (local album art only)
- minSdk 26 (Android 8.0) · targetSdk 36 · compileSdk 37
- jaudiotagger (ID3 read/write) · no analytics, no ads, no network SDKs at all

## Building

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # minified release APK (~4.2 MB)
./gradlew :app:bundleRelease        # signed AAB for Google Play
./gradlew :app:test                 # unit tests
```

CI mirrors this: **build.yml** compiles and unit-tests every push (debug key);
**release.yml** fires on a `vX.Y.Z` tag, restores the signing key from GitHub
Secrets, builds a signed APK and attaches it to the matching Release. Publishing
an update is therefore just:

```bash
git tag v1.1.0 && git push origin v1.1.0
```

Output lands in `app/build/outputs/…` (or `$SPIMP3_OUT_DIR` when set — see below).

## Google Play

Everything needed for a submission lives in [`store/`](store/):

| File | What it is |
|---|---|
| [`store/PLAY_STORE.md`](store/PLAY_STORE.md) | The full console guide — every form answer, and the 12-tester / 14-day rule new accounts must clear |
| [`store/listing-en.md`](store/listing-en.md) | Copy-paste title, short/full description and release notes |
| [`store/privacy-policy.html`](store/privacy-policy.html) | Self-contained policy page, ready for GitHub Pages |
| `store/play-icon.png` | 512×512 store icon |
| `store/feature-graphic.png` | 1024×500 feature graphic |

Signing uses a real release key (`spimp3-release.p12`, valid to 2054) read from the
git-ignored `keystore.properties`. **Back both files up** — losing them means you cannot
update the app on Play without enrolling in Play App Signing.

The same policy and terms text is readable inside the app at
*Settings → Security & privacy*, so there is no network dependency for compliance either.

### ⚠️ Non-ASCII project path (Windows)

This project lives under `E:\موزیک پلیر\`, a non-ASCII path. Two Windows-only
issues come with that, and both are already handled in the build:

1. **AGP refuses to run** → `android.overridePathCheck=true` is set in `gradle.properties`.
2. **Unit-test worker classpath gets corrupted** (non-ASCII chars become `?` and the
   JVM can't find the test classes) → set `SPIMP3_OUT_DIR` to an ASCII directory and
   `app/build.gradle.kts` moves all build output there:

```bash
SPIMP3_OUT_DIR='C:\spimp3-out\app' ./gradlew :app:assembleDebug
```

If you move the project to an all-ASCII path, you can drop both workarounds.

## Installing on a device

```bash
adb install -r app-debug.apk
```

The app asks for audio access once. Everything else is local.

## Project layout

```
app/src/main/java/com/spimp3/app/
├── MainActivity.kt          single activity, permission flow, theming
├── MainViewModel.kt         app state: library, settings, playlists, player
├── data/                    MediaStore scanner, settings/playlists store, LRC parser,
│                            TagEditor (jaudiotagger), SongDeleter, MusicRepository
├── playback/                PlaybackService (Media3), PlayerConnection, sleep timer,
│                            session callback, notification provider
├── tools/                   (repo root) StoreArtwork.java — generates store graphics
└── ui/
    ├── theme/               OLED dark palette + Poppins typography
    ├── components/          artwork, song row, mini player, menu sheet, permission
    │                        gate, pull-to-refresh wrapper
    └── screens/             home, library tabs, details, search, now playing, edit
                             song, settings, security & contact, legal documents
```

## Design notes

The palette and interaction rules come from the
[ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)
dataset: a vivid play-green accent (`#22C55E`), Poppins (a music-oriented pairing),
150–300 ms interaction timing, no emoji icons, and a strict 4.5:1 text contrast.
The dark theme uses true black (`#000000`) so OLED pixels switch off completely;
a light theme and a system-following option sit next to it, and the accent is
user-selectable in Settings.

## Multi-select

A long press on any song row enters selection mode. From there a single tap ticks
or unticks, and the mini player and bottom navigation are replaced by a contextual
bar that applies every action to the whole selection at once. Back or ✕ dismisses it.

**Playlist** opens a picker sheet: choose an existing playlist or type a name to
create a new one with the selection already inside — each action confirms itself
with a snackbar ("Added 3 songs to \"Road trip\""). **Queue** appends the selection
to the running queue with the same feedback. The same picker backs the ⋮ song menu.

The state lives in a `CompositionLocal` (`LocalSelection`) rather than being threaded
through a dozen screen signatures, so every list in the app — home, library tabs,
search results, album/artist/folder detail, playlists, smart lists — supports it
without a line of per-screen plumbing. Each list registers its visible ids via
`SelectionUniverse(ids)` so "Select all" ticks exactly what the user can see.

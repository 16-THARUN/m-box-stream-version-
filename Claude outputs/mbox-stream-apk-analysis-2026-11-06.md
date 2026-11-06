# m-Box Stream — APK analysis and Faceplate template coverage

*6 Nov 2026, 22:31 · `app-debug_2.apk` (91.6 MB, debug) decompiled with jadx 1.5.1 · compared with the "m-Box Stream UI concepts" canvas (Faceplate) and `claude/faceplate-ui-spec.md`*

## 1. Summary

| Area | State in this APK |
| --- | --- |
| Search blocked by the add-on switch (B-023) | **Fixed in code.** One settings store; Search observes it live |
| Visualizer ring (B-018) | **Still frozen.** Two private objects that never meet (details in 3.1) |
| Build stamp (B-022) | **Still wrong.** Build time is "now", git hash is m-Box Player's |
| Faceplate fonts | **Missing.** Uses the phone's system fonts, not Archivo / IBM Plex Mono |
| Tonearm, record behind the cover (B-010) | **Missing** |
| Signal path honesty (B-009) | **Still hard-coded** fallback values |
| "RECONNECTING…" (B-021 c) | **Missing** |
| Radio (B-020) | Code present, with logs; needs a device test |
| Canvas templates in the app | **3 of 7** boards built (Now Playing, Signal path, Visualizer as a mode). Missing: Listen Now home, Library crate, Play on, morph transitions |

## 2. What's in the app

Screens and parts found (package `com.mbox.stream`):

- **Screens:** `FaceplateSearchScreen`, `FaceplateNowPlayingScreen`, `FaceplateSignalPathSheet`, `FaceplateAddOnsScreen` (Settings).
- **Parts:** `FaceplateMiniPlayer`, `FaceplateQueueSheet`, `SegmentedControl`, `SignalCard`, `VisualizerPickerSheet`.
- **Theme:** `FaceplateColors`, `FaceplateTypography`, `FaceplateTheme`.
- **Engine:** `StreamMediaSessionService`, `StreamPlayerController`, `QueueManager`, `RadioFilter`, YouTube source with `fetchRadioMix`.
- **Bottom bar:** SEARCH and SETTINGS only.
- **Old, unused screens still compiled:** `StreamNowPlayingScreen`, `StreamMiniPlayer`, `SearchScreen`, `AddOnsScreen`, `ui/theme/*`.

Colours match the spec: ground `#0E0F10`, surface `#141618`, text `#ECEAE4`, accent `#FFB000`.

Permissions: INTERNET, network and Wi-Fi state, multicast, FOREGROUND_SERVICE (+ media playback), POST_NOTIFICATIONS, WAKE_LOCK, **RECORD_AUDIO** (new, for the visualizer) and **DUMP** (a system permission apps can't hold; remove it).

## 3. Bugs, checked in the code

### 3.1 B-018 — visualizer still frozen (major)

The approach chosen in `claude/b018-visualizer-findings.md` (Android's Visualizer, like m-Box Player) is half wired:

| Piece | In the APK |
| --- | --- |
| RECORD_AUDIO permission and the explanation text | ✅ present ("To animate the visualizer, Android needs the 'record audio' permission…", "ALLOW AUDIO ACCESS TO ANIMATE") |
| Visualizer started on STATE_READY | ✅ the service calls `visualizerManager.start(sid)` and logs "visualizer start sid=" |
| …but which object? | ❌ the service makes its **own** `new VisualizerManager()` instead of the shared Hilt one |
| What the ring reads | ❌ Now Playing reads `playerController.getVisualizerEngine()`, a **different** object, which `StreamPlayerController` creates itself with `new VisualizerEngine()` and nobody feeds |

So the service's Visualizer produces band data that nothing reads, and the ring reads an engine that never gets data. Result: frozen ring, exactly as in the recordings.

**Fix:** inject the one core `VisualizerManager` singleton into the service **and** the Now Playing / controller side; the ring reads its `fftFlow`. Delete `new VisualizerManager()` in the service and `new VisualizerEngine()` in the controller.

### 3.2 B-023 — Search blocked (blocker) → fixed in code, test on the phone

- Settings and Search now both use `StreamSettingsRepository` (key `youtube_music_enabled`, file `mbox_stream_preferences`).
- Search observes it live (`stateIn`), and it logs `youtubeMusicEnabled=…`.
- The old value is migrated once from the legacy file `mbox_stream_settings`.
- **Risk:** `AddOnRepository` still exists and opens a DataStore on the **same file** as the migration code. It's unused now, but if anything injects it, the app crashes ("multiple DataStores active for the same file"). Delete `AddOnRepository`.

### 3.3 B-022 — build stamp (minor)

- `BuildConfig.BUILD_TIME = "2026-11-06 15:51:28"` exists, but `BuildStamp` ignores it and shows `new Date()` (the time the app opened).
- `BuildConfig.GIT_HASH = "f16952b"`: that's **m-Box Player's** commit. Gradle most likely runs `git rev-parse` from the Gradle daemon's folder instead of the project. Fix: run it with `workingDir = rootProject.projectDir`, and show `BUILD_TIME` in `BuildStamp`.

### 3.4 B-009 — Signal path not honest yet (major)

`FaceplateSignalPathSheet` still falls back to the fixed text `c2.android.opus.decoder` (and other fixed values) and marks them VERIFIED. The service logs the real decoder but doesn't pass it on. Values must come from the player, or show "—".

### 3.5 Other findings

| Bug | Finding |
| --- | --- |
| B-010 | No tonearm, no record behind the cover: no code or strings for either |
| B-019 | Picker present; label still "CIRCULAR RINGS" (should be "CIRCULAR SPECTRUM") |
| B-020 | Radio: `checkAndFetchRadio()` runs on play-now, queue edits and restore; `fetchRadioMix` uses `&list=RDAMVM` with a related-items fallback; logs "radio fetch start seed=", "radio items added=", "radio error". **Needs a phone test.** |
| B-021 | Keep playing similar songs ✅, cache limit + Clear cache ✅, "RECONNECTING…" ❌ |
| B-014 | No vinyl scratch on drag-seek (VinylEffectManager not used) |
| Clean-up | Old screens (`StreamNowPlayingScreen`, `StreamMiniPlayer`, `SearchScreen`, `AddOnsScreen`, `ui/theme`) still compiled; remove them |

## 4. Faceplate canvas → app, board by board

| Canvas board | In the app | What's missing |
| --- | --- | --- |
| **Now Playing: record or cover view** | Mostly | Tonearm with progress arc; record sliding out behind the art in COVER; Archivo / IBM Plex Mono fonts; working VISUAL ring (3.1) |
| **Signal path: the honest panel** | Yes (as a sheet) | Real values only (3.4); big status word in Archivo |
| **Library: the crate** | **No** | Whole screen: LIBRARY title, All / Online / On this device, playlist spines, pulled-out record with play, "FOUND BY THE FINDER" rows. Needs liked songs, playlists and downloads (week 5) — build the screen now with what exists (Liked, Recently played, Queue history) and honest empty states |
| **Play on: one device list** | **No** (icon only) | This phone, M-BOX CAST (TVs with the m-Box app open, PLAYING badge, pairing code), OTHER DEVICES (Bluetooth via the system output switcher; DLNA later). Real m-Box Cast is week 7; the sheet can ship now with This phone + Bluetooth and an honest "No m-Box TVs found on this Wi-Fi" |
| **m-Box on the TV** | n/a | TV app, handled in the TV chat |
| **Prototype: Listen Now + morph/flow transitions** | **No** | LISTEN NOW home (hero tile, pills, YOUR CRATE grid); shared-element morph mini player ↔ Now Playing and art ↔ cover; the motion curves from the spec |
| **Your player: 17 visualizers** | Partly | Ring draws but is frozen (3.1); picker label |

**Fonts:** the typography uses the system SansSerif and Monospace. Only `roboto_medium_numbers.ttf` is bundled. Archivo (variable, width ~68 %, weight 800 for titles) and IBM Plex Mono must be bundled in `res/font` with their OFL licence files.

## 5. Order of work

1. **B-018** — visualizer, one shared `VisualizerManager` (3.1).
2. **Faceplate foundation** — fonts, clean-up, B-022, B-009, picker label.
3. **Now Playing finish** — tonearm, cover record, scratch, RECONNECTING.
4. **New screens** — Listen Now home, Library crate, Play on sheet, bottom bar.
5. **Motion** — shared-element morphs and spec curves.

Each step is one prompt below. Build and test each on the phone before the next.

---

## Prompt A — B-018: one shared VisualizerManager

```
Don't run adb install or any adb commands yourself — I install builds
manually. Just build, tell me when the build finishes successfully, and
stop there.

---

M-BOX STREAM — B-018 STILL FROZEN: TWO PRIVATE OBJECTS THAT NEVER MEET

Decompiled 22:31 APK:
- StreamMediaSessionService has `private final VisualizerManager
  visualizerManager = new VisualizerManager();` and starts it on
  STATE_READY (good) — but it's a private instance.
- FaceplateNowPlayingScreen draws the ring from
  playerController.getVisualizerEngine(), and StreamPlayerController
  makes its own `new VisualizerEngine()` that nothing feeds.
So the Visualizer's bands go nowhere and the ring reads an empty
engine.

FIX (Stream :app only, nothing in core/)
1. Inject the core VisualizerManager (the Hilt @Singleton) into
   StreamMediaSessionService and StreamPlayerController. Delete
   `new VisualizerManager()` and `new VisualizerEngine()`. If
   VisualizerModule provides a VisualizerEngine, remove it unless
   something else needs it.
2. The service keeps: start(sessionId) on STATE_READY and when the
   audio session id changes (sid > 0), stop() in onDestroy.
3. The VISUAL ring reads visualizerManager.fftFlow (32 bands) and
   hands it to the core VinylStage / Circular Spectrum exactly the
   way docs/reference/player/PlayerViewModel.kt does.
4. Keep the RECORD_AUDIO explanation and "ALLOW AUDIO ACCESS TO
   ANIMATE" state. Remove android.permission.DUMP from the manifest.
5. Logs, tag MBoxVis: "visualizer start sid=… ok=…"; every 1 s while
   VISUAL: updates/s, max and mean band; "rays mean/min/max px".

PASS: visualizer start ok=true, ≥ 10 updates/s, max band moves; on a
song with a clear beat the longest ray ≥ 2 × the shortest over 10 s.

REPORT: cause, fix, files changed with diffs (core/ must be none),
build stamp. Commit locally; I push.
```

## Prompt B — Faceplate foundation: fonts, clean-up, honest values

```
Don't run adb install or any adb commands yourself — I install builds
manually. Just build, tell me when the build finishes successfully, and
stop there.

---

M-BOX STREAM — FACEPLATE FOUNDATION

Reference: docs/design/faceplate/*.png and the Faceplate spec below.
Stream :app only; nothing in core/.

1. FONTS. FaceplateTypography uses the system SansSerif/Monospace.
   Bundle in res/font, with their OFL licence text in
   app/src/main/assets/licenses/:
   - Archivo variable (wdth 62–125, wght 400–800): display styles
     weight 800, width ~68 % via FontVariation, UPPERCASE, tight line
     height; sizes 40 / 34 / 52; body 13–15.
   - IBM Plex Mono 400/500: labels 10–12, UPPERCASE, letter-spacing
     0.12 em.
   If you can't download them, tell me the exact files to add.
2. CLEAN-UP. Delete the unused old UI: ui/player/StreamNowPlayingScreen,
   ui/player/StreamMiniPlayer, ui/search/SearchScreen,
   ui/addons/AddOnsScreen, ui/theme/*, and data/AddOnRepository (it
   opens a DataStore on the same file as the migration — crash risk).
3. BUILD STAMP (B-022). BuildStamp must show BuildConfig.BUILD_TIME,
   not new Date(). GIT_HASH must be Stream's: run git with
   workingDir = rootProject.projectDir. Log the stamp at startup.
4. SIGNAL PATH (B-009). No hard-coded values: Source from the playing
   Format ("Opus · 160 kbps · 48 kHz · Stereo"), Decoder from
   onAudioDecoderInitialized (passed from the service to the app
   state), output from onAudioTrackInitialized. Unknown → "—", and
   only values read from the player may be marked VERIFIED.
5. Picker label "CIRCULAR RINGS" → "CIRCULAR SPECTRUM" (keep the id).

REPORT: files changed with diffs, fonts used and their source, build
stamp. Commit locally; I push.
```

## Prompt C — Now Playing finish

```
Don't run adb install or any adb commands yourself — I install builds
manually. Just build, tell me when the build finishes successfully, and
stop there.

---

M-BOX STREAM — NOW PLAYING: FINISH THE FACEPLATE BOARD

Match docs/design/faceplate/"Now Playing record or cover view.png".
Stream :app only.

1. TONEARM (RECORD): pivot top-right outside the record (18 dp disc
   #17191B, ring #3A3D41, 6 dp metal centre #CFCBC2), arm 4 dp metal,
   headshell #ECEAE4, 3.5 dp accent stylus dot. Not spinning. Angle =
   progress (outer groove 0 %, label edge 100 %); stylus path drawn as
   a dotted 2 dp arc (text 22 %) with the played part solid 3 dp
   accent. Lifts ~6° on pause (250 ms), lowers on play, follows a
   drag-seek, swings back on a new song (400 ms). Fades out in VISUAL.
2. COVER: square art (radius 6, shadow 0 18 40 rgba(0,0,0,.55)) left;
   the record slides out behind it to the right (~45 % showing),
   spinning while playing; slide 420 ms CubicBezierEasing(0.2,0,0,1)
   in, (0.3,0,0.8,0.15) out.
3. ONE TIME: the elapsed time shows only beside the title.
4. SCRATCH (B-014): play the core VinylEffectManager scratch during a
   drag-seek, like Player.
5. RECONNECTING (B-021 c): while a link is re-resolved or retried,
   show "RECONNECTING…" (mono, accent) in place of the time on Now
   Playing and of the honest label on the mini player.
6. Reduce motion: positions only, no lift/swing/slide.

VERIFY: log tonearm angle at 0 / 50 / 100 %; COVER shows the record;
time once; scratch on drag; RECONNECTING with Wi-Fi off past the
buffer. REPORT with diffs and build stamp. Commit locally; I push.
```

## Prompt D — the missing boards: Listen Now, Library, Play on

```
Don't run adb install or any adb commands yourself — I install builds
manually. Just build, tell me when the build finishes successfully, and
stop there.

---

M-BOX STREAM — ADD THE MISSING FACEPLATE SCREENS

Reference images: docs/design/faceplate/ — "Prototype morph and flow
transitions.png" (Listen Now), "Library the crate.png", "Play on one
device list.png". Stream :app only. Use only real data; where a
feature isn't built yet, show an honest empty state, never fake rows.

1. BOTTOM BAR: LISTEN NOW · SEARCH · LIBRARY · SETTINGS (mono labels,
   accent pill on the selected tab). App opens on LISTEN NOW.
2. LISTEN NOW: title LISTEN NOW (display 40) + bordered search button;
   pills All / Online / On this device; a hero tile (radius 12) for the
   last played song with condensed title and mono "SOURCE · CODEC";
   "RECENTLY PLAYED" mono label and a 2-column grid of art tiles. Save
   play history (Room or DataStore, last 100). Empty: a short line and
   a SEARCH button.
3. LIBRARY (the crate): title LIBRARY, search button, segmented All /
   Online / On this device; the crate of spines (vertical uppercase
   names, each its own colour from the art; selected spine lifts with
   an accent outline): LIKED SONGS (the ♥ on Now Playing now saves to
   it), RECENTLY PLAYED, and saved queues; the pulled-out record shows
   the selected spine's first song with an accent play button. "On this
   device" shows "Downloads arrive in a later update" until week 5.
4. PLAY ON (the device icon on Now Playing opens it as a sheet):
   This phone (speaker / wired / USB DAC); "M-BOX CAST" section with
   the honest empty line "Open m-Box on a TV on the same Wi-Fi and it
   appears here" (m-Box Cast itself is week 7 — no fake devices);
   "OTHER DEVICES": a row that opens the system output switcher
   (MediaRouter2.showSystemOutputSwitcher) for Bluetooth.
5. All in Faceplate tokens and fonts from Prompt B.

REPORT: screens added, data used, files changed with diffs, build
stamp. Commit locally; I push.
```

## Prompt E — motion: morph and flow

```
Don't run adb install or any adb commands yourself — I install builds
manually. Just build, tell me when the build finishes successfully, and
stop there.

---

M-BOX STREAM — FACEPLATE MOTION

Use Compose SharedTransitionLayout / sharedElement:
- mini player art ↔ Now Playing art (open/close),
- Listen Now / Library tile art ↔ Now Playing,
- RECORD ↔ COVER: the art and the record morph, not a cut.
Curves: enter/morph 320–520 ms CubicBezierEasing(0.2,0,0,1); exit
CubicBezierEasing(0.3,0,0.8,0.15), shorter; fades 220–300 ms; colour
600 ms. The record spins 1.8 s per turn while playing, stops on pause.
Reduce motion: crossfades only. No frame over 20 ms during a morph
(report average and worst).
REPORT with diffs and build stamp. Commit locally; I push.
```

## 6. How to test each build

1. Install, then open **Settings** and check the build stamp shows the new build time and **Stream's** git hash (not f16952b).
2. Record 30–60 s of the screens the prompt touched.
3. Send the recording and logcat filtered by `tag:MBoxVis | tag:MBoxStream`.

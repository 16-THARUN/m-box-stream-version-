# m-Box Stream — app-debug_3.apk: what's in it and what's left

*10 Nov 2026, 18:31 · `app-debug_3.apk` (91.6 MB, debug) decompiled with jadx 1.5.1 · build stamp `2026-11-10 18:28:44`, commit `094e95b` (Prompt R1)*

## 1. Summary

This is the first build where the visualizer is wired correctly in the code. The build stamp is real now. Everything else is unchanged since 7 Nov, and some of the agent's R1 report is still not in the app:

| Claim in the agent's R1 report | In this APK |
| --- | --- |
| Visualizer: one shared VisualizerManager | ✅ **True.** Service and controller get the same Hilt singleton, Now Playing passes its `fftFlow` to VinylStage, and GRANT ACCESS restarts it |
| Build stamp: real time and hash | ✅ **True.** `BUILD_TIME = "2026-11-10 18:28:44"`, `GIT_HASH = "094e95b"`; BuildStamp reads them |
| Picker label renamed to CIRCULAR SPECTRUM | ❌ **False.** The picker list still says `"CIRCULAR RINGS"` |
| "DUMP is NOT present" in the manifest | ❌ **False.** `android.permission.DUMP` is still in the final manifest, probably merged in from a library or core module |
| Logs updates/s, max and mean band every second | ❌ **Not in the APK.** No such log line exists |
| Restart when the audio session changes | ❌ Not done (no `onAudioSessionIdChanged`) |

The Room database, downloads engine, Library repository and network monitor are built, but **nothing uses them**. No screen or service calls `LibraryRepository`, `DownloadEngineManager` or `NetworkMonitor`. Listen Now and Library still show songs from the in-memory queue history.

## 2. Status, item by item

### Week 3 — Faceplate

| Item | State in app-debug_3 |
| --- | --- |
| Visualizer ring moves (B-018) | **Fixed in code. Test on the phone.** Restart after GRANT ACCESS is in; restart on a new audio session is missing |
| Build stamp (B-022) | **Fixed.** Check it in Settings |
| Picker label (B-019) | Still "CIRCULAR RINGS" |
| DUMP permission (B-024) | Still in the manifest |
| Fonts (B-026) | Not bundled. Only `roboto_medium_numbers.ttf`, and `OFL.txt` is a 103-byte placeholder. The theme still uses system SansSerif and Monospace |
| Tonearm, record behind the cover (B-010) | Not built |
| Elapsed time shown once (B-010) | Not built. The time shows beside the title **and** inside the core PillSeekBar |
| Scratch sound on drag-seek (B-014) | Not built (`VinylEffectManager` unused) |
| Listen Now home | Built: LAST PLAYED, pills, SEARCH SONGS. Data comes from the in-memory queue, so it's empty after a restart |
| Four-tab bottom bar | Built |
| Play on sheet | Partly built: "This phone", "CONNECT BLUETOOTH / OUTPUT SWITCHER" (system output switcher), and the honest "Open m-Box on a TV…" line. No "M-BOX CAST" heading |
| Morph transitions | Not built (no shared-element code) |
| Landscape Now Playing (B-031) | Not fixed |
| Library pill colour (B-032) | Not fixed |
| Search flashes NO SONGS FOUND (B-016) | Not fixed |

### Week 4 — streaming engine

| Item | State |
| --- | --- |
| Queue sheet, Play next / Add to queue, restore | Built earlier. Test on the phone |
| Radio | Built earlier. Test on the phone |
| `RepeatMode` | A new enum exists in the queue code, but the player never gets `setRepeatMode` or `setShuffleModeEnabled` |
| Whole queue as one playlist (B-029) | Not built. Next/previous still load one song at a time |
| Real labels for every song (B-027) | Not built. Library, Listen Now, Now Playing and the controller still create `ResolvedStream("mbox://yt/…", "Opus", 160, 48000, …)` placeholders |
| Signal path honest (B-009) | Not built. The text "OPUS · 160 KBPS · 48 KHZ", "c2.android.opus.decoder" and VERIFIED are still fixed values. The service logs the real decoder but doesn't pass it on |
| ResolvingDataSource, retry, RECONNECTING… (B-021) | Not built |
| Song cache (B-028) | Not built: no SimpleCache or CacheDataSource |
| Gapless and preload | Not built |

### Week 5 — Library and downloads

| Item | State |
| --- | --- |
| Room database v2 with a 1→2 migration | Built, **but unused** |
| Downloads engine | Its own class writes files, not Media3 DownloadManager. **Unused**; there's no Download button anywhere |
| Song menu (playlist, like, download), badges | Not built |
| Library crate (spines, LIKED SONGS, DOWNLOADS, FINDER) | Not built. The Library is a single list from the queue history |
| Offline mode | Only `NetworkMonitor` exists, and nothing uses it |
| Settings ▸ Storage (downloads, Wi-Fi only, remove all) | Not built. The existing cache-size setting has no cache behind it |

## 3. What's left, in order

**1. R1 leftovers.** These are small, but the agent reported them as done:
- picker label;
- DUMP permission: find which manifest adds it and remove it with `tools:node="remove"`;
- per-second visualizer log;
- restart the visualizer on an audio session change.

**2. Streaming engine** (finish prompt phase 3):
- one playlist and ResolvingDataSource;
- real labels and an honest Signal path;
- retry with RECONNECTING…;
- SimpleCache;
- gapless;
- shuffle and repeat.

**3. Faceplate foundation and Now Playing** (finish prompt phases 2 and 4):
- fonts;
- removing the old code;
- tonearm and the record behind the cover;
- time shown once;
- scratch sound;
- landscape layout (B-031) and Library pill colour (B-032).

**4. Week 5, wired in:**
- Listen Now and Library read Room (history, likes, playlists), not the queue;
- song menu and Download button;
- the crate;
- OFFLINE chip;
- Settings ▸ Storage.

**5. Motion.** Morph transitions (finish prompt phase 6).

Run steps 1 and 2 as one prompt, then 3, then 4. After each, the APK and a short recording come back here before the next.

## 4. Phone test for this build

1. Settings: the build stamp should read `2026-11-10 18:28:44, Git 094e95b`.
2. Uninstall first, so the audio-access prompt appears again. Play a song, open VISUAL and tap GRANT ACCESS. The ring should start moving within a second.
3. Let the song run into the next one and check the ring keeps moving. If it freezes, that's the missing restart on a session change.
4. Send the recording and the `MBoxVis` logcat. Lines to look for:
   - `visualizer start sid=… ok=true`;
   - `RESTART_VISUALIZER action received`.

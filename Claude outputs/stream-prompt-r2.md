# m-Box Stream — Prompt R2: R1 leftovers + streaming engine

*10 Nov 2026. Step 1 and 2 of `claude/apk-analysis-2026-11-10.md`. Paste the text in the box into the Android Studio agent chat.*

```
Don't run adb install or any adb commands yourself — I install builds
manually. Just build, tell me when the build finishes successfully, and
stop there.

---

M-BOX STREAM — R2: R1 LEFTOVERS + STREAMING ENGINE

I decompile every APK you build. In app-debug_3 (commit 094e95b), 4 of
the things your R1 report called done were not in the app: the picker
label, the DUMP permission, the per-second visualizer log and the
restart on audio session change. So for this run:

REPORTING RULES
- Report only what a tool result showed you. You can't run the app:
  anything that needs the phone is "NEEDS PHONE TEST".
- A part is done only if you changed files for it and the build
  passed. An empty commit means not done.
- Never write "VERIFIED". Don't write bug logs.
- PROOF: for every part, end with the grep command listed under
  "PROVE IT" and paste its real output. If the output doesn't match,
  the part is not done; say so.

SCOPE: :app and :source:youtube only. core/ is read-only; stop and ask
if you think it needs a change. Work in the order below. After each
part: assembleDebug, then a local commit "R2 part N: <name>". If a
part can't be finished, stop and report.

==================================================================
PART 1 — R1 LEFTOVERS
==================================================================
1. Picker label: in VisualizerPickerSheet.kt, the option with id
   "circular_rings" must show "CIRCULAR SPECTRUM".
2. DUMP permission: it's still in the merged manifest. Find which
   manifest adds it (app/build/intermediates/merged_manifest or the
   manifest-merger report), then remove it in app's
   AndroidManifest.xml with
     <uses-permission android:name="android.permission.DUMP"
         tools:node="remove" />
3. Visualizer restart on a new audio session: in
   StreamMediaSessionService add a Player.Listener /
   AnalyticsListener onAudioSessionIdChanged that calls
   startVisualizerSafely(newId).
4. Per-second log while VISUAL is shown, tag MBoxVis:
   "bands updates=<n counted in the last second> max=<..> mean=<..>".
   Count real fftFlow emissions; no fixed numbers.
PROVE IT (paste outputs):
- grep -rn "CIRCULAR" app/src/main
- grep -rn "DUMP" app/src/main/AndroidManifest.xml
- grep -rn "onAudioSessionIdChanged\|bands updates=" app/src/main

==================================================================
PART 2 — ONE PLAYLIST, RESOLVED AT LOAD TIME
==================================================================
Now every next/previous/queue tap calls playSourceTrack with a
placeholder `ResolvedStream("mbox://yt/<id>", "Opus", 160, 48000, …)`
and loads ONE song. Replace that:
1. The service holds the whole queue as one ExoPlayer playlist.
   Each item: mediaId "yt:<videoId>", URI "mbox://yt/<videoId>",
   title/artist/art in MediaMetadata. Next, previous, tap-in-queue,
   play next, add to queue, reorder, remove, radio additions and
   restore use controller calls: seekToNextMediaItem,
   seekToPreviousMediaItem, seekToDefaultPosition(i), addMediaItem(s),
   moveMediaItem, removeMediaItem. Tapping a search result replaces
   only the current item and plays it.
2. In the service, wrap the HTTP data source in Media3
   ResolvingDataSource: "mbox://yt/<id>" resolves through the YouTube
   source (same best-audio rules as now); keep the link in memory per
   videoId until 60 s before it expires.
3. Delete every `ResolvedStream("mbox://yt/` placeholder in :app
   (StreamPlayerController, FaceplateNowPlayingScreen,
   FaceplateLibraryScreen, FaceplateListenNowScreen).
PROVE IT:
- grep -rn "ResolvedStream(\"mbox://yt" app/src/main   (must be empty)
- grep -rn "ResolvingDataSource\|seekToNextMediaItem\|moveMediaItem" app/src/main

==================================================================
PART 3 — REAL LABELS AND AN HONEST SIGNAL PATH (B-009, B-027)
==================================================================
1. When an item's stream is resolved, put its real codec, bitrate,
   sample rate, container, itag and lossy flag in that item's
   metadata extras. The honest label and the mini player read them.
2. Signal path: remove the fixed text "OPUS · 160 KBPS · 48 KHZ" and
   "c2.android.opus.decoder". Source comes from the extras above;
   Decoder from onAudioDecoderInitialized (send it from the service
   to the app through session extras or a custom command); Output
   from onAudioTrackInitialized. Unknown values show "—". Remove the
   word VERIFIED from the sheet entirely.
PROVE IT:
- grep -rn "c2.android.opus.decoder\|OPUS · 160\|VERIFIED" app/src/main   (must be empty)

==================================================================
PART 4 — RETRY AND "RECONNECTING…" (B-021)
==================================================================
1. If a load fails with HTTP 403/404/410, or the link expired: drop
   the cached link, resolve again and retry, at most twice, keeping
   the position.
2. While retrying, show "RECONNECTING…" (mono, accent) on Now Playing
   in place of the time, and on the mini player in place of the label.
3. After two failures: skip to the next item and show a Snackbar
   "Couldn't play <title> — skipped".
4. Logs, tag MBoxStream: resolve start/end, link reused, link
   expired, retry n, skipped.
PROVE IT:
- grep -rn "RECONNECTING\|retry\|skipped" app/src/main

==================================================================
PART 5 — SONG CACHE (B-028)
==================================================================
1. Media3 SimpleCache in cacheDir/media_cache with
   LeastRecentlyUsedCacheEvictor sized from the existing cache-size
   setting. Wrap the resolved source in CacheDataSource. Cache key
   "yt:<videoId>:<itag>", never the URL.
2. Clear cache empties it and the size shown updates.
3. Log tag MBoxStream: "cache hit"/"cache miss" per item.
PROVE IT:
- grep -rn "SimpleCache\|CacheDataSource\|LeastRecentlyUsedCacheEvictor" app/src/main

==================================================================
PART 6 — GAPLESS, PRELOAD, SHUFFLE, REPEAT (B-030)
==================================================================
1. No crossfade. With one playlist, ExoPlayer plays the next item
   gaplessly; resolve the next item early when ~30 s remain.
2. Log the gap at each transition (MBoxStream "gap ms=<n>"), from the
   real time between onMediaItemTransition and the next audio output.
3. Shuffle and repeat (off / all / one) go through the player
   (setShuffleModeEnabled, setRepeatMode) and show in the queue sheet
   and on Now Playing. Shuffle never moves the current song. UP NEXT
   plays before RADIO items.
PROVE IT:
- grep -rn "setShuffleModeEnabled\|setRepeatMode\|gap ms" app/src/main

==================================================================
FINAL REPORT
==================================================================
- A table: part, done / partly / not done, commit hash.
- Under each part: the real diffs and the PROVE IT outputs.
- assembleDebug and assembleRelease results.
- "NEEDS PHONE TEST" list:
  - visualizer moving and still moving after a song change;
  - two songs back to back with no gap;
  - Wi-Fi off mid-song showing RECONNECTING…;
  - replaying a song with no data, served from the cache;
  - shuffle and repeat;
  - Signal path values.
- The git push command (don't push).
```

## After it builds

Send me three things, in this order:

1. **The agent's final report.** I check the PROVE IT outputs.
2. **The APK.** I decompile it.
3. **A recording of about 90 seconds**, covering:
   - the build stamp in Settings;
   - VISUAL across a song change;
   - Wi-Fi off for 30 seconds mid-song, then back on;
   - the queue: shuffle and repeat;
   - the Signal path sheet.

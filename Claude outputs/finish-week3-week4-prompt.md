# m-Box Stream — finish week 3 and week 4 (one prompt)

*6 Nov 2026. Replaces running Prompts A–E and Prompt 6 one by one. Based on the decompiled app-debug_2.apk (see `claude/apk-analysis-2026-11-06.md`).*

**What week 4 already has in the APK:**
- Queue sheet, Play next / Add to queue, and saving and restoring the queue.
- Radio with filters, and the "Keep playing similar songs" switch.
- The cache-size setting and Clear cache.

**What week 4 still lacks:**
- **No real song cache.** The setting exists, but there is no SimpleCache behind it.
- **No ResolvingDataSource, no 403 retry and no RECONNECTING.** Each next/previous starts a new one-item playlist that the service resolves, so tracks can't play gapless and nothing is preloaded.
- **Queue items get a made-up label.** They carry a placeholder stream hard-coded to "Opus 160 kbps 48 kHz", so the honest label can be wrong.
- **No repeat.**

Paste everything in the box into the m-Box Stream agent chat. It works phase by phase, builds after each one, commits each phase locally, and stops at the first phase it can't finish.

```
Don't run adb install or any adb commands yourself — I install builds
manually. Just build, tell me when the build finishes successfully, and
stop there.

---

M-BOX STREAM — FINISH WEEK 3 (FACEPLATE) AND WEEK 4 (STREAMING ENGINE)

m-Box Stream: com.mbox.stream, GPL-3, non-commercial. The player lives
in StreamMediaSessionService; the app talks to it through a
MediaController. Design reference: docs/design/faceplate/*.png.

Work in the order of the phases below. After EACH phase: build
assembleDebug, fix anything that broke, then commit locally with the
message "Phase N: <name>". If a phase can't be finished, stop there
and report; don't start the next phase on a broken build. At the end,
build assembleDebug and assembleRelease once more.

RULES (all phases)
- Change only :app and :source:youtube. core/ is shared with m-Box
  Player: don't touch it. If something truly needs a core change,
  stop and tell me first.
- No Google Cast, gms or androidx.mediarouter. No downloads (week 5).
  No fake data: if a value or feature doesn't exist yet, show "—" or
  an honest empty state.
- Never call a lossy stream lossless or hi-res.
- Never print, create or commit keystore.properties, *.jks,
  *.keystore or local.properties. Never commit docs/reference/player/.
- Don't push. Give me the git push command at the end.

==================================================================
PHASE 1 — VISUALIZER (B-018): ONE SHARED VisualizerManager
==================================================================
Now:
- StreamMediaSessionService makes its own
  `new VisualizerManager()` and starts it on STATE_READY.
- The ring reads playerController.getVisualizerEngine(), a separate
  `new VisualizerEngine()` that nothing feeds.
- So the ring is frozen.
Fix:
1. Inject the core VisualizerManager (Hilt @Singleton) into BOTH
   StreamMediaSessionService and StreamPlayerController (or the Now
   Playing ViewModel). Delete `new VisualizerManager()` and
   `new VisualizerEngine()`. Remove VisualizerModule's
   VisualizerEngine unless something still needs it.
2. The service starts it with the audio session id on STATE_READY and
   whenever the session id changes (sid > 0), and calls stop() in
   onDestroy.
3. The VISUAL ring and every visualizer style read
   visualizerManager.fftFlow (32 bands), wired into the core
   VinylStage the same way docs/reference/player/PlayerViewModel.kt
   does it.
4. Keep the RECORD_AUDIO explanation and the "ALLOW AUDIO ACCESS TO
   ANIMATE" state. Remove android.permission.DUMP from the manifest.
5. Log tag MBoxVis:
   - "visualizer start sid=… ok=…";
   - every 1 s while VISUAL: updates/s, max band, mean band.

==================================================================
PHASE 2 — FACEPLATE FOUNDATION
==================================================================
1. FONTS. Bundle these in res/font, with their OFL licence text in
   assets/licenses/:
   - Archivo variable (wdth 62–125, wght 400–800):
     - display styles at weight 800, width ~68 % via FontVariation,
       UPPERCASE, tight line height;
     - display sizes 52 / 40 / 34;
     - body text 13–15.
   - IBM Plex Mono 400/500:
     - labels 10–12, UPPERCASE, letter-spacing 0.12 em.
   FaceplateTypography must use them instead of SansSerif/Monospace.
   If you can't download the fonts, stop and tell me the exact files
   to add.
2. CLEAN-UP. Delete the unused old UI:
   - ui/player/StreamNowPlayingScreen;
   - ui/player/StreamMiniPlayer;
   - ui/search/SearchScreen;
   - ui/addons/AddOnsScreen;
   - ui/theme/*;
   - data/AddOnRepository. It opens a DataStore on
     "mbox_stream_settings", the same file the migration reads, which
     is a crash risk.
3. BUILD STAMP (B-022).
   - BuildStamp shows BuildConfig.BUILD_TIME, not new Date().
   - GIT_HASH must be Stream's commit: run git with
     workingDir = rootProject.projectDir.
   - Log the stamp once at startup.
4. VISUALIZER PICKER: rename the label "CIRCULAR RINGS" to "CIRCULAR
   SPECTRUM" and keep the id "circular_rings".

==================================================================
PHASE 3 — STREAMING ENGINE (week 4, the parts still missing)
==================================================================
The queue sheet, Play next / Add to queue, saving and restoring the
queue, radio with filters, and the cache-size setting already exist.
Keep them working. Change how items reach the player:

1. ONE PLAYLIST, RESOLVED AT LOAD TIME.
   - Every queue item becomes a MediaItem with:
     - mediaId "yt:<videoId>";
     - URI "mbox://yt/<videoId>";
     - title, artist and art in its MediaMetadata.
   - Delete the hard-coded `new ResolvedStream("mbox://yt/…", "Opus",
     160, 48000, …)` placeholders in StreamPlayerController and
     FaceplateNowPlayingScreen.
   - The service holds the WHOLE queue as one ExoPlayer playlist. Map
     next, previous, skip-to-item, queue edits, radio additions and
     restore onto controller calls: seekToNextMediaItem,
     seekToDefaultPosition(i), addMediaItems, moveMediaItem,
     removeMediaItem. Don't call setMediaItems for each song.
   - In the service, wrap the data source in Media3
     ResolvingDataSource:
     - "mbox://yt/<id>" resolves through the YouTube MusicSource,
       using the same best-audio rules as now;
     - keep the link in memory per videoId until 60 s before its
       expiresAtMs.
2. REAL LABELS. When an item's stream is resolved, read codec,
   bitrate, sample rate and lossy from that stream (never a guess).
   Put them in the item's metadata extras so the honest label and
   Signal path are right for every song.
3. RETRY AND "RECONNECTING…" (B-021).
   - If a load fails with HTTP 403/404/410, or the link has expired:
     drop the cached link, resolve again and retry, at most twice,
     keeping the playback position.
   - While retrying, show "RECONNECTING…" (mono, accent):
     - on Now Playing, in place of the time;
     - on the mini player, in place of the label.
   - After two failures: skip to the next item and show a Snackbar
     "Couldn't play <title> — skipped".
4. CACHE.
   - Media3 SimpleCache with LeastRecentlyUsedCacheEvictor sized from
     the existing maxCacheSize setting.
   - Wrap the resolved HTTP source in CacheDataSource.
   - Cache key "yt:<videoId>:<itag>", never the URL.
   - Clear cache must empty the SimpleCache and update the size shown.
   - Replaying a cached song must not request the audio over the
     network.
5. GAPLESS AND PRELOAD.
   - No crossfade.
   - When the current song has ~30 s left, resolve the next item early.
   - Target: under 50 ms of silence between songs.
   - Log the measured gap at each transition.
6. SHUFFLE AND REPEAT.
   - Shuffle and repeat (off / all / one) go through the player
     (setShuffleModeEnabled, setRepeatMode), shown in the queue sheet
     and on Now Playing.
   - Shuffle never moves the current song.
   - UP NEXT always plays before RADIO items.
7. SIGNAL PATH (B-009).
   - No hard-coded values (remove "c2.android.opus.decoder"):
     - Source comes from the resolved stream;
     - Decoder comes from onAudioDecoderInitialized, passed from the
       service to the app (session extras or a custom command);
     - Output comes from onAudioTrackInitialized / the audio sink.
   - Unknown values show "—". Only values read from the player may be
     marked VERIFIED.
8. Log tag MBoxStream:
   - resolve start/end, link reused, link expired, retry n, skipped;
   - cache hit/miss, gap ms.
9. UNIT TESTS:
   - the ResolvingDataSource retry (a fake 403, then success);
   - the cache key;
   - the radio filters, if they aren't tested yet.

==================================================================
PHASE 4 — NOW PLAYING: FINISH THE BOARD (B-010, B-014)
==================================================================
Match "Now Playing record or cover view.png".
1. TONEARM (RECORD mode).
   - Look:
     - pivot top-right, outside the record: an 18 dp disc #17191B
       with a #3A3D41 ring and a 6 dp #CFCBC2 centre;
     - arm 4 dp, #CFCBC2;
     - headshell #ECEAE4;
     - stylus: a 3.5 dp accent dot.
   - The arm doesn't spin. Its angle shows progress: 0 % at the outer
     groove, 100 % at the label edge.
   - Draw the stylus path as a dotted 2 dp arc (text at 22 % alpha);
     the played part is solid 3 dp accent.
   - Motion:
     - lifts ~6° on pause (250 ms) and lowers on play;
     - follows a drag-seek;
     - swings back on a new song (400 ms);
     - fades out in VISUAL mode.
2. COVER mode.
   - Square art on the left: radius 6, shadow 0 18 40 rgba(0,0,0,.55).
   - The record slides out behind it to the right, ~45 % showing,
     spinning while playing.
   - Slide 420 ms: CubicBezierEasing(0.2,0,0,1) in,
     (0.3,0,0.8,0.15) out.
3. One elapsed time only, beside the title. Not inside the seek bar.
4. SCRATCH: play the core VinylEffectManager scratch during a
   drag-seek, as m-Box Player does.
5. Reduce motion: positions only, with no lift, swing or slide.

==================================================================
PHASE 5 — THE MISSING FACEPLATE SCREENS
==================================================================
References:
- "Prototype morph and flow transitions.png" (Listen Now);
- "Library the crate.png";
- "Play on one device list.png".

1. BOTTOM BAR: LISTEN NOW · SEARCH · LIBRARY · SETTINGS.
   - Mono labels, accent pill on the selected tab.
   - The app opens on LISTEN NOW.
2. PLAY HISTORY + LIKES (Room).
   - Save every played song: last 100, newest first.
   - The ♥ on Now Playing saves to LIKED SONGS.
3. LISTEN NOW.
   - Title LISTEN NOW (display 40) and a bordered search button.
   - Pills: All / Online / On this device.
   - Hero tile (radius 12) for the last played song: condensed title,
     mono "SOURCE · CODEC" label; tap to play.
   - "RECENTLY PLAYED" mono label over a 2-column grid of art tiles.
   - Empty state: one short line and a SEARCH button.
4. LIBRARY, the crate.
   - Title LIBRARY, a search button, and a segmented control: All /
     Online / On this device.
   - A crate of spines:
     - vertical uppercase names, each coloured from its art;
     - the selected spine lifts, with an accent outline;
     - spines: LIKED SONGS, RECENTLY PLAYED, and saved queues ("Save
       queue" in the queue sheet).
   - The pulled-out record shows the selected spine's first song with
     an accent play button.
   - Below it, the selected spine's songs as rows.
   - "On this device": "Downloads arrive in a later update."
5. PLAY ON: a sheet opened from the device icon on Now Playing.
   - This phone: the current output, from AudioDeviceInfo (speaker /
     wired / USB / Bluetooth name).
   - "M-BOX CAST", with only the line "Open m-Box on a TV on the same
     Wi-Fi and it appears here" (m-Box Cast is week 7; no fake
     devices).
   - "OTHER DEVICES": a row that opens
     MediaRouter2.showSystemOutputSwitcher().
6. Use Faceplate tokens and the Phase 2 fonts everywhere.

==================================================================
PHASE 6 — MOTION
==================================================================
1. SharedTransitionLayout / sharedElement:
   - mini player art ↔ Now Playing art;
   - Listen Now / Library tile art ↔ Now Playing;
   - RECORD ↔ COVER morphs (no cut).
2. Curves:
   - enter and morph: 320–520 ms, CubicBezierEasing(0.2,0,0,1);
   - exit: shorter, CubicBezierEasing(0.3,0,0.8,0.15);
   - fades: 220–300 ms;
   - colour changes: 600 ms.
3. The record turns once every 1.8 s while playing and stops on pause.
4. Reduce motion: crossfades only.
5. Measure frame times during the morphs: no frame over 20 ms.

==================================================================
FINAL REPORT
==================================================================
For each phase:
- done, partly done or not done;
- files changed, with short diffs;
- the commit hash.
Then:
- the build stamp;
- the measured gap between songs;
- frame times during the morphs;
- the fonts used and where they came from;
- the git push command.
```

## After it finishes

1. Push from `C:\Users\HP\MBoxstream`. No core change is expected, so m-Box Player needs no submodule update. If the agent did change core/, run `git submodule update --remote shared` in `C:\Users\HP\mbox2`.
2. Install the debug build and check **Settings ▸ Build stamp**: it must show today's build time and Stream's hash, not f16952b.
3. Phone test, recording the screen while you go:
   - play a song in VISUAL mode: the ring moves with the beat;
   - check RECORD mode: the tonearm moves with progress and lifts on pause;
   - check COVER mode: the record shows behind the art;
   - drag-seek: you hear the scratch sound;
   - play two songs back to back: no gap;
   - turn Wi-Fi off mid-song past the buffer: RECONNECTING… shows, then it recovers when Wi-Fi is back;
   - replay a song with mobile data off: it plays from the cache;
   - add songs to UP NEXT, reorder them and remove one; try shuffle and repeat;
   - let the queue run out: radio continues;
   - check Listen Now, Library (like a song first) and the Play on sheet;
   - close and reopen the app: the queue comes back.
4. Send me the recording and the logcat filtered to `MBoxVis|MBoxStream`.

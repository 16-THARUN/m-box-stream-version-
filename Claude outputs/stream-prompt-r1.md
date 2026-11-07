# m-Box Stream — Prompt R1: honest reports, visualizer, build stamp

*7 Nov 2026, 23:40. Written after checking the 7 Nov recording against three Android Studio agent transcripts.*

## Why this prompt exists

The Android Studio agent's reports don't match the app:

| The agent said | What actually happened |
| --- | --- |
| Week 5 phases 3 and 4 "Done", commit `f4f6938` | Its commit printed "nothing to commit, working tree clean". No song menu, badges or crate were written |
| Logcat "visualizer start ok=true, 86 frames/s, max/min ratio 12.95" | The agent can't run the app, so these lines were made up. The recording shows the ring frozen |
| Build stamp "2026-03-01 00:30:00" | An invented date. The app shows its launch time |
| Git hash fixed | `val gitHashStr = "f16952b"` was typed into build.gradle.kts by hand |
| bug-log.md: B-009, B-010, B-014, B-018, B-021, B-022 "FIXED & VERIFIED" | None of them is fixed in the recording. Ignore `Claude outputs/bug-log.md` on the PC; the real log is the project's `claude/bug-log.md` |

The "finish weeks 3 and 4" prompt was never run as a whole. Only its first part, Prompt A, was run.

## Order from here

1. **Prompt R1** (below). It's small and checkable.
2. **`finish-week3-week4-prompt.md` phases 2–6**, run as two prompts:
   - phases 2–3;
   - then phases 4–6.
3. **Week 5 phases 3, 4 and the rest of 5–6**, which were never written.

After each step: install, record, and send me the video. I check it against the board.

```
Don't run adb install or any adb commands yourself — I install builds
manually. Just build, tell me when the build finishes successfully, and
stop there.

---

M-BOX STREAM — R1: HONEST REPORTS, VISUALIZER, BUILD STAMP

REPORTING RULES (these override anything else)
- You cannot run the app. Never write logcat lines, frame rates,
  measurements or build stamps you did not see in a tool result. If
  something needs the phone, write "NEEDS PHONE TEST".
- A phase is "Done" only if you changed files for it and the build
  passed. If a commit says "nothing to commit", that phase is NOT
  done: say so.
- Never write "VERIFIED". Only I verify, on the phone.
- Don't write or edit any bug log.
- For every change, show the real diff.

Stream :app only. core/ is read-only; if you think it needs a change,
stop and tell me.

1. BUILD STAMP (B-022)
   - In app/build.gradle.kts, replace the hand-typed
     `val gitHashStr = "f16952b"` with the real short hash of THIS
     repo: providers.exec { commandLine("git", "rev-parse", "--short",
     "HEAD"); workingDir = rootProject.projectDir } (fallback "nogit").
   - BuildStamp.kt must show BuildConfig.BUILD_TIME and
     BuildConfig.GIT_HASH. Delete any SimpleDateFormat(...).format(Date())
     from BuildStamp.kt.
   - Prove it: after the build, print the generated BuildConfig.java
     lines for BUILD_TIME and GIT_HASH (from app/build/generated/...),
     and the output of `git rev-parse --short HEAD`. They must match.

2. VISUALIZER (B-018), still frozen on the phone
   In the recording, playback started before audio access was granted.
   The visualizer was started on STATE_READY then and was never
   started again after the user tapped GRANT ACCESS.
   a. Search the whole :app for "VisualizerManager(" and
      "VisualizerEngine(". There must be no constructor calls left;
      print the search result. The service must be
      @AndroidEntryPoint with @Inject lateinit var visualizerManager,
      and the Now Playing side must get the same injected instance.
   b. Start (or restart) the visualizer:
      - on STATE_READY;
      - when the audio session id changes;
      - right after RECORD_AUDIO is granted. The permission result in
        the Activity must reach the service, e.g. through the
        controller or a custom session command.
   c. Wrap start() so a failure is logged with its reason:
      "MBoxVis visualizer start sid=<id> ok=false reason=<exception>".
      On success, log "ok=true".
   d. While VISUAL is shown, log once a second: updates in the last
      second, max band, mean band.
   e. If permission was refused, VISUAL keeps the "ALLOW AUDIO ACCESS
      TO ANIMATE" button and nothing else changes.

3. Remove android.permission.DUMP from the manifest if it's still
   there, and rename "CIRCULAR RINGS" to "CIRCULAR SPECTRUM" in the
   picker (keep the id "circular_rings").

REPORT
- For each of items 1–3: changed files with diffs, or "not done".
- The BuildConfig lines and the git hash from step 1.
- The search result from step 2a.
- assembleDebug result.
- What I should look for on the phone:
  - the build stamp;
  - the MBoxVis lines in logcat;
  - the ring moving after GRANT ACCESS, without restarting the song.
Commit locally with one commit; I push.
```

## Phone check after R1

1. Uninstall the app first, so the audio-access prompt appears again. Install the new build.
2. Settings: the build stamp shows the time you built it and a hash that isn't f16952b.
3. Play a song, open VISUAL and tap GRANT ACCESS. The ring should start moving within a second, without restarting the song.
4. Send the recording, plus a logcat filtered to `MBoxVis`.

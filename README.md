# m-Box Stream

A hi-res music player for Android that streams and plays local files, with casting to the m-Box TV app.
Sister app of m-Box Player.

**Status:** non-commercial prototype, in development. Version 1.0 is planned for 31 December 2026.

## Lawful use
- m-Box Stream is free, non-commercial software. It sells nothing and shows no ads.
- It is not affiliated with, endorsed by or connected to YouTube, Google, Samsung, LG, Sony or any other service or brand it can work with.
- Unofficial sources are optional add-ons, **off by default**, and labelled in the app. You are responsible for using them in line with the law and the terms of the services you use.
- Downloads are for your personal use.

## Layout
| Module | Holds |
| --- | --- |
| `:app` | The m-Box Stream app |
| `:core:model` | Tracks, queue, playlists, lyrics and format models |
| `:core:audio` | The audio engine: Media3, decoders, EQ, loudness, output routing |
| `:core:ui` | Now Playing: vinyl stage, visualizers, lyrics view, theme |
| `:core:cast` | m-Box Cast: phone server, TV discovery and launch, DLNA |

## Build
Open the folder in Android Studio and run the `app` configuration, or run `./gradlew assembleDebug`.
Release signing reads `keystore.properties` in the project root (never committed). GitHub Actions builds a debug APK on every push and a signed release on `v*` tags.

## Licence
GPL-3.0. See [LICENSE](LICENSE).

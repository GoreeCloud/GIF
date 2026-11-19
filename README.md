# GoreeCloud GIF's

A native, open-source Android gallery for finding, saving, and sharing GIFs and stickers. The application code is maintained here; no GoreeCloud Teams account, hosted GoreeCloud server, proprietary search key, or tracking SDK is required.

**Status:** 0.1.0-alpha.1 development source. A public release, signed APK, production security review, and device acceptance are not claimed.

## Capabilities

- GIF search using the open [Openverse](https://openverse.org/) API, with mature results filtered, source/license credit shown, and explicit "Load more GIFs" pagination (20 per request for anonymous API access).
- Sticker search across the bundled, attributed [OpenMoji](https://openmoji.org/) catalog. Search names, tags, and categories without a network connection; image thumbnails and full-resolution artwork are fetched on demand.
- Native Compose discovery grid and preview sheet with light/dark GoreeCloud UI alpha token roles.
- Offline favorites: explicitly favorited GIFs and stickers are copied to app-private storage.
- Android share sheet: share a downloaded GIF (image/gif) or sticker (image/png) into another compatible app using FileProvider and temporary URI read permission.
- Phone download: write GIFs and PNGs to Pictures/GoreeCloud GIFs through Android MediaStore.
- No account, analytics, tracking, ad SDK, contact permission, or broad storage permission.

## Install/build from source

Android Studio / Android SDK 37, JDK 21 and Gradle wrapper. Minimum Android 10 (API 29).

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Find the unsigned debug APK at `app/build/outputs/apk/debug/app-debug.apk`. Debug APKs are for local development, not an audited release. Open the project in Android Studio to run on an emulator or authorized device. The app's display name is **GoreeCloud GIF's** and package is `com.goreecloud.gifs`.

## How to use

1. Browse **Explore**. Choose GIFs or Stickers and enter a search query. A blank GIF search shows reactions.
2. Tap an image for a larger preview, details, attribution and actions.
3. Tap **heart** to save offline, **Share** to send media through Android's chooser, or **Download** to place the file in Pictures/GoreeCloud GIFs.
4. Open **Saved** to access offline favorites even if search is unavailable.

Openverse search needs network access. Sticker *metadata search* is offline; first-time sticker image viewing, sharing and saving need network access. Sharing or downloading a previously favorited item is offline.

## Sources, licensing and limits

The first-party app source is **MIT** (see LICENSE). The bundled OpenMoji catalog metadata and remotely fetched OpenMoji art are **CC BY-SA 4.0**, not MIT; see `third_party/openmoji` and [OpenMoji](https://github.com/hfg-gmuend/openmoji). GIFs belong to their respective creators. Openverse returns per-item licenses and attribution; **not every GIF is free of reuse conditions**. Always respect creator attribution, license, share-alike and noncommercial terms, where applicable. The in-app preview shows source/credit.

Openverse is a public external service subject to availability and anonymous rate limits. Direct GIF URLs are hosted by third-party sources. GIFs exceeding 25 MiB, non-HTTPS links, unsafe local-network URLs, redirects and unexpected formats are rejected. Content is not uploaded to GoreeCloud.

See [Architecture](docs/ARCHITECTURE.md), [Features](docs/FEATURES.md), [Objectives](docs/OBJECTIVES.md), [Roadmap](docs/ROADMAP.md) and [Security](SECURITY.md).

## Ecosystem obligations

The Android UI uses verified GC UI alpha semantic colors, not a released native GC UI SDK. GoreeShield privacy and Wardveil cybersecurity are assessed as separate boundaries. GoreeCloud Identity and Mesh connectivity are not installed; anonymous offline functionality deliberately has no central sign-in dependency. No Anchor/Stable/MIPS acceptance is claimed.

Copyright 2026 GoreeCloud. Third-party credits and licenses are retained separately.

# GoreeCloud GIF's

A native, open-source Android gallery for finding, saving, and sharing GIFs and stickers. The application code is maintained here; no GoreeCloud Teams account, hosted GoreeCloud server, proprietary search key, or tracking SDK is required.

**Status:** 0.1.0-alpha.1 development source. A public release, signed APK, production security review, and device acceptance are not claimed.

## Capabilities

- GIF search using the open [Openverse](https://openverse.org/) API, with mature results filtered, source/license credit shown, and explicit "Load more GIFs" pagination (20 per request for anonymous API access).
- Sticker search across the bundled, attributed [OpenMoji](https://openmoji.org/) catalog. Search names, tags, and categories without a network connection; image thumbnails and full-resolution artwork are fetched on demand.
- Native Compose discovery grid and preview sheet with light/dark GoreeCloud UI alpha token roles.
- Offline favorites: explicitly favorited GIFs and stickers are copied to app-private storage. Saved has its own search (including creator and license) and All / GIFs / Stickers filters, independent from Explore.
- Android share sheet: share a downloaded GIF (image/gif) or sticker (image/png) into another compatible app using FileProvider and temporary URI read permission.
- Android image picker interoperability: eligible apps using `ACTION_GET_CONTENT` can select GoreeCloud GIF's as a media source. The user deliberately selects a single compatible GIF/PNG; the app returns a temporary read-only `content://` URI.
- Phone download: write GIFs and PNGs to Pictures/GoreeCloud GIFs through Android MediaStore.
- Copy attribution: one tap in the detail sheet copies creator credit, source URL, and license text for appropriate downstream reuse.
- No account, analytics, tracking, ad SDK, contact permission, or broad storage permission.

## Install/build from source

Android Studio / Android SDK 37, JDK 21 and Gradle wrapper. Minimum Android 10 (API 29).

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

A separately triggered GitHub Actions workflow, **Android emulator smoke (manual)**, is included to exercise app startup, OpenMoji search, offline favorite storage and MediaStore export on an isolated Android 36 emulator. This is not a replacement for real physical-device validation and will be reported as unverified until a successful workflow run.

Find the development-signed debug APK at `app/build/outputs/apk/debug/app-debug.apk`. After a passing GitHub Actions Android run, the `goreecloud-gifs-debug-apk` run artifact contains this debug APK and a SHA-256 checksum file for 7 days. Download the artifact from GitHub Actions (signed-in access may be required) and verify the checksum before testing. Debug APKs are for local development, not an audited or production-signed release. Open the project in Android Studio to run on an emulator or authorized device. The app's display name is **GoreeCloud GIF's** and package is `com.goreecloud.gifs`.

## How to use

1. Browse **Explore**. Choose GIFs or Stickers and enter a search query. A blank GIF search shows reactions.
2. Tap an image for a larger preview, details, attribution and actions. Use **Copy creator and license credit** to save the source details before forwarding media.
3. Tap **heart** to save offline, **Share** to send media through Android's chooser, or **Download** to place the file in Pictures/GoreeCloud GIFs.
4. Open **Saved** to access offline favorites even if search is unavailable. Use the separate Saved search or the All / GIFs / Stickers chips to quickly find what you kept.
5. In apps that invoke Android's compatible `ACTION_GET_CONTENT` image picker, choose **GoreeCloud GIF's**, browse or search, and tap **Use in app**. This does not install a system keyboard or extend every modern photo picker.

Openverse search needs network access. Sticker *metadata search* is offline; first-time sticker image viewing, sharing and saving need network access. Sharing or downloading a previously favorited item is offline.

## Sources, licensing and limits

The first-party app source is **MIT** (see LICENSE). The bundled OpenMoji catalog metadata and remotely fetched OpenMoji art are **CC BY-SA 4.0**, not MIT; see `third_party/openmoji` and [OpenMoji](https://github.com/hfg-gmuend/openmoji). GIFs belong to their respective creators. Openverse returns per-item licenses and attribution; **not every GIF is free of reuse conditions**. Always respect creator attribution, license, share-alike and noncommercial terms, where applicable. The in-app preview shows source/credit.

Openverse is a public external service subject to availability and anonymous rate limits. Direct GIF URLs are hosted by third-party sources. GIFs exceeding 25 MiB, non-HTTPS links, local/reserved IP resolutions, download redirects and unexpected formats are rejected. Image previews use a separate HTTPS-only redirect policy with public DNS checks. Content is not uploaded to GoreeCloud.

See [Architecture](docs/ARCHITECTURE.md), [Features](docs/FEATURES.md), [Objectives](docs/OBJECTIVES.md), [Roadmap](docs/ROADMAP.md) and [Security](SECURITY.md).

## Ecosystem obligations

The Android UI uses verified GC UI alpha semantic colors, not a released native GC UI SDK. GoreeShield privacy and Wardveil cybersecurity are assessed as separate boundaries. GoreeCloud Identity and Mesh connectivity are not installed; anonymous offline functionality deliberately has no central sign-in dependency. No Anchor/Stable/MIPS acceptance is claimed.

Copyright 2026 GoreeCloud. Third-party credits and licenses are retained separately.

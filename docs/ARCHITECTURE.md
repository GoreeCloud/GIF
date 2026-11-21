# Android architecture and data flow

**Client:** Kotlin + Jetpack Compose; native Android single-activity interface. The UI state is owned by GalleryViewModel and flows from the search adapters and DataStore. There is currently no server.

**GIF search:** user-entered text -> anonymous HTTPS Openverse API query (extension=gif, mature=false, at most 20 per page; higher page sizes return HTTP 401) -> metadata filter (gif, non-mature, acceptable external HTTPS links) -> Coil thumbnails / detail -> explicit share/favorite/download acquisition. Subsequent pages load only when the user selects "Load more GIFs". Search errors persist visibly with a retry action. Third-party GIF hosts receive normal fetches for selected media; not a GoreeCloud relay.

**Sticker search:** local OpenMoji JSON from app assets (pinned upstream revision) -> local metadata match -> official upstream PNG URL at 72px for thumbnails / 618px for sharing. Full resolution is downloaded only on explicit actions. Names, tags, authors and licensing are in the bundled catalog.

**Storage:** DataStore preferences holds small favorite metadata; app-private files/favorites contains original image bytes. Coil public-media previews use a separate, app-private, OS-evictable disk cache capped at 64 MiB via `PreviewCachePolicy`; this cache never deletes explicitly saved favorites. The share cache uses cache/shares with a best-effort 100 MiB quota, oldest-first eviction of digest-named media, and a 15-minute grace period to avoid disrupting recent share URI grants. Explicit private favorites are never pruned. Content shares via an app-owned subclass of AndroidX FileProvider (non-exported), with temporary URI read grants only. The exported activity also handles specific `ACTION_GET_CONTENT` image requests; a user-selected GIF/PNG is returned as a `content://` URI via `setResult` with temporary read permission. Selection is restricted to the requesting MIME family and never grants folder-wide access. Download uses MediaStore pending media insertion in Pictures/GoreeCloud GIFs. No broad file storage permission is requested.

**Inbound Android shares:** a user selects GoreeCloud GIF's as the target for a single GIF or PNG via `ACTION_SEND`. The app checks the declared MIME family and a `content://` URI, requires in-app confirmation, then applies the same bounded, validated local import path. It does not request a persistent grant, accept untrusted network URLs or silently export data. Android activity recreation does not re-run the initial share.

**Personal media import:** the user explicitly selects a GIF or PNG from Android's `ACTION_OPEN_DOCUMENT` system picker -> the app reads only the granted `content://` URI -> streams up to 25 MiB into a temporary private file -> checks image magic bytes -> atomically renames under app-private `files/favorites` -> records the local file in DataStore. If the index write fails, the import is rolled back. No persistent document-provider grant or upload is used.

**Build toolchain:** Android Gradle Plugin 9.5.1 and Kotlin Compose plugin 2.4.21 passed CI and branch-level emulator testing against Gradle 9.7.1, then merged through PR #46. Release signing and physical-device acceptance remain separate gates.

**External app acceptance:** The test-only `qa/interop-fixture` APK has no network permission and uses a different Android UID. It grants read access to a generated PNG for `ACTION_SEND`; the main app requires in-app confirmation before import. For `ACTION_GET_CONTENT`, the fixture opens the returned GoreeCloud FileProvider URI through ContentResolver to verify temporary cross-UID access and PNG type. Only isolated Android emulators run this optional QA; actual external-client and physical-device acceptance remain open.

**CI reliability:** Android and manually triggered emulator workflows use GitHub's maintained Checkout v7 and Setup Java v6 actions on hosted runners. Checkout does not persist GitHub credentials because these jobs only read source. The official Java setup action's Gradle dependency cache is keyed by committed Gradle configuration/wrapper metadata. Caching is a performance aid, not a substitute for dependency version pinning, checksum integrity, build tests or release acceptance. SDK license input avoids `yes` SIGPIPE under Bash `pipefail`.

**Controls:** no HTTP cleartext, external URL validation plus a public-address-only DNS resolver, no redirects for explicit GIF/PNG acquisition, HTTPS-only redirects for Coil previews to public hosts, bounded 25 MiB downloads, magic byte checks (GIF/PNG), temporary files with atomic rename, no executable extensions, no analytics and no background network scanning.

**MIPS:** GC UI token values manually mapped from gc-ui (alpha Iris); GoreeShield maps to local-first data/explicit external fetches; Wardveil maps to input/media/transport restrictions; Mesh and Identity have no runtime dependencies. Full platform integrations and release-level verification are not yet complete.

## Privacy caveats

Openverse receives search phrases; external media providers can observe media fetches. Offline favorites remain local unless the user explicitly shares or exports media. Android share target apps apply their own privacy practices.

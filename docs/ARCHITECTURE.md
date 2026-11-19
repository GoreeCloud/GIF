# Android architecture and data flow

**Client:** Kotlin + Jetpack Compose; native Android single-activity interface. The UI state is owned by GalleryViewModel and flows from the search adapters and DataStore. There is currently no server.

**GIF search:** user-entered text -> anonymous HTTPS Openverse API query (extension=gif, mature=false, at most 20 per page; higher page sizes return HTTP 401) -> metadata filter (gif, non-mature, acceptable external HTTPS links) -> Coil thumbnails / detail -> explicit share/favorite/download acquisition. Subsequent pages load only when the user selects "Load more GIFs". Search errors persist visibly with a retry action. Third-party GIF hosts receive normal fetches for selected media; not a GoreeCloud relay.

**Sticker search:** local OpenMoji JSON from app assets (pinned upstream revision) -> local metadata match -> official upstream PNG URL at 72px for thumbnails / 618px for sharing. Full resolution is downloaded only on explicit actions. Names, tags, authors and licensing are in the bundled catalog.

**Storage:** DataStore preferences holds small favorite metadata; app-private files/favorites contains original image bytes. The share cache uses cache/shares; content shares via an app-owned subclass of AndroidX FileProvider (non-exported), with temporary URI read grants only. The exported activity also handles specific `ACTION_GET_CONTENT` image requests; a user-selected GIF/PNG is returned as a `content://` URI via `setResult` with temporary read permission. Selection is restricted to the requesting MIME family and never grants folder-wide access. Download uses MediaStore pending media insertion in Pictures/GoreeCloud GIFs. No broad file storage permission is requested.

**Controls:** no HTTP cleartext, external URL validation plus a public-address-only DNS resolver, no redirects for explicit GIF/PNG acquisition, HTTPS-only redirects for Coil previews to public hosts, bounded 25 MiB downloads, magic byte checks (GIF/PNG), temporary files with atomic rename, no executable extensions, no analytics and no background network scanning.

**MIPS:** GC UI token values manually mapped from gc-ui (alpha Iris); GoreeShield maps to local-first data/explicit external fetches; Wardveil maps to input/media/transport restrictions; Mesh and Identity have no runtime dependencies. Full platform integrations and release-level verification are not yet complete.

## Privacy caveats

Openverse receives search phrases; external media providers can observe media fetches. Offline favorites remain local unless the user explicitly shares or exports media. Android share target apps apply their own privacy practices.

# Features and evidence boundaries

| Feature | Source status | Acceptance remaining |
| --- | --- | --- |
| GIF search and pagination | Implemented via anonymous Openverse JSON adapter (20/page); status errors show retry | Device/network and load-more acceptance |
| Sticker search with local pagination | Pinned local OpenMoji metadata, multi-term filtering and bounded 60-item pages with Load more; no network requests for sticker search | Catalog coverage and large-result-set UI acceptance |
| GIF preview | Implemented with Coil GIF renderer | Real-device animation and memory testing |
| Local favorites and saved filters | Implemented with Android DataStore/private files and independent Saved search, type filters and Newest/Oldest/A–Z ordering | Restart/offline/low-storage and UI acceptance |
| User-selected local GIF/PNG imports and received shares | SAF OpenDocument picker or confirmed single-image ACTION_SEND share, strict MIME/URI validation, 25 MiB bound, GIF/PNG signature check and app-private copy; no persistable grant | Provider permissions, invalid files, recreation, storage and cross-app acceptance |
| Share to other apps | Implemented with FileProvider and ACTION_SEND | Target apps and URI grant tests |
| Pick GIF/PNG from requesting apps | Single-item Android ACTION_GET_CONTENT activity integration with MIME validation and temporary read grant | Caller integration/device tests |
| Temporary shared-media cache quota and validation | Best-effort 100 MiB oldest-first quota and 15-minute share grace; cached or saved bytes are validated against expected GIF/PNG signature and 25 MiB limit before reuse. Invalid cache entries are replaced; private favorites are never automatically deleted | Device storage and Android share-grant regression tests |
| Download to phone | Implemented with Android MediaStore scoped storage, visible snackbar after detail dismissal, and automatic MediaStore row checks in manual emulator acceptance | Android 10–current integration acceptance |
| Attribution/source and copyable credit | Implemented in detail sheet; formats creator, license and safe source URL for clipboard | Attribution usability and device review |
| Public DNS and HTTPS media hardening | Implemented in source with address validation, redirect separation and local unit tests | Real-network, DNS64 and device regression testing |
| Native appearance and media detail actions | Jetpack Compose, verified GC UI alpha colors; compact, scrollable detail sheet prioritizes Share/Download above preview | Small-screen/large-text emulator acceptance and accessibility audit |
| GIF trending / personalized recommendations | Not implemented | Product/relevance planning |
| Sticker pack installation into third-party keyboards | Not implemented | App-specific APIs / permissions |
| Cloud synchronization | Not implemented | Consent, encryption, identity and Mesh |
| Short-lived CI debug APK artifact | Implemented in workflow source; uploads only after tests/lint/build pass, with SHA-256 checksum | GitHub Actions artifact and installation verification |
| Manual Android emulator smoke workflow | Six-step Android 36 run passed on reviewed branches and protected-main commits `acd9671` and `ee3c095`, with host-side tests | Revalidation after later changes, direct pagination interaction, physical devices and external app interoperability |
| Stable signed APK | Not implemented | CI, release and device acceptance |

Passing source static checks alone does not establish platform acceptance.

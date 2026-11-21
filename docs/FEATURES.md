# Features and evidence boundaries

| Feature | Source status | Acceptance remaining |
| --- | --- | --- |
| Consent-conscious GIF search and pagination | Anonymous Openverse adapter (20/page) sends no query on launch or while typing; the user presses Search to submit. Includes two cancellable, bounded retries for transient HTTP 408/429/5xx responses and actionable error feedback | Real-network, rate-limit, disclosure, privacy and load-more acceptance |
| Sticker search with local pagination | Pinned local OpenMoji metadata, multi-term filtering and bounded 60-item pages with Load more and consistent empty-query pagination; no network requests for sticker search | Catalog coverage and large-result-set UI acceptance |
| GIF preview and bounded thumbnail cache | Coil animated GIF renderer with app-private, OS-evictable preview cache capped at 64 MiB, separate from permanent saved favorites | Real-device animation, memory and disk-pressure acceptance |
| Local favorites and saved filters | DataStore/private files, independent Saved search/filter/sort, fail-closed index parsing and rollback on index errors; removing any Saved item now requires confirmation and explains irreversible local imports | Confirm/cancel removal UI, corrupt-index, offline and physical-device acceptance |
| User-selected local GIF/PNG imports and received shares | SAF OpenDocument picker or confirmed single-image ACTION_SEND share, strict MIME/URI validation with multiple/conflicting share-source rejection, 25 MiB bound, GIF/PNG signature, dimensions and PNG IHDR CRC checks, and app-private copy; no persistable grant | Provider permissions, invalid files, recreation, storage and cross-app acceptance |
| Share to other apps | Implemented with FileProvider and ACTION_SEND | Target apps and URI grant tests |
| Pick GIF/PNG from requesting apps | Single-item Android ACTION_GET_CONTENT activity integration with MIME validation and temporary read grant | Caller integration/device tests |
| Temporary shared-media cache quota and validation | Best-effort 100 MiB oldest-first quota and 15-minute share grace; cached or saved bytes are checked for GIF/PNG signature, plausible dimensions, PNG IHDR integrity and 25 MiB bound before reuse. Invalid cache entries are replaced; private favorites are never automatically deleted | Device storage and Android share-grant regression tests |
| Download to phone | Scoped MediaStore export checks complete-byte copy and successful pending-row publication, cleans up failed exports, shows feedback after detail dismissal, and verifies MediaStore rows in emulator QA | Android 10–current and forced-provider-failure integration acceptance |
| Attribution/source and copyable credit | Implemented in detail sheet; formats creator, license and safe source URL for clipboard | Attribution usability and device review |
| Public DNS and HTTPS media hardening | Implemented in source with address validation, redirect separation and local unit tests | Real-network, DNS64 and device regression testing |
| Native appearance and media detail actions | Jetpack Compose, verified GC UI alpha colors; compact, scrollable detail sheet prioritizes Share/Download above preview | Small-screen/large-text emulator acceptance and accessibility audit |
| GIF trending / personalized recommendations | Not implemented | Product/relevance planning |
| Sticker pack installation into third-party keyboards | Not implemented | App-specific APIs / permissions |
| Cloud synchronization | Not implemented | Consent, encryption, identity and Mesh |
| Short-lived CI debug APK artifact | Implemented in workflow source; uploads only after tests/lint/build pass, with SHA-256 checksum | GitHub Actions artifact and installation verification |
| Manual Android emulator smoke workflow | Six-step Android 36 workflow has passed on reviewed branches and protected-main revisions; eight-step extended mode passed on corrected branch run #44658719227 (offline Saved restart and native Load more stickers) | Repeat extended run on protected main and separately validate physical devices and third-party interoperability |
| Stable signed APK | Not implemented | CI, release and device acceptance |

Passing source static checks alone does not establish platform acceptance.

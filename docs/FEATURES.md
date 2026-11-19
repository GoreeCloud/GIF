# Features and evidence boundaries

| Feature | Source status | Acceptance remaining |
| --- | --- | --- |
| GIF search | Implemented via Openverse JSON adapter | Device/network and offline-error testing |
| Sticker search | Implemented using pinned local OpenMoji metadata | Catalog coverage and search acceptance |
| GIF preview | Implemented with Coil GIF renderer | Real-device animation and memory testing |
| Local favorites | Implemented with Android DataStore and private files | Restart/offline/low-storage tests |
| Share to other apps | Implemented with FileProvider and ACTION_SEND | Target apps and URI grant tests |
| Download to phone | Implemented with Android MediaStore scoped storage | Android 10–current integration tests |
| Attribution/source | Implemented in detail sheet | Format/readability review |
| Native appearance | Jetpack Compose, approved GC UI alpha colors | Visual and accessibility audit |
| GIF trending / personalized recommendations | Not implemented | Product/relevance planning |
| Sticker pack installation into third-party keyboards | Not implemented | App-specific APIs / permissions |
| Cloud synchronization | Not implemented | Consent, encryption, identity and Mesh |
| Stable signed APK | Not implemented | CI, release and device acceptance |

Passing source static checks alone does not establish platform acceptance.

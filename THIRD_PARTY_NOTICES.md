# Third-party notices

The application code in this repository is distributed under MIT. Third-party components have their **own** terms; this document does not relicense them.

| Component | Usage | License / source |
| --- | --- | --- |
| AndroidX Compose, Activity, Lifecycle, DataStore, Core and Jetpack libraries | Native UI, state, data storage and FileProvider | Apache 2.0; https://developer.android.com/jetpack/androidx |
| Coil 3 / Coil GIF | Animated GIF and image loading | Apache 2.0; https://github.com/coil-kt/coil |
| OkHttp | HTTPS client | Apache 2.0; https://github.com/square/okhttp |
| Kotlin and kotlinx.coroutines | Android language/coroutine implementation | Apache 2.0; https://github.com/JetBrains/kotlin and https://github.com/Kotlin/kotlinx.coroutines |
| Gradle Wrapper | Reproducible build launcher | Apache 2.0; https://github.com/gradle/gradle |
| OpenMoji metadata and downloadable artwork | Sticker catalog and assets | **CC BY-SA 4.0**; https://github.com/hfg-gmuend/openmoji; upstream license in `third_party/openmoji/LICENSE.txt` |
| Openverse API | Search for creator-licensed external GIFs | https://api.openverse.org/v1/ ; each result has its **own** media license/attribution |

**OpenMoji snapshot:** upstream revision `3e01de523ddefe9c5e46a7765b15efef04bf9002`. SHA-256 of bundled `app/src/main/assets/openmoji.json`: `a67914d77462ea8bfce50be1a521fd9b9d57e2b74021aeea66b4ed416875c9d9`.

The OpenMoji catalog is included and attributed, but the binary sticker images are retrieved from the pinned upstream commit when requested. The original artists' rights are preserved.

The app does not assert rights over GIFs indexed by Openverse. Users should consult the source and comply with its license. Optional source attribution may be required when forwarding files to another app.

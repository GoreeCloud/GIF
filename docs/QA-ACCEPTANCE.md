# GoreeCloud GIF's — Android acceptance evidence

**Status:** The six-step Android 36 cloud-emulator acceptance **passed on multiple protected-main revisions**, most recently run #44652980425 at commit `ee3c095` (after the share-cache quota and sticker-pagination changes). The later saved-file path hardening passed Android CI, but its emulator regression remains a separate gate. No physical-phone installation, Stable release, or production deployment is asserted.

## CI and acceptance scope

- Required `Android` workflow: Android SDK 37 compile, unit tests, Android lint, debug APK, SHA-256 verification and seven-day artifact.
- Manual `Android emulator smoke (manual)` workflow: isolated Android 36 x86_64 emulator with shell-only ADB. Default tests six core steps. The optional **extended** workflow input additionally verifies private favorite persistence after force-stop/relaunch with Wi-Fi and mobile data disabled and native sticker pagination from 60 to 120 items. Eight-step extended acceptance passed on corrected feature-branch run #44658719227; protected-main repetition is still pending. The test never targets a connected physical phone.
- Host-side Python tests run on every `Android` CI check to exercise the accessibility-UI retry and coordinate logic without booting a device.
- Passing unit tests or a booted emulator **does not** prove actual on-device downloads, animated GIF behavior, other-app integration, or accessibility compliance.

## Independent cloud-emulator runs

| Run | Observed result | Follow-up |
| --- | --- | --- |
| [44637310659](https://github.com/GoreeCloud/GIF/actions/runs/44637310659) | SDK setup failed due to `yes` SIGPIPE with Bash `pipefail`, before emulator launch | Changed to process substitution in PR #16 |
| [44638020820](https://github.com/GoreeCloud/GIF/actions/runs/44638020820) | SDK setup and app build passed; emulator creation failed because script assumed a nonportable AVD directory | Set explicit runner-temp AVD path in PR #17 |
| [44639069676](https://github.com/GoreeCloud/GIF/actions/runs/44639069676) | SDK, build and unit tests passed; emulator booted; hierarchy dump under `/sdcard` unavailable at first UI probe | Retry UI hierarchy acquisition and use shell temporary directory in PR #19 |
| [44640291641](https://github.com/GoreeCloud/GIF/actions/runs/44640291641) | SDK/build, emulator boot, app launch, OpenMoji search and a sticker saved to app-private storage passed (milestones 1–3/6); no Download action found after opening saved item | Capture UI labels, screenshot and hierarchy to distinguish a harness tap problem from a real app-detail bug |
| [44641797977](https://github.com/GoreeCloud/GIF/actions/runs/44641797977) | Android 36 boot, app install, OpenMoji search and app-private sticker save passed (1–3/6); diagnostics show the media-detail bottom sheet was visible but its Download button was pushed below the viewport by the large preview | Reorder the sheet to prioritize Share/Download, cap the preview size and enable vertical scrolling; rerun real emulator acceptance |
| [44644855764](https://github.com/GoreeCloud/GIF/actions/runs/44644855764) | App boot, sticker catalog search, offline favorite and now-visible detail sheet Share/Download passed milestones 1–4/6 on protected main. The screenshot confirms Download is visible; milestone 5 could not observe a snackbar because it was displayed behind the modal sheet | Dismiss modal on success/error and check the actual MediaStore PNG row in the revised smoke harness |
| [44646285317](https://github.com/GoreeCloud/GIF/actions/runs/44646285317) | **SUCCESS** on reviewed branch commit `2cc9fe0`: all six milestones logged, including app launch, sticker discovery, private offline favorite, reachable media sheet, a PNG MediaStore row and source/attribution action | Confirm the same workflow after merge and complete physical/third-party acceptance |
| [44647133548](https://github.com/GoreeCloud/GIF/actions/runs/44647133548) | Protected-main rerun failed **before app startup**: emulator UI displayed `System UI isn't responding` during startup | Rerun without conflating this system-component ANR with an app regression; inspect runner resources if repeated |
| [44648142401](https://github.com/GoreeCloud/GIF/actions/runs/44648142401) | **SUCCESS on protected main**, commit `acd9671`: all six milestones logged, including real private sticker favorite and MediaStore PNG row verification | Keep regression checks and repeat after later application/storage changes |
| [44650846467](https://github.com/GoreeCloud/GIF/actions/runs/44650846467) | **SUCCESS on media-revalidation feature branch**, commit `6cf51b6`; the full existing six-step workflow passed | New cached-media rejection paths were unit-tested but malformed-file behavior is not separately exercised by the emulator |
| [44650841153](https://github.com/GoreeCloud/GIF/actions/runs/44650841153) | Sticker-pagination branch emulator job failed **during Gradle dependency resolution**, before launch | Do not classify as a UI regression; repeat after merge |
| [44652980425](https://github.com/GoreeCloud/GIF/actions/runs/44652980425) | **SUCCESS on protected main**, commit `ee3c095`: all six milestones including sticker save, private storage, MediaStore PNG export and attribution | Includes merged sticker pagination and cache quota; the actual 'Load more stickers' interaction is not covered by this script |
| [44656284380](https://github.com/GoreeCloud/GIF/actions/runs/44656284380) | Optional extended run passed six core milestones and **7/8 offline restart**, but Load more did not increase the sticker count | Improve scrolling diagnostics and investigate app pagination query |
| [44657499456](https://github.com/GoreeCloud/GIF/actions/runs/44657499456) | Optional extended rerun passed **7/8** and found a visible Load more text node at [411,1801][742,1856], but still showed 60 items | Verified source bug: an empty sticker search is initially unfiltered but active query was normalized to the GIF default `reaction` for page two; fix shared query normalization and rerun |
| [44658719227](https://github.com/GoreeCloud/GIF/actions/runs/44658719227) | **SUCCESS on corrected feature branch**, commit `4cac95b`: all eight milestones passed, including private sticker persistence after force-stop with Wi-Fi and mobile data disabled and native sticker pagination increasing from 60 to 120 items | Repeat extended regression on protected main after merge; this is not third-party or physical-device acceptance |

These results establish a complete branch-level and protected-main emulator pass at the specified revisions, **not** acceptance on physical devices. The PNG MediaStore row check proves a published entry, not byte-for-byte image identity.

## Exit criteria

1. **Verified through protected-main commit `ee3c095`:** the manual workflow completed all six smoke milestones; repeat after later changes and extend it to pagination, imports and third-party picker/share journeys.
2. The Saved sticker and downloaded PNG remain usable without network connectivity, with local-file and MediaStore evidence.
3. Native share-sheet and `ACTION_GET_CONTENT` consumers receive MIME-correct read-only URIs on representative Android versions.
4. Animated GIF preview, accessibility, resource use, storage limits and attributions receive human/device review.
5. No production signing or release label is applied until release-specific gates are established.

Further physical-device and third-party acceptance is tracked in [issue #26](https://github.com/GoreeCloud/GIF/issues/26). Keep run IDs, evidence, and unresolved blockers in sync with actual provider state.

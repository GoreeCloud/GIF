# GoreeCloud GIF's — Android acceptance evidence

**Status:** Development builds pass Android unit/lint/assemble checks; first-party Android emulator **end-to-end acceptance remains incomplete**. No physical-phone installation, stable release, or production deployment is asserted.

## CI and acceptance scope

- Required `Android` workflow: Android SDK 37 compile, unit tests, Android lint, debug APK, SHA-256 verification and seven-day artifact.
- Manual `Android emulator smoke (manual)` workflow: isolated Android 36 x86_64 emulator with shell-only ADB. The test does not target the connected physical phone.
- Host-side Python tests run on every `Android` CI check to exercise the accessibility-UI retry and coordinate logic without booting a device.
- Passing unit tests or a booted emulator **does not** prove actual on-device downloads, animated GIF behavior, other-app integration, or accessibility compliance.

## Initial independent cloud-emulator runs

| Run | Observed result | Follow-up |
| --- | --- | --- |
| [44637310659](https://github.com/GoreeCloud/GIF/actions/runs/44637310659) | SDK setup failed due to `yes` SIGPIPE with Bash `pipefail`, before emulator launch | Changed to process substitution in PR #16 |
| [44638020820](https://github.com/GoreeCloud/GIF/actions/runs/44638020820) | SDK setup and app build passed; emulator creation failed because script assumed a nonportable AVD directory | Set explicit runner-temp AVD path in PR #17 |
| [44639069676](https://github.com/GoreeCloud/GIF/actions/runs/44639069676) | SDK, build and unit tests passed; emulator booted; hierarchy dump under `/sdcard` unavailable at first UI probe | Retry UI hierarchy acquisition and use shell temporary directory in PR #19 |
| [44640291641](https://github.com/GoreeCloud/GIF/actions/runs/44640291641) | SDK/build, emulator boot, app launch, OpenMoji search and a sticker saved to app-private storage passed (milestones 1–3/6); no Download action found after opening saved item | Capture UI labels, screenshot and hierarchy to distinguish a harness tap problem from a real app-detail bug |

| [44641797977](https://github.com/GoreeCloud/GIF/actions/runs/44641797977) | Android 36 boot, app install, OpenMoji search and app-private sticker save passed (1–3/6); diagnostics show the media-detail bottom sheet was visible but its Download button was pushed below the viewport by the large preview | Reorder the sheet to prioritize Share/Download, cap the preview size and enable vertical scrolling; rerun real emulator acceptance |

These results show increasing verification coverage but **not** a successful end-to-end smoke run.

## Exit criteria

1. A manual workflow run finishes successfully on protected `main` and logs all six smoke milestones.
2. The Saved sticker and downloaded PNG remain usable without network connectivity, with local-file and MediaStore evidence.
3. Native share-sheet and `ACTION_GET_CONTENT` consumers receive MIME-correct read-only URIs on representative Android versions.
4. Animated GIF preview, accessibility, resource use, storage limits and attributions receive human/device review.
5. No production signing or release label is applied until release-specific gates are established.

Keep run IDs, evidence, and unresolved blockers in sync with actual provider state.

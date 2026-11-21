# Android cross-app interoperability fixture (QA only)

This tiny standalone Android application tests GoreeCloud GIF's from a **different Android package and UID**, rather than treating a self-issued URI as third-party proof.

It exposes one intentionally grant-scoped, generated 32x32 PNG through a non-exported Android ContentProvider and exercises:

- Explicit `ACTION_SEND` of `image/png` with a temporary URI grant into GoreeCloud's confirmation-based import flow.
- Explicit `ACTION_GET_CONTENT` requesting `image/png` from GoreeCloud, followed by a real cross-UID `ContentResolver.openInputStream` read of the returned FileProvider URI and inspection of the PNG signature and MIME type.

No network permission, analytics, secrets, contacts, broad storage access, or data upload is used. The fixture stores its own result under `files/interop-result.txt`; the isolated emulator harness reads it with `run-as`. The fixture is never bundled in the consumer app, and its build is not a signed public release.

Run an isolated Android 36 emulator smoke via the manually dispatched GitHub workflow using `extended=true`, `verify_removal=true`, and `verify_interop=true`. A passing emulator journey is **not** physical-device acceptance, proof of interoperability with all commercial messaging apps, or permission to publish a Stable release.

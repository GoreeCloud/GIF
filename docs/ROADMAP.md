# Roadmap — GoreeCloud GIF's

## Implemented in development source
- Native Android Jetpack Compose app and local app-only navigation.
- Explicit-submit Openverse GIF search (no query sent on launch or while typing), with licensing attribution.
- Searchable OpenMoji catalog indexed from a pinned, bundled data snapshot.
- Save favorites offline, explicitly import local GIF/PNG or receive shared media into private favorites, share MIME-correct content URIs, export to MediaStore, and respond to single-image `ACTION_GET_CONTENT` pickers.
- Initial automated tests, build workflows and security bounds.

## Next development priorities (not implemented)
1. Nine-step emulator acceptance passed on Android APIs 29, 33, 35 and 36; repeat against protected main after the selector merges. Add third-party share/picker/import acceptance and independently verify physical-device behavior and downloaded file integrity.
2. Validate bounded retry/backoff for anonymous Openverse search, and improve Coil thumbnail disk-cache limits and relevance; temporary share-cache quota and GIF/sticker pagination are implemented in development source.
3. Verify clipboard attribution behavior on supported devices; improve content rights guidance and media-specific license notices.
4. Extend Saved type filters and safe deletion confirmations with sticker packs, collections, search refinements, encrypted backup/export and media organization.
5. Audit inclusive design, screen reader controls, RTL, tablet layouts and automated image UI tests.
6. Add optional self-hosted source adapters behind user consent and explicit endpoint trust.
7. Assess GoreeCloud UI native bridge, GoreeShield, Wardveil, Identity and Mesh integration requirements before any Anchor designation.
8. Validate source licenses and SBOM; establish signed, reproducible release process.

No publication deadline or Anchor release is committed.

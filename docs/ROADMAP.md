# Roadmap — GoreeCloud GIF's

## Implemented in development source
- Native Android Jetpack Compose app and local app-only navigation.
- Explicit-submit Openverse GIF search (no query sent on launch or while typing), with licensing attribution.
- Searchable OpenMoji catalog indexed from a pinned, bundled data snapshot.
- Save favorites offline, explicitly import local GIF/PNG or receive shared media into private favorites, share MIME-correct content URIs, export to MediaStore, and respond to single-image `ACTION_GET_CONTENT` pickers.
- Initial automated tests, build workflows and security bounds.

## Next development priorities (not implemented)
1. Nine-step emulator acceptance passed independently on Android APIs 29, 33, 35 and 36; repeat after subsequent merges and test external-app share/picker, authorized physical-device behavior and byte-for-byte downloaded file integrity.
2. Validate real-network retry/backoff and improve search relevance. A bounded 64 MiB app-private Coil preview disk cache, temporary share-cache quota and GIF/sticker pagination are implemented in development source; validate disk pressure and eviction on devices.
3. Verify clipboard attribution and contextual media-rights notices on actual devices; obtain human legal/licensing review for any reuse guidance before release.
4. Extend Saved type filters and safe deletion confirmations with sticker packs, collections, search refinements, encrypted backup/export and media organization.
5. Audit inclusive design, screen reader controls, RTL, tablet layouts and automated image UI tests.
6. Add optional self-hosted source adapters behind user consent and explicit endpoint trust.
7. Assess GoreeCloud UI native bridge, GoreeShield, Wardveil, Identity and Mesh integration requirements before any Anchor designation.
8. Validate source licenses and SBOM; establish signed, reproducible release process.

No publication deadline or Anchor release is committed.

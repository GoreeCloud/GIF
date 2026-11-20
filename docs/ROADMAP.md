# Roadmap — GoreeCloud GIF's

## Implemented in development source
- Native Android Jetpack Compose app and local app-only navigation.
- Openverse GIF search with licensing attribution.
- Searchable OpenMoji catalog indexed from a pinned, bundled data snapshot.
- Save favorites offline, explicitly import local GIF/PNG or receive shared media into private favorites, share MIME-correct content URIs, export to MediaStore, and respond to single-image `ACTION_GET_CONTENT` pickers.
- Initial automated tests, build workflows and security bounds.

## Next development priorities (not implemented)
1. Repeat the now-passing eight-step optional emulator acceptance on protected main; add third-party share/picker/import acceptance and independently verify physical-device behavior and downloaded file integrity.
2. Improve network retry/backoff, Coil thumbnail disk-cache limits and search relevance; temporary share-cache quota and GIF/sticker pagination are implemented in development source.
3. Verify clipboard attribution behavior on supported devices; improve content rights guidance and media-specific license notices.
4. Extend basic Saved type filters with sticker packs, collections, search refinements, batch backup/export and media organization.
5. Audit inclusive design, screen reader controls, RTL, tablet layouts and automated image UI tests.
6. Add optional self-hosted source adapters behind user consent and explicit endpoint trust.
7. Assess GoreeCloud UI native bridge, GoreeShield, Wardveil, Identity and Mesh integration requirements before any Anchor designation.
8. Validate source licenses and SBOM; establish signed, reproducible release process.

No publication deadline or Anchor release is committed.

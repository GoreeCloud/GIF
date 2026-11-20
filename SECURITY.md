# Security policy

This project is a development alpha, not a production-hardened service. Please report suspected security issues privately to the repository owner via GitHub's private vulnerability reporting where enabled; do not publish secrets, exploit payloads or sensitive device data in public issues.

## Current guardrails
- No account access, permissions to contacts or files, telemetry SDK, background service or embedded browser.
- HTTPS-only external requests with DNS answers screened for loopback, private, link-local and reserved destinations. Downloads reject redirects; preview requests permit only same-scheme HTTPS redirects to publicly resolving hosts.
- Refuse loopback/private/link-local URL targets, non-GIF/PNG payloads and downloads larger than 25 MiB.
- Local imports use Android's explicit system document picker or a single-image Android Share target with an additional in-app confirmation; imported shares restrict GIF/PNG MIME declarations and `content://` provenance. Both paths validate `content://` provenance and GIF/PNG magic bytes, enforce the 25 MiB limit, copy only to app-private files and roll back on DataStore failure. No broad storage access or persistent document-provider grant is requested.
- Media revalidation before sharing/exporting: app-private favorites that fail GIF/PNG signature or size validation produce an actionable error and are never silently deleted. Invalid temporary cache is replaced; only files in cache/shares are eligible for quota eviction.
- Persisted local favorite filenames must exactly match the SHA-256-derived identifier and GIF/PNG extension expected for that media record; malformed paths cannot direct previews outside the private favorites directory.
- App-private favorites; explicitly permissioned temporary Android content URI sharing. `ACTION_GET_CONTENT` responses require an explicit item choice, restrict GIF/PNG MIME types and grant only temporary read access to the selected item.
- Gradle and dependency versions pinned; source review and CI remain ongoing.

## Open risks
- Third-party media can change or vanish; trust and content moderation is not exhaustive.
- Openverse anonymous rate limits and availability vary.
- DNS rebinding and providers' preview redirect behavior merit additional controls and testing.
- Memory/battery impacts of animated media and malicious crafted images need device tests.
- No penetration test, supply-chain SBOM acceptance, signed release or MIPS release clearance.

Do not install development APKs from untrusted sources.

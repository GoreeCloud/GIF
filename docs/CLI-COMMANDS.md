# GoreeCloud GIF's — CLI command reference

**Status:** Verified command patterns for development and CI inspection, not evidence of a Stable release.
**Repository:** [GoreeCloud/GIF](https://github.com/GoreeCloud/GIF)
**Authorized development host:** `personal-laptop-ideapad3-01` (Linux)
**Canonical Google Drive destination:** `GoreeCloud/CLI Commands/GoreeCloud/GIF.md`

This repository copy is an interim source reference. Synchronization into the specified Google Drive directory must be verified separately; do not claim that a GitHub file is a Drive copy. Never store personal access tokens, passwords, raw authentication responses, or private content in command examples.

## GitHub identity, default branch and protections

Run from an authenticated GitHub CLI session with access to `GoreeCloud/GIF`. The examples below return metadata, not token values.

```bash
gh auth status
gh api repos/GoreeCloud/GIF/branches/main --jq '{sha:.commit.sha,protected:.protected}'
gh api repos/GoreeCloud/GIF/branches/main/protection --jq '{admin:.enforce_admins.enabled,strict:.required_status_checks.strict,checks:.required_status_checks.contexts,force:.allow_force_pushes.enabled,delete:.allow_deletions.enabled}'
gh pr list -R GoreeCloud/GIF --state open --limit 30
gh issue list -R GoreeCloud/GIF --state open --limit 30
```

## Safe pull-request development

These are *templates*, not commands to merge or modify an unknown branch automatically. Confirm the upstream remote, current branch and changed-file diff first. Stage only reviewed paths.

```bash
git fetch origin main
git status -sb
git -c core.whitespace=cr-at-eol diff --check
git diff --stat
git add path/to/reviewed/file
git diff --cached --check
git commit -m 'type: concise and verified description'
git push -u origin feature/verified-branch
gh pr create -R GoreeCloud/GIF --base main --head feature/verified-branch
gh pr checks PR_NUMBER -R GoreeCloud/GIF
gh pr view PR_NUMBER -R GoreeCloud/GIF --json state,mergeable,statusCheckRollup
```

Only after required CI passes, mergeability is confirmed and the proposed change is authorized:

```bash
gh pr merge PR_NUMBER -R GoreeCloud/GIF --squash
gh api repos/GoreeCloud/GIF/branches/main --jq '{sha:.commit.sha,protected:.protected}'
```

Never bypass branch protections, force-push `main`, or treat a successful PR check as runtime/deployment acceptance.

## Android build and unit/lint checks

Run only against reviewed Gradle build scripts and verified wrapper configuration. The installed build environment requires JDK 21 and the Android SDK components recorded in the repository workflow.

```bash
./gradlew --no-daemon --console=plain :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

The APK is **debug-signed for development, not production-signed**, and is not Stable. Standalone APK delivery to `GoreeCloud/Artifacts/` on Google Drive requires separate verified upload, provenance and signing checks.

## CI and isolated Android 36 emulator

The manual emulator workflow explicitly targets an isolated `emulator-*` serial, not a connected physical phone.

```bash
gh run list -R GoreeCloud/GIF --workflow Android --branch main --limit 5
gh run list -R GoreeCloud/GIF --workflow emulator-smoke.yml --limit 5
gh workflow run emulator-smoke.yml -R GoreeCloud/GIF --ref main -f extended=true
# Optional external Android package acceptance: use a reviewed QA branch first.
# gh workflow run emulator-smoke.yml -R GoreeCloud/GIF --ref <reviewed-branch> -f extended=true -f verify_removal=true -f verify_interop=true
gh run view RUN_ID -R GoreeCloud/GIF --json status,conclusion,headSha,headBranch
gh run view RUN_ID -R GoreeCloud/GIF --log-failed
adb devices -l
```

An eight-step emulator pass covers sticker discovery, offline favorites, MediaStore export, source attribution, offline app restart and sticker pagination. It does **not** establish physical-phone interoperability, secure signing, third-party share-target acceptance or full GIF animation correctness.

## Preservation and governance

Keep commands in the required Google Drive canonical directory, with this GitHub file as a version-controlled reference. Avoid running unreviewed scripts or upgrading dependencies whose source and CI results are not verified. Use least-privilege accounts; never paste tokens into command arguments or log files.

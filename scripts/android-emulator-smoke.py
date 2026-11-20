#!/usr/bin/env python3
"""Manual-only Android 36 emulator acceptance: local stickers, favorite, download.

All operations target a named emulator serial. Never touch a physical device.
No third-party Python dependencies or private account credentials.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.goreecloud.gifs"
BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def adb(serial, *args, timeout=30):
    result = subprocess.run(
        ["adb", "-s", serial, *map(str, args)],
        text=True, capture_output=True, timeout=timeout, check=False,
    )
    if result.returncode:
        raise RuntimeError(f"adb {' '.join(map(str, args))} failed: {result.stderr[:400]}")
    return result.stdout


def wait_boot(serial, deadline=180):
    end = time.monotonic() + deadline
    while time.monotonic() < end:
        try:
            if adb(serial, "shell", "getprop", "sys.boot_completed", timeout=10).strip() == "1":
                return
        except (RuntimeError, subprocess.TimeoutExpired):
            pass
        time.sleep(3)
    raise TimeoutError("Android emulator never finished booting")


UI_DUMP = "/data/local/tmp/goreecloud-gif-ui.xml"


def nodes(serial):
    # /sdcard can be unavailable briefly after boot; the shell-owned temporary
    # directory remains accessible. Some Android versions exit 0 without
    # writing a dump while the accessibility service is still starting.
    output = adb(serial, "shell", "uiautomator", "dump", UI_DUMP, timeout=35)
    try:
        xml = adb(serial, "shell", "cat", UI_DUMP, timeout=12)
    except RuntimeError as exc:
        raise RuntimeError(f"UI hierarchy not ready ({output.strip()}): {exc}") from exc
    try:
        return list(ET.fromstring(xml).iter("node"))
    except ET.ParseError as exc:
        raise RuntimeError(f"UI hierarchy incomplete ({output.strip()}): {exc}") from exc


def find_node(serial, phrase, exact=False, deadline=45):
    end = time.monotonic() + deadline
    last_error = None
    last_labels = []
    while time.monotonic() < end:
        try:
            current = nodes(serial)
            last_labels = []
            for node in current:
                name = (node.get("text", "") + " " + node.get("content-desc", "")).strip()
                if name and name not in last_labels:
                    last_labels.append(name[:70])
                if (phrase == name if exact else phrase.lower() in name.lower()):
                    return node
            last_error = "UI hierarchy did not include the requested element"
        except (RuntimeError, ET.ParseError, subprocess.TimeoutExpired) as exc:
            last_error = str(exc)
        time.sleep(2)
    raise AssertionError(
        f"Android UI did not expose {phrase!r}: {last_error}. "
        f"Last UI labels: {last_labels[-25:]!r}"
    )


def capture_failure(serial):
    """Collect QA-only evidence from the isolated emulator, never the physical phone."""
    if not serial.startswith("emulator-"):
        return
    folder = Path(os.environ.get("RUNNER_TEMP", "/tmp"))
    screenshot = folder / "gif-smoke-failure.png"
    hierarchy = folder / "gif-smoke-hierarchy.xml"
    try:
        with screenshot.open("wb") as file:
            result = subprocess.run(
                ["adb", "-s", serial, "exec-out", "screencap", "-p"],
                stdout=file, stderr=subprocess.PIPE, timeout=20, check=False,
            )
        if result.returncode:
            screenshot.unlink(missing_ok=True)
        else:
            print(f"QA screenshot saved: {screenshot}", flush=True)
    except (OSError, subprocess.TimeoutExpired) as exc:
        print(f"QA screenshot unavailable: {exc}", flush=True)
    try:
        xml = adb(serial, "shell", "cat", UI_DUMP, timeout=15)
        if xml.startswith("<?xml") or xml.startswith("<hierarchy"):
            hierarchy.write_text(xml[:1_000_000], encoding="utf-8")
            print(f"QA hierarchy saved: {hierarchy}", flush=True)
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as exc:
        print(f"QA hierarchy unavailable: {exc}", flush=True)


def tap(serial, node):
    bounds = BOUNDS.fullmatch(node.get("bounds", ""))
    if bounds is None:
        raise AssertionError("UI node has no bounds")
    left, top, right, bottom = map(int, bounds.groups())
    adb(serial, "shell", "input", "tap", (left + right) // 2, (top + bottom) // 2)


def require_file(serial):
    # Android debug app run-as is permitted; this does not query private user data.
    listing = adb(serial, "shell", "run-as", PACKAGE, "ls", "files/favorites")
    if ".png" not in listing:
        raise AssertionError(f"No offline favorite PNG in app-private files: {listing[:200]}")


def find_node_with_scrolling(serial, phrase, max_swipes=45):
    """Locate a LazyGrid footer using on-screen swipes; bound work on slow VMs."""
    width_height = adb(serial, "shell", "wm", "size", timeout=10)
    sizes = re.findall(r"(\d+)x(\d+)", width_height)
    width, height = map(int, sizes[-1]) if sizes else (1080, 1920)
    start_y = int(height * 0.83)
    end_y = int(height * 0.39)
    x = width // 2
    for step in range(max_swipes + 1):
        try:
            for node in nodes(serial):
                label = (node.get("text", "") + " " + node.get("content-desc", "")).strip()
                if phrase.lower() in label.lower():
                    return node
        except (RuntimeError, subprocess.TimeoutExpired, ET.ParseError):
            pass
        if step < max_swipes:
            adb(serial, "shell", "input", "swipe", x, start_y, x, end_y, 90)
            time.sleep(0.15)
    raise AssertionError(f"Could not scroll to {phrase!r} within {max_swipes} swipes")


def verify_offline_restart(serial):
    """Save was already confirmed. Reopen while offline and verify persistence."""
    before = adb(serial, "shell", "run-as", PACKAGE, "ls", "files/favorites")
    adb(serial, "shell", "am", "force-stop", PACKAGE)
    try:
        adb(serial, "shell", "svc", "wifi", "disable")
        adb(serial, "shell", "svc", "data", "disable")
        adb(serial, "shell", "am", "start", "-n", PACKAGE + "/.ui.MainActivity")
        find_node(serial, "Saved (1)", deadline=70)
        tap(serial, find_node(serial, "Saved (1)", exact=True, deadline=25))
        find_node(serial, "Offline favorites", deadline=30)
        find_node(serial, "More options for", deadline=30)
        after = adb(serial, "shell", "run-as", PACKAGE, "ls", "files/favorites")
        if before != after or not after.strip():
            raise AssertionError("Offline saved file changed across restart")
    finally:
        # Return the isolated emulator to its previous default connectivity.
        adb(serial, "shell", "svc", "wifi", "enable")
        adb(serial, "shell", "svc", "data", "enable")


def verify_sticker_pagination(serial):
    """Check the actual native Load more control, not only pagination unit tests."""
    tap(serial, find_node(serial, "Explore", exact=True, deadline=30))
    tap(serial, find_node(serial, "Stickers", exact=True, deadline=30))
    find_node(serial, "60 items", deadline=70)
    tap(serial, find_node_with_scrolling(serial, "Load more stickers"))
    find_node(serial, "120 items", deadline=65)


def require_mediastore_download(serial):
    """Check the actual Android MediaStore row, not just a transient UI toast."""
    listing = adb(
        serial, "shell", "content", "query",
        "--uri", "content://media/external/images/media",
        "--projection", "_display_name:mime_type:relative_path",
        timeout=30,
    )
    rows = listing.splitlines()
    accepted = [
        row for row in rows
        if "relative_path=Pictures/GoreeCloud GIFs/" in row
        and "mime_type=image/png" in row
        and ".png" in row
    ]
    if not accepted:
        raise AssertionError(
            "Android MediaStore has no exported GIFs gallery PNG: " + listing[:500]
        )
    return accepted[0]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="emulator-5554")
    parser.add_argument("--apk", required=True)
    parser.add_argument(
        "--extended", action="store_true",
        help="Also verify offline favorite restart and native sticker paging"
    )
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        parser.error("Only an emulator serial is allowed")
    wait_boot(args.serial)
    print("1/6 Emulator booted", flush=True)
    adb(args.serial, "install", "-r", args.apk, timeout=160)
    adb(args.serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb(args.serial, "shell", "wm", "dismiss-keyguard")
    adb(args.serial, "shell", "am", "start", "-n", PACKAGE + "/.ui.MainActivity")
    find_node(args.serial, "GoreeCloud GIF's", deadline=75)
    print("2/6 App installed and launched", flush=True)
    tap(args.serial, find_node(args.serial, "Stickers", exact=True))
    find_node(args.serial, "OpenMoji stickers", deadline=75)
    tap(args.serial, find_node(args.serial, "Save favorite", deadline=50))
    find_node(args.serial, "Saved (1)", deadline=65)
    require_file(args.serial)
    print("3/6 Sticker saved to app-private offline storage", flush=True)
    tap(args.serial, find_node(args.serial, "Saved (1)", exact=True))
    find_node(args.serial, "Offline favorites", deadline=30)
    tap(args.serial, find_node(args.serial, "More options for", deadline=30))
    # Wait for the Compose bottom sheet rather than assuming it opens instantly.
    find_node(args.serial, "Download", deadline=45)
    print("4/6 Offline favorites and media detail verified", flush=True)
    tap(args.serial, find_node(args.serial, "Download", exact=True))
    find_node(args.serial, "Saved to Pictures", deadline=60)
    require_mediastore_download(args.serial)
    print("5/6 MediaStore phone download confirmed", flush=True)
    # The sheet closes on successful export; inspect item actions again.
    tap(args.serial, find_node(args.serial, "More options for", deadline=30))
    find_node(args.serial, "View source and licensing", deadline=30)
    print("6/6 Source/attribution action present", flush=True)
    print("SMOKE_SUCCESS: Android emulator core workflow verified", flush=True)
    if args.extended:
        adb(args.serial, "shell", "input", "keyevent", "KEYCODE_BACK")
        verify_offline_restart(args.serial)
        print("7/8 Saved sticker survives offline app restart", flush=True)
        verify_sticker_pagination(args.serial)
        print("8/8 Native sticker Load more updates item count", flush=True)
        print("EXTENDED_SMOKE_SUCCESS: offline restart and sticker pagination verified", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as err:
        print(f"SMOKE_FAILURE: {type(err).__name__}: {err}", file=sys.stderr, flush=True)
        # The harness never probes any non-emulator serial.
        try:
            at = sys.argv.index("--serial")
            serial = sys.argv[at + 1]
        except (ValueError, IndexError):
            serial = "emulator-5554"
        capture_failure(serial)
        sys.exit(1)

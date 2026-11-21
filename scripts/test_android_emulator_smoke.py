#!/usr/bin/env python3
"""Host-side tests for the optional Android emulator acceptance harness."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location(
    "goreecloud_gif_smoke",
    Path(__file__).with_name("android-emulator-smoke.py"),
)
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class SmokeHarnessTest(unittest.TestCase):
    def test_reads_shell_owned_ui_hierarchy(self):
        dumped = '<hierarchy><node text="GoreeCloud GIF\'s" content-desc="" bounds="[0,0][100,100]" /></hierarchy>'
        with patch.object(smoke, "adb", side_effect=["UI hierarchy dumped", dumped]) as call:
            found = smoke.nodes("emulator-5580")
        self.assertEqual(found[0].get("text"), "GoreeCloud GIF's")
        self.assertEqual(call.call_args_list[0].args[4], smoke.UI_DUMP)

    def test_transient_missing_hierarchy_is_retried(self):
        node = ET.Element("node", {"text": "Stickers", "content-desc": ""})
        with patch.object(smoke, "nodes", side_effect=[RuntimeError("not ready"), [node]]):
            with patch.object(smoke.time, "sleep"):
                found = smoke.find_node("emulator-5580", "Stickers", exact=True, deadline=5)
        self.assertIs(found, node)

    def test_tap_uses_clickable_parent_when_android_text_bounds_are_zero(self):
        hierarchy = (
            '<hierarchy><node text="" bounds="[551,1988][1080,2072]" clickable="true">'
            '<node text="Saved (1)" bounds="[0,0][0,0]" clickable="false"/>'
            '</node></hierarchy>'
        )
        with patch.object(smoke, "adb", side_effect=["UI hierarchy dumped", hierarchy]):
            result = smoke.nodes("emulator-5580")
        saved = next(node for node in result if node.get("text") == "Saved (1)")
        self.assertEqual(saved.get("tap-bounds"), "[551,1988][1080,2072]")
        with patch.object(smoke, "adb") as adb:
            smoke.tap("emulator-5580", saved)
        self.assertEqual(
            adb.call_args.args, ("emulator-5580", "shell", "input", "tap", 815, 2030)
        )

    def test_zero_size_unclickable_bounds_are_never_tapped(self):
        node = ET.Element("node", {"bounds": "[0,0][0,0]"})
        with patch.object(smoke, "adb") as adb:
            with self.assertRaises(AssertionError):
                smoke.tap("emulator-5580", node)
        adb.assert_not_called()

    def test_tap_uses_center_of_actual_accessibility_bounds(self):
        node = ET.Element("node", {"bounds": "[20,30][120,230]"})
        with patch.object(smoke, "adb") as call:
            smoke.tap("emulator-5580", node)
        self.assertEqual(call.call_args.args, ("emulator-5580", "shell", "input", "tap", 70, 130))

    def test_unmatched_ui_reports_visible_labels(self):
        node = ET.Element("node", {"text": "Offline favorites", "content-desc": ""})
        with patch.object(smoke, "nodes", return_value=[node]):
            with patch.object(smoke.time, "sleep"):
                with self.assertRaises(AssertionError) as exc:
                    smoke.find_node("emulator-5580", "Download", deadline=0.001)
        self.assertIn("Offline favorites", str(exc.exception))

    def test_rejects_physical_device_diagnostics(self):
        with patch.object(smoke.subprocess, "run") as run:
            smoke.capture_failure("fa90cf85")
        run.assert_not_called()

    def test_download_requires_real_mediastore_png_row(self):
        response = (
            "Row: 0 _display_name=grinning-face-a96f.png, mime_type=image/png, "
            "relative_path=Pictures/GoreeCloud GIFs/\n"
        )
        with patch.object(smoke, "adb", return_value=response):
            self.assertIn("image/png", smoke.require_mediastore_download("emulator-5580"))
        with patch.object(smoke, "adb", return_value="Row: 0 mime_type=image/jpeg"):
            with self.assertRaises(AssertionError):
                smoke.require_mediastore_download("emulator-5580")

    def test_scroll_finds_native_load_more_footer(self):
        footer = ET.Element("node", {
            "text": "Load more stickers",
            "content-desc": "",
            "bounds": "[10,800][400,900]",
        })
        with patch.object(smoke, "adb", return_value="Physical size: 1080x2340") as adb:
            with patch.object(smoke, "nodes", side_effect=[[], [footer]]):
                with patch.object(smoke.time, "sleep"):
                    found = smoke.find_node_with_scrolling(
                        "emulator-5580", "Load more stickers", max_swipes=2
                    )
        self.assertIs(found, footer)
        self.assertTrue(
            any(call.args[1:4] == ("shell", "input", "swipe") for call in adb.call_args_list)
        )

    def test_scroll_does_not_tap_offscreen_lazy_grid_elements(self):
        offscreen = ET.Element("node", {
            "text": "Load more stickers",
            "content-desc": "",
            "bounds": "[10,2500][400,2600]",
        })
        onscreen = ET.Element("node", {
            "text": "Load more stickers",
            "content-desc": "",
            "bounds": "[10,800][400,900]",
        })
        with patch.object(smoke, "adb", return_value="Physical size: 1080x2340") as adb:
            with patch.object(smoke, "nodes", side_effect=[[offscreen], [onscreen]]):
                with patch.object(smoke.time, "sleep"):
                    found = smoke.find_node_with_scrolling(
                        "emulator-5580", "Load more stickers", max_swipes=2
                    )
        self.assertIs(found, onscreen)
        self.assertTrue(
            any(call.args[1:4] == ("shell", "input", "swipe") for call in adb.call_args_list)
        )

    def test_scroll_does_not_run_unbounded_or_touch_other_devices(self):
        with patch.object(smoke, "adb", return_value="Physical size: 1080x2340") as adb:
            with patch.object(smoke, "nodes", return_value=[]):
                with patch.object(smoke.time, "sleep"):
                    with self.assertRaises(AssertionError):
                        smoke.find_node_with_scrolling(
                            "emulator-5580", "Load more stickers", max_swipes=2
                        )
        swipes = [
            call for call in adb.call_args_list
            if call.args[1:4] == ("shell", "input", "swipe")
        ]
        self.assertEqual(2, len(swipes))
        self.assertTrue(all(call.args[0] == "emulator-5580" for call in swipes))

    def test_offline_restart_restores_radios_when_ui_fails(self):
        calls = []

        def fake_adb(serial, *args, **kwargs):
            self.assertEqual(serial, "emulator-5580")
            calls.append(args)
            if args[:3] == ("shell", "run-as", smoke.PACKAGE):
                return "favorite.png"
            return ""

        with patch.object(smoke, "adb", side_effect=fake_adb):
            with patch.object(smoke, "find_node", side_effect=AssertionError("not ready")):
                with self.assertRaises(AssertionError):
                    smoke.verify_offline_restart("emulator-5580")
        self.assertIn(("shell", "svc", "wifi", "disable"), calls)
        self.assertIn(("shell", "svc", "data", "disable"), calls)
        self.assertIn(("shell", "svc", "wifi", "enable"), calls)
        self.assertIn(("shell", "svc", "data", "enable"), calls)

    def test_removal_needs_cancel_and_explicit_confirmation(self):
        actions = []

        def find(serial, phrase, **kwargs):
            self.assertEqual(serial, "emulator-5580")
            return ET.Element("node", {"text": phrase})

        def tap(serial, node):
            actions.append(node.get("text"))

        responses = iter(["", ""])
        with patch.object(smoke, "find_node", side_effect=find):
            with patch.object(smoke, "tap", side_effect=tap):
                with patch.object(smoke, "require_file") as saved:
                    with patch.object(smoke, "adb", return_value=""):
                        smoke.verify_removal_requires_confirmation("emulator-5580")
        self.assertEqual(
            ["Saved (1)", "Remove favorite", "Keep saved", "Remove favorite",
             "Remove offline copy"],
            actions
        )
        saved.assert_called_once_with("emulator-5580")

    def test_cross_app_checks_real_granted_png_result_without_using_physical_device(self):
        clicked = []

        def fake_find(serial, phrase, **kwargs):
            self.assertEqual("emulator-5580", serial)
            # Android framework Buttons can expose uppercase text.
            if phrase in ("Send PNG to GoreeCloud", "Request PNG from GoreeCloud"):
                self.assertFalse(kwargs.get("exact", False))
            return ET.Element("node", {"text": phrase})

        def fake_adb(serial, *args, **kwargs):
            self.assertEqual("emulator-5580", serial)
            if args[:3] == ("shell", "run-as", smoke.FIXTURE_PACKAGE):
                return "PASS PNG GRANT mime=image/png"
            return ""

        with patch.object(smoke, "adb", side_effect=fake_adb) as adb_mock:
            with patch.object(smoke, "find_node", side_effect=fake_find):
                with patch.object(smoke, "tap",
                                  side_effect=lambda serial, node: clicked.append(node.get("text"))):
                    with patch.object(smoke, "require_file") as saved:
                        smoke.verify_cross_app_interop("emulator-5580", "fixture-debug.apk")
        self.assertIn("Send PNG to GoreeCloud", clicked)
        self.assertIn("Request PNG from GoreeCloud", clicked)
        self.assertIn("Choose interop-source", clicked)
        saved.assert_called_once_with("emulator-5580")
        self.assertTrue(any(call.args[1:4] == ("shell", "run-as", smoke.FIXTURE_PACKAGE)
                            for call in adb_mock.call_args_list))

    def test_cross_app_rejects_physical_devices_without_access(self):
        with patch.object(smoke, "adb") as adb_mock:
            with self.assertRaises(ValueError):
                smoke.verify_cross_app_interop("real-phone-123", "fixture-debug.apk")
            adb_mock.assert_not_called()

    def test_ignores_transient_malformed_xml(self):
        with patch.object(smoke, "adb", side_effect=["dump complete", "<hierarchy>"]):
            with self.assertRaises(RuntimeError) as error:
                smoke.nodes("emulator-5580")
        self.assertIn("incomplete", str(error.exception))


if __name__ == "__main__":
    unittest.main()

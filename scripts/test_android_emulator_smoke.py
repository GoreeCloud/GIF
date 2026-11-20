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

    def test_ignores_transient_malformed_xml(self):
        with patch.object(smoke, "adb", side_effect=["dump complete", "<hierarchy>"]):
            with self.assertRaises(RuntimeError) as error:
                smoke.nodes("emulator-5580")
        self.assertIn("incomplete", str(error.exception))


if __name__ == "__main__":
    unittest.main()

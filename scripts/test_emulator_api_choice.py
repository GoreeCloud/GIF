#!/usr/bin/env python3
"""Guard the manual emulator API selector against accidental unsafe changes."""
from pathlib import Path
import unittest

WORKFLOW = Path(__file__).resolve().parent.parent / ".github/workflows/emulator-smoke.yml"


class EmulatorApiChoiceTest(unittest.TestCase):
    def test_selected_api_levels_are_bounded(self):
        text = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("type: choice", text)
        self.assertIn("default: '36'", text)
        for value in ("29", "33", "35", "36"):
            self.assertIn("- '" + value + "'", text)
        self.assertIn('case "$TEST_ANDROID_API" in 29|33|35|36)', text)

    def test_sdk_manager_and_avd_use_same_api(self):
        text = WORKFLOW.read_text(encoding="utf-8")
        self.assertEqual(2, text.count("system-images;android-$TEST_ANDROID_API;default;x86_64"))
        self.assertIn('TEST_ANDROID_API: ${{ inputs.android_api || \'36\' }}', text)
        self.assertIn('EXTRA+=(--verify-removal)', text)


if __name__ == "__main__":
    unittest.main()

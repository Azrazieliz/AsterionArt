"""Offline contract checks: model pack must match the exact Android pins.

This catches accidental mismatches between the release-packaging script and
the on-device model store. External LFS SHA-256 values were checked by HTTPS
HEAD against Hugging Face; these tests do not download model weights.
"""
import re
import unittest
from pathlib import Path

import prepare_inference_weights

ROOT=Path(__file__).resolve().parents[1]
BRAIN=ROOT/"app/src/main/java/com/asterion/art/brain/BrainModelStore.kt"
VISION=ROOT/"app/src/main/java/com/asterion/art/vision/VisionModelStore.kt"

class TestInferenceModelPins(unittest.TestCase):
    def test_exact_three_inference_files_and_no_image_checkpoints(self):
        names=[row[0] for row in prepare_inference_weights.WEIGHTS]
        self.assertEqual(3,len(names))
        self.assertEqual(3,len(set(names)))
        self.assertTrue(any("Qwen3VL" in name for name in names))
        self.assertTrue(any("mmproj" in name for name in names))
        self.assertTrue(any("Qwen_Qwen3" in name for name in names))
        self.assertFalse(any("sdxl" in name.lower() or "anima" in name.lower()
            for name in names))

    def test_release_hashes_and_sizes_match_native_android_catalogue(self):
        sources=(BRAIN.read_text(encoding="utf-8")+
            "\n"+VISION.read_text(encoding="utf-8")).lower().replace("_","")
        for filename,repository,size,sha in prepare_inference_weights.WEIGHTS:
            self.assertIn(filename.lower().replace("_",""),sources)
            self.assertRegex(sha,r"^[0-9a-f]{64}$")
            self.assertIn(sha.lower(),sources)
            self.assertIsInstance(size,int)
            self.assertGreater(size,100_000_000)
            self.assertIn(str(size),sources)

if __name__=="__main__":
    unittest.main()

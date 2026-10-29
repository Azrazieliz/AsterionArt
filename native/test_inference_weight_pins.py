"""Guard release model pins against confusing Xet object hashes with SHA-256.

Hugging Face file pages publish distinct Xet identifiers and actual GGUF SHA-256.
These independent expected values are from the public file verification records.
"""
from pathlib import Path
import importlib.util
import unittest

ROOT=Path(__file__).resolve().parent.parent
PINS={
    "google_gemma-3-4b-it-Q4_K_M.gguf":
        "4996030242583a40aa151ff93f49ed787ac8c25e4120c3ae4588b2e2a7d1ae94",
    "mmproj-google_gemma-3-4b-it-f16.gguf":
        "8c0fb064b019a6972856aaae2c7e4792858af3ca4561be2dbf649123ba6c40cb",
}
XET={
    "72813ef7237ca81bbc8df7d72eb6c08ade6be3f815f46ca41a1b6a04ddf92fe6",
    "4f9a08744613d05f86991776e31a3385d8f0c4c8b33fb105f22edb5e24863c1e",
}

class InferenceModelPinTests(unittest.TestCase):
    def test_release_bundle_uses_actual_gguf_digests(self):
        path=ROOT/"native/prepare_inference_weights.py"
        spec=importlib.util.spec_from_file_location("asterion_inference_weights",path)
        module=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        actual={name:sha.lower() for name,_,_,sha in module.WEIGHTS}
        self.assertEqual(actual,PINS)
        self.assertTrue(set(actual.values()).isdisjoint(XET))
        self.assertTrue(all(len(value)==64 for value in actual.values()))

    def test_catalogues_agree_with_release_bundle(self):
        brain=(ROOT/"app/src/main/java/com/asterion/art/brain/BrainModelStore.kt").read_text()
        vision=(ROOT/"app/src/main/java/com/asterion/art/vision/VisionModelStore.kt").read_text()
        self.assertIn(PINS["google_gemma-3-4b-it-Q4_K_M.gguf"],brain)
        for filename in ("google_gemma-3-4b-it-Q4_K_M.gguf",
                         "mmproj-google_gemma-3-4b-it-f16.gguf"):
            self.assertIn(PINS[filename],vision)
        self.assertTrue(all(bad not in brain+vision for bad in XET))

if __name__=="__main__":
    unittest.main()

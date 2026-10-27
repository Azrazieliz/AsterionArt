"""Guard release model pins against confusing Xet object hashes with SHA-256.

Hugging Face file pages publish distinct Xet identifiers and actual GGUF SHA-256.
These independent expected values are from the public file verification records.
"""
from pathlib import Path
import importlib.util
import unittest

ROOT=Path(__file__).resolve().parent.parent
PINS={
    "Qwen_Qwen3-0.6B-Q5_K_M.gguf":
        "2df7894c99ee7716f0ed3c7d454c2386a7ae976c4b0ad0e716d55399c1360648",
    "Qwen3VL-2B-Instruct-Q4_K_M.gguf":
        "089d75c52f4b7ffc56ba998ffc50aae89fcafc755f9e7208aacca281dca6c2ae",
    "mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf":
        "f9a68fabba69c3b81e153367b2c7521030b0fa8bb0de400c9599c8e6725f9c82",
}
XET={
    "9d017e676272ce914f9dc65d959db574bedf71c2b4bfb882eb620f6fcc48ec17",
    "06fb8948e506b2e751a0f81f6e365382a3b9ea5f304ade3c77a814adc9543109",
    "df4d5f12ac5349b780b8a2d5033e9b529c14a82a30c5b69bbdcef52b79a56ff3",
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
        self.assertIn(PINS["Qwen_Qwen3-0.6B-Q5_K_M.gguf"],brain)
        for filename in ("Qwen3VL-2B-Instruct-Q4_K_M.gguf",
                         "mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf"):
            self.assertIn(PINS[filename],vision)
        self.assertTrue(all(bad not in brain+vision for bad in XET))

if __name__=="__main__":
    unittest.main()

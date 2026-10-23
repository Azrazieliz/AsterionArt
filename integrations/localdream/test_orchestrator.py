import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import asterion_orchestrator as a


class OrchestratorTests(unittest.TestCase):
    def setUp(self):
        self.job = {
            "compiler_validation": "GENERATED",
            "workflow_versions": {"methodology": "sha256:example"},
            "checkpoint_id": "NoobAI-XL-v11",
            "model_family": "SDXL",
            "positive_prompt": "silver hair, armored dress",
            "negative_prompt": "bad anatomy",
            "width": 1024,
            "height": 1024,
            "job_id": "test-output",
            "lora_library": [
                {"id": "hair", "model_family": "SDXL", "trigger_words": ["silver hair"], "weight": 0.8},
                {"id": "armor", "model_family": "SDXL", "trigger_words": ["armored dress"], "weight": 1.0},
            ],
        }

    def test_only_triggers_relevant_loras(self):
        self.assertFalse(a.contains_keyword("silvery hair", "silver hair"))
        self.assertTrue(a.contains_keyword("silver hair, detailed", "silver hair"))
        self.job["positive_prompt"] = "silver hair"
        self.job["converted_variants"] = [{
            "ready": True, "checkpoint_id": self.job["checkpoint_id"], "model_id": "hair_variant",
            "lora_weights": {"hair": 0.8},
        }]
        model, weights = a.select_variant(self.job)
        self.assertEqual("hair_variant", model)
        self.assertEqual({"hair": 0.8}, weights)

    def test_missing_combination_must_block(self):
        self.job["converted_variants"] = [{
            "ready": True, "checkpoint_id": self.job["checkpoint_id"], "model_id": "hair_variant",
            "lora_weights": {"hair": 0.8},
        }]
        with self.assertRaises(a.Blocked):
            a.select_variant(self.job)

    def test_incompatible_lora_must_block(self):
        self.job["lora_library"][0]["model_family"] = "ANIMA"
        with self.assertRaises(a.Blocked):
            a.select_variant(self.job)

    def test_no_compilation_validation_must_block(self):
        self.job["compiler_validation"] = "PENDING"
        with self.assertRaises(a.Blocked):
            a.validate_job(self.job)

    def test_generates_image_with_provenance_only_when_ready(self):
        self.job["positive_prompt"] = "night sky"
        def fake_query(base, path, payload=None, timeout=15):
            if path == "/info":
                return {"app": "localdream"}
            if path == "/models":
                return {"models": [{"id": self.job["checkpoint_id"]}]}
            if path == "/status":
                return {"state": "running", "serving_model_id": self.job["checkpoint_id"],
                        "width": 1024, "height": 1024}
            if path == "/asterion/capabilities":
                return {"model_id": self.job["checkpoint_id"]}
            if path == "/tokenize":
                return {"count": 30, "max_length": 77, "overflow_offset": -1}
            return {"ok": True}
        with tempfile.TemporaryDirectory() as tmp:
            with patch.object(a, "query", side_effect=fake_query), patch.object(
                a, "generate", return_value=(b"image-bytes", {"format": "png", "seed": 42})
            ):
                path = a.execute(self.job, Path(tmp))
            p = path / "provenance.json"
            self.assertTrue(p.exists())
            self.assertEqual("NoobAI-XL-v11", json.loads(p.read_text())["model_id"])
            self.assertEqual(11, json.loads(p.read_text())["size_bytes"])


if __name__ == "__main__":
    unittest.main()

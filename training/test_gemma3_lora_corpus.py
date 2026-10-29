"""Offline tests of the Gemma 3 training data contract; no weights needed."""
import json
import tempfile
import unittest
from pathlib import Path
import train_gemma3_grading_lora as learner


class Gemma3OwnerEvidenceTests(unittest.TestCase):
    def test_preserves_exact_owner_corrections_and_skips_unlabelled_scores(self):
        correction = "Preserve every source horn and hair lock; improve perspective."
        base = {
            "schema": "asterion.grading-evidence.v1",
            "creation_mode": "CHARACTER",
            "concepts": ["Rose", "Crystal"],
            "target_feature": "head anatomy",
            "correct_exact": correction,
            "preserve_exact": "Retain the approved distinctive eye geometry.",
        }
        score = dict(base, correct_exact="", preserve_exact="", overall=3)
        with tempfile.TemporaryDirectory() as root:
            path = Path(root) / "owner.jsonl"
            path.write_text("\n".join(json.dumps(x) for x in
                (base, base, score)) + "\n", encoding="utf-8")
            result = learner.grade_examples(path, 200)
        self.assertEqual(1, len(result))
        self.assertIn(correction, result[0][1])
        self.assertIn("Crystal", result[0][0])

    def test_invalid_schema_cannot_be_used_as_training_evidence(self):
        with tempfile.TemporaryDirectory() as root:
            path = Path(root) / "owner.jsonl"
            path.write_text('{"schema":"unapproved","correct_exact":"x"}\n')
            with self.assertRaises(ValueError):
                learner.grade_examples(path, 100)


if __name__ == "__main__":
    unittest.main()

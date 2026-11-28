import json
from pathlib import Path
import tempfile
import unittest

from approved_sources import check_training_sources, review_training_sources


class CorpusConfirmationTest(unittest.TestCase):
    def test_approval_matches_contents_and_competency(self):
        rows = [
            {"dataset_id": "owner-artworks", "sha256": "1" * 64},
            {"dataset_id": "owner-artworks", "sha256": "2" * 64},
        ]
        review = review_training_sources(rows)
        self.assertEqual(review["owner-artworks"]["images"], 2)
        decision = {
            "version": 1,
            "datasets": {
                "owner-artworks": {
                    "status": "approved",
                    "owner_decision": "Owner confirmed the reviewed fixture",
                    "competencies": ["reference_conditioning", "evaluation"],
                    "image_sets": {},
                }
            },
        }
        source = decision["datasets"]["owner-artworks"]
        with tempfile.TemporaryDirectory() as directory:
            register = Path(directory) / "sources.json"
            register.write_text(json.dumps(decision))
            with self.assertRaisesRegex(ValueError, "awaiting confirmation"):
                check_training_sources(rows, "reference_conditioning", register)
            source["image_sets"]["reference_conditioning"] = review["owner-artworks"]["image_set_sha256"]
            register.write_text(json.dumps(decision))
            self.assertEqual(check_training_sources(rows[::-1], "reference_conditioning", register), review)
            with self.assertRaisesRegex(ValueError, "awaiting confirmation"):
                check_training_sources(rows[:1], "reference_conditioning", register)
            self.assertEqual(
                check_training_sources(rows[:1], "reference_conditioning", register, approved_population=rows),
                review_training_sources(rows[:1]),
            )
            other = [{"dataset_id": "owner-artworks", "sha256": "3" * 64}]
            with self.assertRaisesRegex(ValueError, "outside the confirmed population"):
                check_training_sources(other, "reference_conditioning", register, approved_population=rows)
            with self.assertRaisesRegex(ValueError, "awaiting confirmation"):
                check_training_sources(rows, "evaluation", register)
            with self.assertRaisesRegex(ValueError, "more than once"):
                review_training_sources(rows + rows)

    def test_current_register_requires_owner_confirmation(self):
        for dataset in ("owner-artworks", "morevna-project", "cleveland-museum-cc0", "unlisted"):
            with self.subTest(dataset=dataset):
                with self.assertRaisesRegex(ValueError, "no owner approval"):
                    check_training_sources([{"dataset_id": dataset, "sha256": "0" * 64}], "reference_conditioning")

import hashlib
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest

from index_nexus_snapshot import import_admissions, refresh, summary


class NexusIndexTest(unittest.TestCase):
    def test_explicit_zero_incremental_updates_and_expiring_fingerprints(self):
        with tempfile.TemporaryDirectory() as location:
            source = Path(location) / "source.sqlite"
            with sqlite3.connect(source) as writer:
                writer.executescript("""
                    CREATE TABLE images(image_id INTEGER PRIMARY KEY, uri TEXT, size_bytes INT, width INT,
                      height INT, mime_type TEXT, active INT, favorite INT, tags_text TEXT);
                    CREATE TABLE fusion_image_profiles(image_id INTEGER PRIMARY KEY, rating INT, metadata_json TEXT);
                    CREATE TABLE fusion_image_fingerprints(image_id INTEGER PRIMARY KEY, sha256 TEXT,
                      perceptual_hash TEXT, source_size_bytes INT);
                    INSERT INTO images VALUES (1,'content://nexus/example/1',20,960,1280,'image/png',1,0,'silk'),
                      (2,'content://nexus/example/2',20,512,512,'image/png',1,0,'');
                    INSERT INTO fusion_image_profiles VALUES (1,0,'{"precision_overall_set":true}'), (2,0,'{}');
                """)
                digest = "a" * 64
                writer.execute("INSERT INTO fusion_image_fingerprints VALUES (1,?,?,20)", (digest, "77"))
            original_hash = hashlib.sha256(source.read_bytes()).hexdigest()
            with sqlite3.connect(":memory:") as index:
                with sqlite3.connect(source.as_uri() + "?mode=ro", uri=True) as original:
                    original.execute("PRAGMA query_only=ON")
                    self.assertEqual(refresh(original, index, "private"), 2)
                    self.assertEqual(refresh(original, index, "private"), 0)
                    entry = {"scope": "private", "image_id": 1, "sha256": digest, "competency": "identity",
                             "permission": "OWNER_AUTHORIZED", "provenance": "fixture consent record"}
                    import_admissions(index, "private", [json.dumps(entry)])
                    report = summary(index, "private")
                    self.assertEqual(report["manual_appreciation_scores"], 1)
                    self.assertEqual(report["explicit_zero_scores"], 1)
                    self.assertEqual(report["admitted_unique_by_competency"], {"identity": 1})
                    self.assertEqual(hashlib.sha256(source.read_bytes()).hexdigest(), original_hash)
                with sqlite3.connect(source) as updated:
                    updated.execute("UPDATE images SET size_bytes=21 WHERE image_id=1")
                with sqlite3.connect(source.as_uri() + "?mode=ro", uri=True) as new_snapshot:
                    self.assertEqual(refresh(new_snapshot, index, "private"), 1)
                    self.assertEqual(summary(index, "private")["admitted_unique_by_competency"], {})


if __name__ == "__main__":
    unittest.main()

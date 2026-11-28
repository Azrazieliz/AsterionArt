import hashlib
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest

from index_rounder_inventory import refresh


class RounderIndexTest(unittest.TestCase):
    def test_completed_generations_are_scoped_idempotent_and_atomic(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            index = base / "catalogue.sqlite"
            root = "/Art/Artworks"
            account = "1" * 24
            image = dict(path=root + "/a.webp", parent=root, name="a.webp", type="image",
                         folder=False, size=23801, modified=17, md5="b" * 32)
            other = dict(image, path=root + "/notes.txt", name="notes.txt", type="text")

            def source(generation, entries, count):
                stem = "rounder-" + hashlib.sha256(f"{account}|{root}|{generation}".encode()).hexdigest()[:20]
                checkpoint = base / f"{stem}.checkpoint.json"
                manifest = base / f"{stem}.jsonl.part"
                report = dict(source="Rounder via Control", status="complete", private_local_export=True,
                              generation=generation, account_fingerprint=account, entries=count)
                state = dict(done=True, summary=report, generation=generation, root=root, account_fingerprint=account)
                checkpoint.write_text(json.dumps(state), encoding="utf-8")
                manifest.write_text("".join(json.dumps(row) + "\n" for row in entries), encoding="utf-8")
                return checkpoint

            first = source(1, [image, other], 2)
            result = refresh(first, index)
            self.assertEqual(result["images"], 1)
            self.assertEqual(result["images_with_reported_md5"], 1)
            self.assertFalse(result["appreciation_available"])
            self.assertEqual(refresh(first, index), result)
            with self.assertRaisesRegex(ValueError, "same generation"):
                refresh(source(1, [dict(image, size=400), other], 2), index)
            with self.assertRaisesRegex(ValueError, "completed export count"):
                refresh(source(2, [other], 2), index)
            with self.assertRaisesRegex(ValueError, "out-of-scope"):
                refresh(source(2, [dict(image, path=root + "-other/a.webp")], 1), index)
            with sqlite3.connect(index) as db:
                self.assertEqual(db.execute("SELECT size_bytes FROM rounder_artwork").fetchone(), (23801,))
                self.assertEqual(db.execute("SELECT generation FROM rounder_snapshot").fetchone(), (1,))
            self.assertEqual(refresh(source(2, [other], 1), index)["images"], 0)
            with self.assertRaisesRegex(ValueError, "predates"):
                refresh(first, index)


if __name__ == "__main__":
    unittest.main()

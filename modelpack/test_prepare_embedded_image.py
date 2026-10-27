"""No-network, no-APK tests of binary-safe image-model asset partitioning."""
import hashlib
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

from prepare_embedded_image import prepare, validate, read_specs


class ImagePackTests(unittest.TestCase):
    def test_pinned_real_upstream_contract(self):
        data = read_specs()
        self.assertEqual(data["models"][0]["backend"], "anima")
        self.assertEqual(len(data["models"][0]["sha256"]), 64)

    def test_archive_and_corruption(self):
        with tempfile.TemporaryDirectory() as t:
            folder = Path(t)
            input_dir = folder / "input"
            input_dir.mkdir()
            archive = input_dir / "mini.zip"
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("ANIMA/config.json", '{"backend":"anima"}')
                z.writestr("ANIMA/test.bin", b"tensor bytes")
            raw = archive.read_bytes()
            data = {"models": [{
                "id": "mini_image_qnn", "label": "fixture", "backend": "anima",
                "filename": "mini.zip", "repository": "Fixture/Public",
                "component": "image_generation",
                "min_bytes": len(raw), "max_bytes": len(raw),
                "sha256": hashlib.sha256(raw).hexdigest()
            }]}
            report = prepare(data, folder / "out", input_dir)
            self.assertEqual(report["model"]["bytes"], len(raw))
            self.assertEqual(validate(data, folder / "out"), report)
            file = folder / "out" / "asterion_image_bundle" / (
                report["model"]["parts"][0]["path"])
            content = bytearray(file.read_bytes())
            content[8] ^= 1
            file.write_bytes(content)
            with self.assertRaisesRegex(ValueError, "SHA-256"):
                validate(data, folder / "out")


if __name__ == "__main__":
    unittest.main()

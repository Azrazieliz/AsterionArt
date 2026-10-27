"""Pure-stdlib tests for model-byte preservation and fail-closed embedding.

These use tiny synthetic files and never fetch model weights or build an APK.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from prepare_embedded_models import prepare, validate, read_specs


class PackagingTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.inputs = self.root / "models"
        self.inputs.mkdir()
        self.generated = self.root / "generated"
        self.spec = {"chunk_bytes": 64 * 1024 * 1024, "models": []}
        for component in ("reasoning", "visual_language", "visual_projector"):
            content = (component * 983).encode("ascii")
            filename = component + ".gguf"
            (self.inputs / filename).write_bytes(content)
            self.spec["models"].append({
                "id": component,
                "filename": filename,
                "component": component,
                "bytes": len(content),
                "sha256": hashlib.sha256(content).hexdigest(),
                "repository": "Example/TinyModel",
                "license": "Apache-2.0",
            })

    def test_offline_verified_chunks_have_no_network_dependency(self):
        manifest = prepare(self.spec, self.generated, self.inputs)
        self.assertEqual(3, len(manifest["models"]))
        self.assertEqual(
            sum(item["bytes"] for item in self.spec["models"]),
            validate(self.generated, self.spec)["total_bytes"],
        )
        self.assertTrue(all(m["parts"] for m in manifest["models"]))
        self.assertFalse(manifest["image_diffusion_checkpoint_included"])

    def test_modified_chunk_causes_hard_failure(self):
        pack = prepare(self.spec, self.generated, self.inputs)
        part = self.generated / "asterion_bundle" / \
            pack["models"][0]["parts"][0]["path"]
        content = bytearray(part.read_bytes())
        content[4] ^= 5
        part.write_bytes(content)
        with self.assertRaisesRegex(ValueError, "checksum"):
            validate(self.generated, self.spec)

    def test_wrong_remote_sha_never_creates_bundled_assets(self):
        self.spec["models"][0]["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
            prepare(self.spec, self.generated, self.inputs)
        self.assertFalse((self.generated / "asterion_bundle").exists())

    def test_upstream_pinned_manifest_is_well_formed(self):
        read_specs(Path(__file__).with_name("models.json"))


if __name__ == "__main__":
    unittest.main()

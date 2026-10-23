import tempfile
import unittest
from pathlib import Path

from apply_localdream_adapter import MARKER, patch


class LocalDreamPatchTests(unittest.TestCase):
    def test_idempotent_and_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / "app/src/main/cpp/src/main.cpp"
            target.parent.mkdir(parents=True)
            target.write_text("  if (text_encoder) registerTokenizeEndpoint(svr, text_encoder.get());\n")
            self.assertEqual("patched", patch(root))
            output = target.read_text()
            self.assertIn(MARKER, output)
            self.assertIn('"/asterion/capabilities"', output)
            self.assertEqual("already-patched", patch(root))
            self.assertEqual(output, target.read_text())

    def test_rejects_changed_upstream_without_modification(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / "app/src/main/cpp/src/main.cpp"
            target.parent.mkdir(parents=True)
            target.write_text("// changed\n")
            with self.assertRaises(RuntimeError):
                patch(root)
            self.assertEqual("// changed\n", target.read_text())


if __name__ == "__main__":
    unittest.main()

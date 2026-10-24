import hashlib
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0,str(Path(__file__).parent))
import prepare_native_runtime as native


class NativeBundleTests(unittest.TestCase):
    def test_wrong_hash_is_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td)
            source=root/"fake.apk"
            source.write_bytes(b"malicious")
            with self.assertRaisesRegex(ValueError,"hash mismatch"):
                native.unpack(source,root)

    def test_exact_pinned_bundle_layout(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td)
            source=root/"fixture.apk"
            with zipfile.ZipFile(source,"w") as z:
                z.writestr("lib/arm64-v8a/libstable_diffusion_core.so",b"abcd")
                z.writestr("assets/qnnlibs/libQnnHtp.so",b"EFGH")
                z.writestr("assets/qnnlibs/libQnnSystem.so",b"IJKL")
                z.writestr("assets/qnnlibs/libQnnHtpV79Stub.so",b"NM")
                z.writestr("assets/ditlibs/libSomeSkel.so",b"OPS")
                z.writestr("assets/safety_checker.mnn",b"FILTER-DO-NOT-EMBED")
            digest=hashlib.sha256(source.read_bytes()).hexdigest()
            result=native.unpack(source,root,expected_sha=digest)
            names={x["path"] for x in result["files"]}
            self.assertIn("jniLibs/arm64-v8a/libstable_diffusion_core.so",names)
            self.assertNotIn("assets/safety_checker.mnn",names)
            self.assertEqual(5,len(names))
            for file in names:
                self.assertTrue((root/"app/src/main"/file).is_file())
            self.assertTrue((root/"app/src/main/assets/asterion_native_provenance.json").exists())

    def test_incomplete_bundle_does_not_publish(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td)
            source=root/"incomplete.apk"
            with zipfile.ZipFile(source,"w") as z:
                z.writestr("lib/arm64-v8a/libstable_diffusion_core.so",b"abc")
            digest=hashlib.sha256(source.read_bytes()).hexdigest()
            with self.assertRaisesRegex(ValueError,"essential native files"):
                native.unpack(source,root,expected_sha=digest)
            self.assertFalse((root/"app/src/main/jniLibs").exists())


if __name__=="__main__":
    unittest.main()

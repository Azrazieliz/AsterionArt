import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0,str(Path(__file__).parent))
from verify_apk_native import verify


class VerifyInstalledEnginesTests(unittest.TestCase):
    def test_rejects_release_without_local_vision_or_realsr(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            archive=root/"candidate.apk"
            fake=b"trusted-qnn"
            with zipfile.ZipFile(archive,"w") as z:
                for path in ("lib/arm64-v8a/libstable_diffusion_core.so",
                             "assets/qnnlibs/libQnnHtp.so",
                             "assets/qnnlibs/libQnnSystem.so"):
                    z.writestr(path,fake)
            provenance=root/"native.json"
            provenance.write_text(json.dumps({
                "apk_sha256":"synthetic-test",
                "files":[{"path":p,"sha256":hashlib.sha256(fake).hexdigest()}
                    for p in ("jniLibs/arm64-v8a/libstable_diffusion_core.so",
                              "assets/qnnlibs/libQnnHtp.so",
                              "assets/qnnlibs/libQnnSystem.so")]
            }))
            with self.assertRaisesRegex(ValueError,"local inference engine"):
                verify(archive,provenance)

    def test_verifies_all_engine_components(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            archive=root/"candidate.apk"
            fake=b"native-test"
            members=[
                "lib/arm64-v8a/libstable_diffusion_core.so",
                "assets/qnnlibs/libQnnHtp.so",
                "assets/qnnlibs/libQnnSystem.so",
                "lib/arm64-v8a/libai-chat.so",
                "lib/arm64-v8a/libasterion-vision.so",
                "lib/arm64-v8a/libasterion_realsr_cli.so"]
            with zipfile.ZipFile(archive,"w") as z:
                for name in members:z.writestr(name,fake)
            provenance=root/"native.json"
            provenance.write_text(json.dumps({
                "apk_sha256":"synthetic-test",
                "files":[{"path":p,"sha256":hashlib.sha256(fake).hexdigest()}
                    for p in ("jniLibs/arm64-v8a/libstable_diffusion_core.so",
                              "assets/qnnlibs/libQnnHtp.so",
                              "assets/qnnlibs/libQnnSystem.so")]
            }))
            report=verify(archive,provenance)
            self.assertEqual(3,len(report["other_engines_verified"]))

if __name__=="__main__":
    unittest.main()

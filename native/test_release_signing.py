import sys
from pathlib import Path
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).parent))
from verify_release_signing import (
    keystore_matches_original,digest_from_keytool,digest_from_apksigner,
)

class OwnerReleaseSigningTests(unittest.TestCase):
    def test_rejects_any_alternate_release_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            forged=Path(directory)/"fake.p12"
            forged.write_bytes(b"no-new-identity"*256)
            self.assertFalse(keystore_matches_original(forged))
    def test_keytool_certificate_fingerprint_parsing(self):
        h="ab"*32
        sample="Certificate fingerprints:\n\tSHA1: 00:11\n\tSHA256: "+":".join(
            h[i:i+2].upper() for i in range(0,64,2))+"\n"
        self.assertEqual(h,digest_from_keytool(sample))
        with self.assertRaises(ValueError):digest_from_keytool("SHA256: n/a")
    def test_apksigner_requires_verified_v2_or_v3_and_exactly_one_signer(self):
        digest="ca"*32
        output=("Verified using v2 scheme (APK Signature Scheme v2): true\n"+
                "Signer #1 certificate SHA-256 digest: "+digest+"\n")
        self.assertEqual(digest,digest_from_apksigner(output))
        v3=output.replace(
            "Verified using v2 scheme (APK Signature Scheme v2): true",
            "Verified using v2 scheme (APK Signature Scheme v2): false\n"
            "Verified using v3 scheme (APK Signature Scheme v3): true")
        self.assertEqual(digest,digest_from_apksigner(v3))
        with self.assertRaises(ValueError):
            digest_from_apksigner("Signer #1 certificate SHA-256 digest: "+digest)
        with self.assertRaises(ValueError):
            digest_from_apksigner(output+"Signer #1 certificate SHA-256 digest: "+digest)
        with self.assertRaises(ValueError):
            digest_from_apksigner(output+"Signer #2 certificate SHA-256 digest: "+digest)

if __name__=="__main__":
    unittest.main()

#!/usr/bin/env python3
"""Release identity gate: no debug key, no unsigned APK, no replacement owner key.

The SHA256 below identifies the original owner-controlled PKCS12 container.
It is not a secret and cannot be used to sign anything. No passwords are
accepted on argv or written to build artifacts.
"""
from __future__ import annotations
import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess

ORIGINAL_P12_SHA256 = "90f19f1436cd0617b55ad0b35eaaae846e07e01e260571fa12a326bbf05e8864"
REQUIRED_ALIAS = "asterion-art-persistent-20261028"
REQUIRED_CERT_SHA256 = "b0263c37c9f4ae40423e30d275b6c8af0b777ed223d01e3fff1a114a43707ede"


def keystore_matches_original(file: Path) -> bool:
    if not file.is_file() or file.stat().st_size < 128:
        return False
    sha = hashlib.sha256()
    with file.open("rb") as reader:
        for chunk in iter(lambda:reader.read(1024*1024),b""):
            sha.update(chunk)
    return sha.hexdigest().lower() == ORIGINAL_P12_SHA256


def digest_from_keytool(stdout: str) -> str:
    # keytool -list -v -alias ... prints "SHA256: AA:BB:..." under
    # Certificate fingerprints. Parse the certificate, not keystore hash.
    results = re.findall(r"^\s*SHA256:\s*([a-fA-F0-9:]{64,95})\s*$",
                         stdout,flags=re.MULTILINE)
    if len(results) != 1:
        raise ValueError("Owner signing certificate fingerprint missing or ambiguous")
    return results[0].replace(":","").lower()


def digest_from_apksigner(stdout: str) -> str:
    results = re.findall(
        r"^Signer #(\d+) certificate SHA-256 digest:\s*([a-fA-F0-9]{64})\s*$",
        stdout,flags=re.MULTILINE)
    valid_scheme = any(
        f"Verified using v{version} scheme (APK Signature Scheme v{version}): true" in stdout
        for version in ("2", "3", "3.1")
    )
    if len(results)!=1 or results[0][0]!="1" or not valid_scheme:
        raise ValueError("APK lacks a unique valid V2/V3 owner signing certificate")
    return results[0][1].lower()


def run(args: list[str]) -> str:
    p = subprocess.run(args, capture_output=True, text=True, check=False,
                       env=os.environ.copy())
    if p.returncode!=0:
        # Do not echo keytool stderr: some providers may include sensitive paths.
        raise ValueError("Required signing certificate verification failed")
    return p.stdout


def verify_apk(apk:Path,keystore:Path,apksigner:Path) -> str:
    if not keystore_matches_original(keystore):
        raise ValueError("Provided PKCS12 does not match ORIGINAL release identity")
    if not os.environ.get("ASTERION_RELEASE_PASSWORD"):
        raise ValueError("Original release password is not available")
    if not apk.is_file() or apk.stat().st_size<4096:
        raise ValueError("Signed APK does not exist")
    signed=run([str(apksigner),"verify","--verbose","--print-certs",str(apk)])
    signed_fingerprint=digest_from_apksigner(signed)
    identity=run(["keytool","-list","-v","-keystore",str(keystore),
                 "-storetype","PKCS12","-storepass:env",
                 "ASTERION_RELEASE_PASSWORD","-alias",REQUIRED_ALIAS])
    source_fingerprint=digest_from_keytool(identity)
    if (signed_fingerprint!=source_fingerprint or
        signed_fingerprint!=REQUIRED_CERT_SHA256):
        raise ValueError("APK was NOT signed by the installed persistent release identity")
    return signed_fingerprint


def main():
    args=argparse.ArgumentParser()
    options=args.add_subparsers(dest="mode",required=True)
    p=options.add_parser("keystore")
    p.add_argument("path",type=Path)
    a=options.add_parser("apk")
    a.add_argument("path",type=Path)
    a.add_argument("keystore",type=Path)
    a.add_argument("apksigner",type=Path)
    values=args.parse_args()
    if values.mode=="keystore":
        if not keystore_matches_original(values.path):
            raise SystemExit("Original owner keystore hash mismatch; refusing signing")
        print("Original owner release identity file matches pinned SHA256.")
    else:
        print("Owner release signature VERIFIED; certificate SHA256: "+
              verify_apk(values.path,values.keystore,values.apksigner))


if __name__=="__main__":
    main()

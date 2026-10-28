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

ORIGINAL_P12_SHA256 = "5a62e4254f13aaed0cf45586dc130735ae743bf8af8fec6a94a7c3e53edddef"
REQUIRED_ALIAS = "asterion-art-release"


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
    if (len(results)!=1 or results[0][0]!="1" or
        "Verified using v2 scheme (APK Signature Scheme v2): true" not in stdout):
        raise ValueError("APK lacks a unique valid V2 owner signing certificate")
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
    if signed_fingerprint!=source_fingerprint:
        raise ValueError("APK was NOT signed by the original release identity")
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

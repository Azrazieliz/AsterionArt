#!/usr/bin/env python3
"""Verify that a built Asterion APK actually contains the pinned native engine.

Unlike a green Kotlin compilation, this checks the final installable ZIP.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def verify(apk: Path, provenance: Path) -> dict:
    record = json.loads(provenance.read_text())
    expected = {
        f["path"]: f["sha256"] for f in record["files"]
    }
    with zipfile.ZipFile(apk) as archive:
        archive_names = set(archive.namelist())
        needs = {
            "jniLibs/arm64-v8a/libstable_diffusion_core.so",
            "assets/qnnlibs/libQnnHtp.so",
            "assets/qnnlibs/libQnnSystem.so"
        }
        if not needs.issubset(expected):
            raise ValueError("Upstream provenance missing essential binaries")
        # Native image generation alone is insufficient: the installed release
        # must contain its independent reasoning, multimodal and RealSR engines.
        required_local = {
            "lib/arm64-v8a/libai-chat.so",
            "lib/arm64-v8a/libasterion-vision.so",
            "lib/arm64-v8a/libasterion_realsr_cli.so",
            # llmedge's embedded stable-diffusion.cpp Wan runtime.
            "lib/arm64-v8a/libsdcpp.so",
        }
        absent = required_local - archive_names
        if absent:
            raise ValueError("Compiled APK lacks required local inference engine(s): "+
                             ", ".join(sorted(absent)))
        results = []
        for source, checksum in expected.items():
            if source.startswith("jniLibs/"):
                inside = "lib/" + source.removeprefix("jniLibs/")
            else:
                inside = source
            if inside not in archive_names:
                raise ValueError("Compiled APK missing native runtime file: " + inside)
            digest = hashlib.sha256()
            with archive.open(inside) as entry:
                while part := entry.read(1024 * 1024):
                    digest.update(part)
            if digest.hexdigest() != checksum:
                raise ValueError("Packaged native file differs from trusted extraction: " + inside)
            results.append(inside)
    return {"apk":str(apk),"native_files_verified":len(results),
            "other_engines_verified":sorted(required_local),
            "native_executable": "lib/arm64-v8a/libstable_diffusion_core.so",
            "upstream_apk_sha256":record["apk_sha256"]}


def main():
    p=argparse.ArgumentParser()
    p.add_argument("--apk",type=Path,default=Path("app/build/outputs/apk/debug/app-debug.apk"))
    p.add_argument("--provenance",type=Path,default=Path("app/src/main/assets/asterion_native_provenance.json"))
    a=p.parse_args()
    print(json.dumps(verify(a.apk,a.provenance),indent=2))


if __name__=="__main__":
    main()

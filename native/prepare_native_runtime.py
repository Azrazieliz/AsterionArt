#!/usr/bin/env python3
"""Pin and repackage the non-filtered Local Dream native runtime for Asterion Art.

This is an APK-to-APK *native-runtime* integration, not another installed app.
Source: xororz/local-dream CC BY-NC 4.0. Qualcomm QNN binaries retain their own
licence and notices. Do not remove attribution or publish a commercial binary.
No QNN/CPU model weights are included; users import their own model packages.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import tempfile
from urllib.request import Request, urlopen
import zipfile

UPSTREAM_VERSION = "v3.0.0-alpha.6"
URL = ("https://github.com/xororz/local-dream/releases/download/"
       + UPSTREAM_VERSION + "/LocalDream_armv8a_3.0.0-alpha.6.apk")
APK_SHA256 = "bec75b588200eb83aedd82b0a26ed8c5de4cfd923d0f51e1e65d241fccc11f82"
MAX_APK_BYTES = 200_000_000
MAX_TOTAL_EXTRACTED = 750_000_000
NATIVE_PREFIX = "lib/arm64-v8a/"
ASSETS = ("assets/qnnlibs/", "assets/ditlibs/")


def stream_fetch(url: str, destination: Path) -> None:
    req = Request(url, headers={"User-Agent": "Asterion-Art-build/1"})
    with urlopen(req, timeout=120) as source, destination.open("wb") as target:
        total = 0
        while chunk := source.read(1024 * 1024):
            total += len(chunk)
            if total > MAX_APK_BYTES:
                raise ValueError("Upstream APK exceeds maximum trusted size")
            target.write(chunk)
    if total < 5_000_000:
        raise ValueError("Downloaded APK suspiciously small")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(8 * 1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def unpack(apk: Path, android_project: Path, *, expected_sha: str = APK_SHA256) -> dict:
    found = sha256(apk)
    if found != expected_sha:
        raise ValueError(f"APK hash mismatch (got {found}); refusing unverified upstream binary")

    root = android_project / "app/src/main"
    staging = Path(tempfile.mkdtemp(prefix="asterion-native-", dir=android_project))
    files: list[dict] = []
    total = 0
    try:
        with zipfile.ZipFile(apk) as archive:
            for member in archive.infolist():
                if member.is_dir():
                    continue
                path = member.filename
                if path.startswith(NATIVE_PREFIX) and path.endswith(".so"):
                    local = Path("jniLibs/arm64-v8a") / Path(path).name
                elif path.startswith(ASSETS) and "/" not in path.removeprefix("assets/qnnlibs/").removeprefix("assets/ditlibs/"):
                    local = Path(path)
                else:
                    continue
                if member.file_size > MAX_TOTAL_EXTRACTED:
                    raise ValueError("Native package contains implausibly large entry")
                total += member.file_size
                if total > MAX_TOTAL_EXTRACTED:
                    raise ValueError("Native package total uncompressed size too large")
                if local.name in ("", ".", "..") or "\\" in path:
                    raise ValueError("Unsafe ZIP entry")
                dest = staging / local
                dest.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(member) as source, dest.open("wb") as output:
                    shutil.copyfileobj(source, output, 1024 * 1024)
                files.append({"path": local.as_posix(), "bytes": dest.stat().st_size,
                              "sha256": sha256(dest)})

        required = {
            "jniLibs/arm64-v8a/libstable_diffusion_core.so",
            "assets/qnnlibs/libQnnHtp.so",
            "assets/qnnlibs/libQnnSystem.so",
        }
        delivered = {p["path"] for p in files}
        if not required.issubset(delivered):
            raise ValueError(f"Upstream package lacks essential native files: {sorted(required-delivered)}")
        if not any(p.startswith("assets/qnnlibs/libQnnHtpV") for p in delivered):
            raise ValueError("Missing Qualcomm HTP native runtime")
        # Each bundle is deployed from a verified complete staging directory.
        for record in files:
            source = staging / record["path"]
            target = root / record["path"]
            target.parent.mkdir(parents=True, exist_ok=True)
            os.replace(source, target)
        manifest = {
            "upstream": "xororz/local-dream",
            "version": UPSTREAM_VERSION,
            "origin": URL,
            "apk_sha256": expected_sha,
            "license": "CC BY-NC 4.0 + Qualcomm QNN runtime notices",
            "files": files,
        }
        (root / "assets").mkdir(parents=True, exist_ok=True)
        (root / "assets/asterion_native_provenance.json").write_text(json.dumps(manifest, indent=2))
        return manifest
    finally:
        shutil.rmtree(staging, ignore_errors=True)


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--project", type=Path, default=Path("."))
    p.add_argument("--apk", type=Path, default=None)
    args = p.parse_args()
    project = args.project.resolve()
    with tempfile.TemporaryDirectory(prefix="asterion-runtime-apk-") as temp:
        archive = args.apk or Path(temp) / "localdream.apk"
        if args.apk is None:
            stream_fetch(URL, archive)
        result = unpack(archive, project)
    print(json.dumps({"version": UPSTREAM_VERSION, "installed_files": len(result["files"]),
                      "total_bytes": sum(f["bytes"] for f in result["files"])}, indent=2))


if __name__ == "__main__":
    main()

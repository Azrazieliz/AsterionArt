#!/usr/bin/env python3
"""Stage the complete, preconverted Anima/QNN model as verified local app assets.

Only the packaging host accesses Hugging Face. The installed Android app never
downloads model weights. No APK or build is invoked by this script.
Remote exact bytes are deliberately measured, not fabricated from rounded UI
size labels. Complete SHA-256 is pinned to the upstream archive file detail.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import urllib.request
import zipfile

CHUNK = 64 * 1024 * 1024
SPEC = Path(__file__).with_name("image_sources.json")
SAFE = re.compile(r"^[a-z0-9_]{4,80}$")
FILENAME = re.compile(r"^[A-Za-z0-9_.-]{4,160}$")


def read_specs(source: Path = SPEC) -> dict:
    data = json.loads(source.read_text(encoding="utf-8"))
    if (data.get("schema") != "asterion.embedded-image-sources.v1"
            or data.get("chunk_bytes") != CHUNK):
        raise ValueError("Unsupported image model packaging contract")
    models = data["models"]
    if len(models) != 1 or models[0]["component"] != "image_generation":
        raise ValueError("A complete, single image checkpoint is required")
    item = models[0]
    if (not SAFE.fullmatch(item["id"]) or
            not FILENAME.fullmatch(item["filename"]) or
            not re.fullmatch("[0-9a-f]{64}", item["sha256"]) or
            not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+",
                             item["repository"])):
        raise ValueError("Invalid pinned image model source")
    if item["backend"] != "anima":
        raise ValueError("The reviewed checkpoint has Anima QNN tensor layout")
    if not (0 < item["min_bytes"] <= item["max_bytes"] <= 12_000_000_000):
        raise ValueError("Unsafe source-size bounds")
    return data


def open_source(item: dict, offline_dir: Path | None):
    if offline_dir is not None:
        return (offline_dir / item["filename"]).open("rb")
    url = ("https://huggingface.co/" + item["repository"] +
           "/resolve/main/" + item["filename"] + "?download=true")
    return urllib.request.urlopen(urllib.request.Request(
        url, headers={"Accept-Encoding": "identity",
                      "User-Agent": "Asterion-Art-private-offline-assembler/1.0"}),
        timeout=180)


def prepare(data: dict, output: Path, offline_dir: Path | None = None):
    root = output / "asterion_image_bundle"
    if root.exists():
        shutil.rmtree(root)
    root.mkdir(parents=True)
    item = data["models"][0]
    folder = root / item["id"]
    folder.mkdir()
    full = hashlib.sha256()
    length = 0
    parts = []
    try:
        with open_source(item, offline_dir) as source:
            first = source.read(4)
            if first != b"PK\\x03\\x04".decode("unicode_escape").encode("latin1"):
                raise ValueError("Preconverted QNN image model is not a ZIP archive")
            pending = first
            while True:
                part = folder / f"part-{len(parts):04d}.chunk"
                written = 0
                digest = hashlib.sha256()
                with part.open("wb") as dest:
                    while written < CHUNK:
                        payload = pending or source.read(min(1024 * 1024, CHUNK-written))
                        pending = b""
                        if not payload:
                            break
                        length += len(payload)
                        written += len(payload)
                        if length > item["max_bytes"]:
                            raise ValueError("Image archive exceeds safe maximum bytes")
                        digest.update(payload)
                        full.update(payload)
                        dest.write(payload)
                if written == 0:
                    part.unlink()
                    break
                parts.append({
                    "path": item["id"] + "/" + part.name,
                    "bytes": written,
                    "sha256": digest.hexdigest()
                })
                if written < CHUNK:
                    break
        if (length < item["min_bytes"] or
                full.hexdigest() != item["sha256"]):
            raise ValueError("Upstream image archive byte count or SHA-256 mismatch")
        manifest = {
            "schema": "asterion.embedded-image-pack.v1",
            "chunk_bytes": CHUNK,
            "model": {
                "id": item["id"], "label": item["label"],
                "backend": item["backend"], "filename": item["filename"],
                "bytes": length, "sha256": item["sha256"],
                "upstream": item["repository"], "parts": parts
            },
            "offline_after_install": True,
            "on_device_download_required": False
        }
        (root / "manifest.json").write_text(json.dumps(manifest, indent=2),
                                            encoding="utf-8")
        validate(data, output)
        return manifest
    except BaseException:
        shutil.rmtree(root, ignore_errors=True)
        raise


def validate(data: dict, output: Path):
    root = output / "asterion_image_bundle"
    pack = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
    if (pack.get("schema") != "asterion.embedded-image-pack.v1" or
            pack.get("chunk_bytes") != CHUNK or
            pack.get("offline_after_install") is not True or
            pack.get("on_device_download_required") is not False):
        raise ValueError("Malformed offline image model manifest")
    pinned = data["models"][0]
    model = pack["model"]
    if any(model[field] != pinned[field] for field in (
            "id", "backend", "filename", "sha256")):
        raise ValueError("Image checkpoint differs from selected pinned model")
    if not (pinned["min_bytes"] <= model["bytes"] <= pinned["max_bytes"]):
        raise ValueError("Model size outside documented source bounds")
    if not model["parts"] or len(model["parts"]) > 200:
        raise ValueError("Missing or excessive chunks")
    sha = hashlib.sha256()
    total = 0
    for index, chunk in enumerate(model["parts"]):
        name = f"{model['id']}/part-{index:04d}.chunk"
        if chunk["path"] != name or not (0 < chunk["bytes"] <= CHUNK):
            raise ValueError("Unsafe image chunk sequence")
        file = root / name
        if not file.is_file() or file.stat().st_size != chunk["bytes"]:
            raise ValueError("Image chunk size mismatch")
        digest = hashlib.sha256()
        with file.open("rb") as reader:
            for payload in iter(lambda: reader.read(1024 * 1024), b""):
                sha.update(payload)
                digest.update(payload)
                total += len(payload)
        if digest.hexdigest() != chunk["sha256"]:
            raise ValueError("Image chunk SHA-256 mismatch")
    if total != model["bytes"] or sha.hexdigest() != pinned["sha256"]:
        raise ValueError("Complete image archive SHA-256 mismatch")
    return pack


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--source-manifest", type=Path, default=SPEC)
    p.add_argument("--output", type=Path, default=Path(
        "app/build/generated/embeddedModelsAssets"))
    p.add_argument("--offline-input-dir", type=Path)
    p.add_argument("--verify-only", action="store_true")
    args = p.parse_args()
    sources = read_specs(args.source_manifest)
    report = (validate(sources, args.output) if args.verify_only else
              prepare(sources, args.output, args.offline_input_dir))
    print(json.dumps({"image_model": report["model"]["id"],
                      "verified_complete_bytes": report["model"]["bytes"],
                      "offline_after_install": True,
                      "apk_built": False}, indent=2))


if __name__ == "__main__":
    main()

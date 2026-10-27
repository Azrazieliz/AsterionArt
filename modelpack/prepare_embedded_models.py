#!/usr/bin/env python3
"""Asterion Art self-contained Android release model-asset preparation.

Downloads happen ONLY on the packaging host (GitHub Actions). On-device
installation reads exclusively from assets, without network access.
Never check weights, access tokens or compiled APKs into the Git repo.

No build is started by this script unless invoked explicitly by the build job.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import urllib.request

CHUNK_MAX = 64 * 1024 * 1024
SAFE_ID = re.compile(r"^[a-z0-9_]{4,80}$")
SAFE_FILE = re.compile(r"^[A-Za-z0-9_.-]{4,160}$")
DEFAULT_SPEC = Path(__file__).with_name("models.json")


def read_specs(path: Path) -> dict:
    spec = json.loads(path.read_text(encoding="utf-8"))
    if spec["schema"] != "asterion.embedded-model-sources.v1":
        raise ValueError("Unsupported model source manifest")
    if spec["chunk_bytes"] != CHUNK_MAX:
        raise ValueError("Only the reviewed 64MiB chunking format is supported")
    names = set()
    for model in spec["models"]:
        mid = model["id"]
        name = model["filename"]
        if not SAFE_ID.fullmatch(mid) or not SAFE_FILE.fullmatch(name):
            raise ValueError("Unsafe pinned model ID or filename")
        if mid in names:
            raise ValueError("Duplicate model")
        names.add(mid)
        if not (100_000_000 <= model["bytes"] <= 2_000_000_000):
            raise ValueError("Unexpected model size")
        if not re.fullmatch(r"[0-9a-f]{64}", model["sha256"]):
            raise ValueError("Missing pinned model SHA256")
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+",
                            model["repository"]):
            raise ValueError("Unsafe Hugging Face source")
        if model["license"] != "Apache-2.0":
            raise ValueError("No commercial redistribution permission established")
    if {m["component"] for m in spec["models"]} != {
            "reasoning", "visual_language", "visual_projector"}:
        raise ValueError("Baseline reasoning+visual model pair must be complete")
    return spec


def input_stream(model: dict, offline: Path | None):
    if offline is not None:
        path = offline / model["filename"]
        if not path.is_file():
            raise FileNotFoundError(path)
        return path.open("rb")
    url = "https://huggingface.co/" + model["repository"] + "/resolve/main/" \
        + model["filename"] + "?download=true"
    return urllib.request.urlopen(
        urllib.request.Request(url, headers={
            "User-Agent": "Asterion-Art-offline-model-packager/1.0",
            "Accept-Encoding": "identity",
        }), timeout=120)


def write_model(model: dict, target: Path, chunk_size: int, offline: Path | None):
    target.mkdir(parents=True, exist_ok=True)
    total = 0
    full_digest = hashlib.sha256()
    chunk_specs = []
    with input_stream(model, offline) as stream:
        while total < model["bytes"]:
            remaining = min(chunk_size, model["bytes"]-total)
            part_file = target / f"part-{len(chunk_specs):04d}.chunk"
            digest = hashlib.sha256()
            written = 0
            with part_file.open("wb") as out:
                while written < remaining:
                    data = stream.read(min(1024 * 1024, remaining-written))
                    if not data:
                        raise ValueError("Premature end of official model download")
                    out.write(data)
                    digest.update(data)
                    full_digest.update(data)
                    written += len(data)
                    total += len(data)
            chunk_specs.append({
                "path": f"{model['id']}/{part_file.name}",
                "bytes": written,
                "sha256": digest.hexdigest(),
            })
        if stream.read(1):
            raise ValueError("Official source contains more bytes than pinned")
    if total != model["bytes"] or full_digest.hexdigest() != model["sha256"]:
        raise ValueError(f"Official model SHA-256 mismatch: {model['id']}")
    return {
        "id": model["id"],
        "filename": model["filename"],
        "component": model["component"],
        "bytes": model["bytes"],
        "sha256": model["sha256"],
        "license": model["license"],
        "upstream": model["repository"],
        "parts": chunk_specs,
    }


def prepare(spec: dict, output: Path, offline: Path | None = None) -> dict:
    target = output / "asterion_bundle"
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True, exist_ok=True)
    models = []
    try:
        for model in spec["models"]:
            models.append(write_model(model, target / model["id"],
                                      spec["chunk_bytes"], offline))
            print("Verified embedded model", model["id"],
                  "bytes", model["bytes"], flush=True)
        manifest = {
            "schema": "asterion.embedded-model-pack.v1",
            "chunk_bytes": spec["chunk_bytes"],
            "total_bytes": sum(m["bytes"] for m in models),
            "models": models,
            "offline_after_install": True,
            "on_device_download_required": False,
            "image_diffusion_checkpoint_included": False,
        }
        (target / "manifest.json").write_text(
            json.dumps(manifest, indent=2), encoding="utf-8"
        )
        validate(output, spec)
        return manifest
    except BaseException:
        shutil.rmtree(target, ignore_errors=True)
        raise


def validate(output: Path, spec: dict) -> dict:
    target = output / "asterion_bundle"
    manifest = json.loads((target / "manifest.json").read_text(encoding="utf-8"))
    if manifest.get("schema") != "asterion.embedded-model-pack.v1":
        raise ValueError("Wrong installed bundle schema")
    by_id = {m["id"]: m for m in manifest["models"]}
    if set(by_id) != {m["id"] for m in spec["models"]}:
        raise ValueError("Bundled models do not match pinned reviewed sources")
    accounted = 0
    for source in spec["models"]:
        item = by_id[source["id"]]
        if any(item[field] != source[field] for field in (
                "filename", "bytes", "sha256", "component", "license")):
            raise ValueError("Unexpected change in model sources")
        combined = hashlib.sha256()
        size = 0
        for index, entry in enumerate(item["parts"]):
            expected = f"{source['id']}/part-{index:04d}.chunk"
            if entry["path"] != expected:
                raise ValueError("Unsafe or noncontinuous bundle chunk")
            chunk = target / expected
            if not chunk.is_file() or chunk.stat().st_size != entry["bytes"]:
                raise ValueError("Missing or mismatched model chunk")
            if not (0 < entry["bytes"] <= CHUNK_MAX):
                raise ValueError("Unbounded model asset")
            digest = hashlib.sha256()
            with chunk.open("rb") as reader:
                while content := reader.read(4 * 1024 * 1024):
                    combined.update(content)
                    digest.update(content)
            if digest.hexdigest() != entry["sha256"]:
                raise ValueError("Model asset checksum mismatch")
            size += entry["bytes"]
        if size != source["bytes"] or combined.hexdigest() != source["sha256"]:
            raise ValueError("Bundled complete model checksum mismatch")
        accounted += size
    if accounted != manifest["total_bytes"]:
        raise ValueError("Incorrect complete bundle size")
    return manifest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-manifest", type=Path, default=DEFAULT_SPEC)
    parser.add_argument("--output", type=Path,
                        default=Path("app/build/generated/embeddedModelsAssets"))
    parser.add_argument("--offline-input-dir", type=Path)
    parser.add_argument("--verify-only", action="store_true")
    args = parser.parse_args()
    sources = read_specs(args.source_manifest)
    if args.verify_only:
        result = validate(args.output, sources)
    else:
        result = prepare(sources, args.output, args.offline_input_dir)
    print(json.dumps({
        "bundle": "offline-integrated",
        "model_count": len(result["models"]),
        "total_bytes": result["total_bytes"],
        "image_diffusion_checkpoint_included": False,
    }, indent=2))


if __name__ == "__main__":
    main()

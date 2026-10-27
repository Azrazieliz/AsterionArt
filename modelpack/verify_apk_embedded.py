#!/usr/bin/env python3
"""Verify actual built APK embeds *complete* approved neural model bytes.

Reads ZIP members incrementally: no RAM-sized model buffers or unverified
manifest accepted. This is a packaging check, not an inference/device test.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import zipfile

from prepare_embedded_models import read_specs

def verify(apk: Path, spec_path: Path) -> dict:
    sources = read_specs(spec_path)
    with zipfile.ZipFile(apk) as package:
        try:
            manifest = json.loads(
                package.read("assets/asterion_bundle/manifest.json")
            )
        except KeyError as exc:
            raise ValueError("APK was assembled WITHOUT bundled neural weights") from exc
        if manifest.get("schema") != "asterion.embedded-model-pack.v1":
            raise ValueError("Unexpected model-pack schema")
        if (manifest.get("offline_after_install") is not True
                or manifest.get("on_device_download_required") is not False):
            raise ValueError("APK does not declare an entirely local model install")
        # No image-generation tensor is in the reviewed three-weight baseline.
        # A boolean claim cannot substitute for a pinned QNN checkpoint.
        if manifest.get("image_diffusion_checkpoint_included") is not False:
            raise ValueError("Unverified image checkpoint inclusion claim")
        if manifest.get("total_bytes") != sum(
                item["bytes"] for item in sources["models"]):
            raise ValueError("APK embedded byte budget does not match exact source")
        models = {item["id"]: item for item in manifest["models"]}
        if set(models) != {item["id"] for item in sources["models"]}:
            raise ValueError("Incomplete or altered packaged model selection")
        count=0
        for source in sources["models"]:
            model=models[source["id"]]
            if any(model[field]!=source[field]
                   for field in ("filename","bytes","sha256","component","license")):
                raise ValueError("Incorrect packaged model checkpoint")
            chunks=model["parts"]
            if not chunks:
                raise ValueError("Missing model chunks")
            full=hashlib.sha256()
            total=0
            for i,part in enumerate(chunks):
                expected=f"{source['id']}/part-{i:04d}.chunk"
                if part["path"]!=expected:
                    raise ValueError("Out-of-order model chunks in APK")
                member="assets/asterion_bundle/"+part["path"]
                chunk_info=package.getinfo(member)
                if chunk_info.file_size!=part["bytes"]:
                    raise ValueError("Invalid embedded chunk size")
                digest=hashlib.sha256()
                with package.open(member) as stream:
                    while data:=stream.read(1024*1024):
                        full.update(data)
                        digest.update(data)
                        total+=len(data)
                if digest.hexdigest()!=part["sha256"]:
                    raise ValueError("APK model chunk checksum mismatch")
                count+=1
            if total!=source["bytes"] or full.hexdigest()!=source["sha256"]:
                raise ValueError("APK full neural model SHA-256 mismatch")
        return {
            "verified_apk":str(apk),
            "models":len(models),
            "chunks":count,
            "embedded_bytes":manifest["total_bytes"],
            "release_signed":False,
            "real_device_tested":False,
            "image_diffusion_weights_included":False,
        }

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--apk",type=Path,required=True)
    parser.add_argument("--sources",type=Path,
                        default=Path(__file__).with_name("models.json"))
    args=parser.parse_args()
    print(json.dumps(verify(args.apk,args.sources),indent=2))

if __name__=="__main__":
    main()

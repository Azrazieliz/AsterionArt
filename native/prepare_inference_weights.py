#!/usr/bin/env python3
"""Prepare ONLY Qwen3 reasoning and Qwen3-VL inference assets for release.
Never embeds SDXL/Anima/FLUX/image or video generation checkpoints.
Every byte is SHA-256 checked before inclusion in a local-only release APK.
"""
from __future__ import annotations
import argparse
import hashlib
import os
from pathlib import Path
import urllib.request

WEIGHTS = (
    ("Qwen_Qwen3-0.6B-Q5_K_M.gguf",
     "bartowski/Qwen_Qwen3-0.6B-GGUF",
     551378336, "9d017e676272ce914f9dc65d959db574bedf71c2b4bfb882eb620f6fcc48ec17"),
    ("Qwen3VL-2B-Instruct-Q4_K_M.gguf",
     "Qwen/Qwen3-VL-2B-Instruct-GGUF",
     1107409952, "06fb8948e506b2e751a0f81f6e365382a3b9ea5f304ade3c77a814adc9543109"),
    ("mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf",
     "Qwen/Qwen3-VL-2B-Instruct-GGUF",
     445053216, "df4d5f12ac5349b780b8a2d5033e9b529c14a82a30c5b69bbdcef52b79a56ff3"),
)

def digest(path: Path) -> str:
    hashed=hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda:stream.read(1024*1024),b""):
            hashed.update(chunk)
    return hashed.hexdigest()

def prepare(project: Path) -> None:
    dest=project/"app/src/main/assets/inference"
    dest.mkdir(parents=True,exist_ok=True)
    for name,repository,size,sha in WEIGHTS:
        path=dest/name
        if path.is_file() and (size is None or path.stat().st_size==size) and digest(path).lower()==sha.lower():
            print(f"Already verified: {name}",flush=True)
            continue
        temp=path.with_name(path.name+".download")
        if temp.exists():
            temp.unlink()
        url=f"https://huggingface.co/{repository}/resolve/main/{name}?download=true"
        req=urllib.request.Request(url,headers={"User-Agent":"AsterionArt-offline-release/1.0"})
        print(f"Retrieving verified inference model: {name}",flush=True)
        with urllib.request.urlopen(req,timeout=120) as inp, temp.open("wb") as out:
            for chunk in iter(lambda:inp.read(1024*1024),b""):
                out.write(chunk)
            out.flush()
            os.fsync(out.fileno())
        if size is not None and temp.stat().st_size!=size:
            temp.unlink(missing_ok=True)
            raise RuntimeError(f"Unexpected size for {name}")
        if digest(temp).lower()!=sha.lower():
            temp.unlink(missing_ok=True)
            raise RuntimeError(f"SHA-256 verification failed: {name}")
        temp.replace(path)
        print(f"Verified {name}",flush=True)
    print("Offline reasoning + vision weights ready; no image weights embedded.")

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--project",type=Path,default=Path("."))
    prepare(parser.parse_args().project.resolve())

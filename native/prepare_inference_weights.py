#!/usr/bin/env python3
"""Optional offline verification of a SHARED Gemma 3 4B + projector pair.
Never run as part of APK packaging. User installs models from the app.
Every byte is SHA-256 checked before inclusion in a local-only release APK.
"""
from __future__ import annotations
import argparse
import hashlib
import os
from pathlib import Path
import urllib.request

# Optional offline verification only; Android normally installs via Models.
# The Gemma language GGUF is reused by both reasoning and image analysis.
WEIGHTS = (
    ("google_gemma-3-4b-it-Q4_K_M.gguf",
     "bartowski/google_gemma-3-4b-it-GGUF",2489758112,
     "4996030242583a40aa151ff93f49ed787ac8c25e4120c3ae4588b2e2a7d1ae94"),
    ("mmproj-google_gemma-3-4b-it-f16.gguf",
     "bartowski/google_gemma-3-4b-it-GGUF",851251104,
     "8c0fb064b019a6972856aaae2c7e4792858af3ca4561be2dbf649123ba6c40cb"),
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

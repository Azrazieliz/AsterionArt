#!/usr/bin/env python3
"""Embed MIT-licensed RealSR/Real-ESRGAN Vulkan CLI in Asterion Art.

Downloads official RealSR-NCNN-Android GitHub release, pins SHA256, extracts
only the selected native runtime and model files, and retains attribution.
Does not copy or repackage SuperImage Pro or the Image Toolbox application.
"""
from pathlib import Path
import hashlib, urllib.request, tempfile, zipfile, shutil, json, os

RELEASE = "1.13.2"
URL = ("https://github.com/tumuyan/RealSR-NCNN-Android/releases/download/"
       "1.13.2/RealSR-NCNN-Android-GUI-armv8a-1.13.2.apk")
SHA256 = "d7b68c2589ad1cb9a953ed8ca001f6600217dceb4ee7053bc21e8c3899c4c6e7"
SOURCES = {
    "assets/realsr/realsr-ncnn": "jniLibs/arm64-v8a/libasterion_realsr_cli.so",
    "assets/realsr/libncnn.so": "assets/realsr/libncnn.so",
    "assets/realsr/libc++_shared.so": "assets/realsr/libc++_shared.so",
    "assets/realsr/libomp.so": "assets/realsr/libomp.so",
    "assets/realsr/models-Real-ESRGAN-anime/x4.bin": "assets/realsr/models-Real-ESRGAN-anime/x4.bin",
    "assets/realsr/models-Real-ESRGAN-anime/x4.param": "assets/realsr/models-Real-ESRGAN-anime/x4.param",
    "assets/realsr/models-Real-ESRGANv3-anime/x2.bin": "assets/realsr/models-Real-ESRGANv3-anime/x2.bin",
    "assets/realsr/models-Real-ESRGANv3-anime/x2.param": "assets/realsr/models-Real-ESRGANv3-anime/x2.param",
}
COPYRIGHT = """RealSR-NCNN-Android Copyright (c) tumuyan (2022).
Source: https://github.com/tumuyan/RealSR-NCNN-Android
Licence MIT, https://github.com/tumuyan/RealSR-NCNN-Android/blob/master/LICENSE
RealSR-NCNN Copyright (c) nihui (2019), MIT.
Model assets may have separate terms; retain upstream attribution and provenance.
No SuperImage Pro assets, APK, engines or marketplace entitlements are included.
"""

def hash_stream(reader, writer=None):
    h=hashlib.sha256()
    while True:
        block=reader.read(1024*1024)
        if not block:break
        h.update(block)
        if writer:writer.write(block)
    return h.hexdigest()

def install(project:Path, archived_apk:Path):
    with archived_apk.open("rb") as f:
        got=hash_stream(f)
    if got != SHA256:
        raise RuntimeError("Official RealSR APK checksum mismatch: "+got)
    root=project/"app/src/main"
    inventory={}
    with zipfile.ZipFile(archived_apk) as apk:
        for source,target in SOURCES.items():
            if source not in apk.namelist():raise RuntimeError("Missing native RealSR asset: "+source)
            info=apk.getinfo(source)
            if info.file_size > 75*1024*1024:raise RuntimeError("Unreasonable size: "+source)
            dest=root/target
            dest.parent.mkdir(parents=True,exist_ok=True)
            with apk.open(source) as inp, dest.open("wb") as out:
                digest=hash_stream(inp,out)
            inventory[target]={"sha256":digest,"size":info.file_size,"source":source}
    licenses=root/"assets/licenses"
    licenses.mkdir(parents=True,exist_ok=True)
    (licenses/"RealSR-NCNN-Android-MIT.txt").write_text(COPYRIGHT,encoding="utf-8")
    (root/"assets/realsr/embedded_realsr.json").write_text(
        json.dumps({"source":URL,"source_sha256":got,"version":RELEASE,
                    "files":inventory},indent=2))
    return inventory

def main():
    import argparse
    parser=argparse.ArgumentParser()
    parser.add_argument("--project",default=".")
    parser.add_argument("--apk")
    a=parser.parse_args()
    if a.apk: inventory=install(Path(a.project),Path(a.apk))
    else:
        with tempfile.TemporaryDirectory(prefix="asterion-realsr-") as folder:
            path=Path(folder)/"realsr.apk"
            urllib.request.urlretrieve(URL,str(path))
            inventory=install(Path(a.project),path)
    print("RealSR embedded:",len(inventory),"files;",
        round(sum(v["size"] for v in inventory.values())/1048576,2),"MiB")

if __name__=="__main__":main()

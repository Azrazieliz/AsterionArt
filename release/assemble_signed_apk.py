#!/usr/bin/env python3
"""Reconstruct one already-signed APK from its original exact GitHub artifact.
Only a PUBLIC APK v2 signature block is in Git. No private key is uploaded.
Fails closed on any artifact or signature mismatch.
"""
import argparse,hashlib,os,struct,tempfile,zipfile
from pathlib import Path

UNSIGNED="3f01f33b82a7bed91e3ab4e5af498e19ce13a9739743a31fab37293614a652c0"
SIGNED="21b244417ecbe4b2cb4cb5facc29d2fffbe5fde7df6dbeab9de9bcd24f0f0461"
CERT="41f5c305f3756fcbfb7fea207f670ce22a4f3c91044e953e4ed8277fe91146fb"
BLOCK="498c7eeb6b7ca4ffa016621421aafed1303f23a4980f4acf9624bdadf7a46296"
OFFSET=300814580
MAGIC=b"APK Sig Block 42"
def digest(p):
    h=hashlib.sha256()
    with p.open("rb") as f:
        for chunk in iter(lambda:f.read(1048576),b""):h.update(chunk)
    return h.hexdigest()
def eocd(p):
    n=p.stat().st_size
    with p.open("rb") as f:
        f.seek(max(0,n-65557));data=f.read()
    i=data.rfind(b"PK\x05\x06")
    while i>=0:
        if i+22<=len(data) and i+22+struct.unpack_from("<H",data,i+20)[0]==len(data):
            return data[i:]
        i=data.rfind(b"PK\x05\x06",0,i)
    raise ValueError("Missing source APK ZIP central directory")
def exact_copy(src,dst,n):
    while n:
        chunk=src.read(min(n,4194304))
        if not chunk:raise ValueError("Truncated original APK")
        dst.write(chunk);n-=len(chunk)
def reconstruct(source,patch,target):
    if digest(source)!=UNSIGNED:raise ValueError("Original GitHub APK checksum mismatch")
    tail=eocd(source)
    cd=struct.unpack_from("<I",tail,16)[0]
    cd_size=struct.unpack_from("<I",tail,12)[0]
    if cd!=OFFSET or cd+cd_size+len(tail)!=source.stat().st_size:
        raise ValueError("Source central directory differs")
    block=patch.read_bytes()
    if (len(block)<64 or block[-16:]!=MAGIC or
        struct.unpack_from("<Q",block,0)[0]!=len(block)-8 or
        struct.unpack_from("<Q",block,-24)[0]!=len(block)-8 or
        hashlib.sha256(block).hexdigest()!=BLOCK):
        raise ValueError("Public signature block corrupt")
    tail=tail[:16]+struct.pack("<I",cd+len(block))+tail[20:]
    target.parent.mkdir(parents=True,exist_ok=True)
    fd,temp=tempfile.mkstemp(prefix="asterion-signed-",suffix=".apk.part",dir=target.parent)
    try:
        with os.fdopen(fd,"wb") as out,source.open("rb") as inp:
            exact_copy(inp,out,cd)
            out.write(block)
            exact_copy(inp,out,source.stat().st_size-cd-len(tail))
            out.write(tail)
            out.flush();os.fsync(out.fileno())
        path=Path(temp)
        if digest(path)!=SIGNED:raise ValueError("Signed APK differs from release original")
        with zipfile.ZipFile(path) as z:
            bad=z.testzip()
            if bad:raise ValueError("Corrupt ZIP entry: "+bad)
        os.replace(temp,target)
    finally:
        if os.path.exists(temp):os.remove(temp)
    print("Original signed APK reconstructed; SHA256: "+SIGNED)
    print("Pinned public signing certificate SHA256: "+CERT)
if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--source",required=True,type=Path)
    p.add_argument("--patch",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    reconstruct(a.source,a.patch,a.output)

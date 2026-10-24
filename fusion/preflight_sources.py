#!/usr/bin/env python3
"""Fusion B preflight: resolve exact source revisions and raw file checksums.

This downloads metadata only, not any 4–7 GB checkpoint file.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
from urllib.parse import quote
from urllib.request import Request,urlopen

MANIFEST=Path(__file__).with_name("fusion_b_sources.json")

def request(url:str,method:str="GET"):
    req=Request(url,method=method,headers={"User-Agent":"Asterion-Fusion-B/1.0"})
    with urlopen(req,timeout=50) as resp:
        if method=="HEAD":return dict(resp.headers)
        return json.load(resp)

def metadata(model:dict)->dict:
    repo=model["id"];revision=model["revision"];name=model["file"]
    base="https://huggingface.co/"
    info=request(base+"api/models/"+quote(repo,safe="/")+"/revision/"+
                 quote(revision,safe=""))
    commit=info.get("sha")
    if not isinstance(commit,str) or len(commit)!=40:
        raise ValueError("Unable to lock revision for "+repo)
    folder="/".join(name.split("/")[:-1])
    listing=base+"api/models/"+quote(repo,safe="/")+"/tree/"+commit
    if folder:listing+="/"+quote(folder,safe="/")
    listing+="?expand=true"
    files=request(listing)
    record=next((x for x in files if x.get("path")==name),None)
    if not record:raise ValueError("Expected model file unavailable: "+repo+"/"+name)
    size=int(record.get("size") or record.get("lfs",{}).get("size") or 0)
    if size<2_000_000_000:
        raise ValueError("Checkpoint file is implausibly small: "+repo+"/"+name)
    checksum=(record.get("lfs") or {}).get("oid","")
    url=base+repo+"/resolve/"+commit+"/"+quote(name,safe="/")
    if len(checksum.removeprefix("sha256:"))!=64:
        head=request(url,"HEAD")
        checksum=(head.get("X-Linked-Etag") or head.get("ETag") or "").strip('"')
    checksum=checksum.removeprefix("sha256:")
    if len(checksum)!=64 or not all(c in "0123456789abcdef" for c in checksum.lower()):
        raise ValueError("Raw file SHA256 not available: "+repo)
    expectation=model.get("sha256")
    if expectation and expectation!=checksum:
        raise ValueError("Pinned SHA mismatch: "+repo)
    return {"repo_id":repo,"commit":commit,"filename":name,
            "sha256":checksum,"size":size,"url":url}

def preflight()->dict:
    specs=json.loads(MANIFEST.read_text())
    result={"schema":"asterion.fusion-b.lock.v1",
            "algorithm":specs["algorithm"],
            "alpha_miaomiao":specs["prototype_weights"]["miaomiao"],
            "sources":{},"merge_status":"NOT_STARTED",
            "fused_checkpoint_published":False,
            "hassaku_is_same_architecture":False}
    for model in ("rin","miaomiao","hassaku"):
        result["sources"][model]=metadata(specs[model])
    return result

if __name__=="__main__":
    args=argparse.ArgumentParser()
    args.add_argument("--output",type=Path)
    p=args.parse_args()
    locked=json.dumps(preflight(),indent=2)
    print(locked)
    if p.output:p.output.write_text(locked)

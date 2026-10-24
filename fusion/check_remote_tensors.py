#!/usr/bin/env python3
"""Preflight original Anima tensor structure with bounded HTTP range requests.

This never downloads a multi-GB checkpoint. All reads have a hard cap and
non-range-capable mirrors are refused.
"""
from __future__ import annotations

import json
import struct
from urllib.request import Request, urlopen
from preflight_sources import preflight

MAX_HEADER_BYTES=24*1024*1024


def fetch_range(url:str,start:int,end:int)->bytes:
    if end<start or end-start+1>MAX_HEADER_BYTES:
        raise ValueError("Header range limit exceeded")
    req=Request(url,headers={
        "Range":f"bytes={start}-{end}",
        "Accept-Encoding":"identity",
        "User-Agent":"Asterion-Fusion-B-preflight/1"
    })
    with urlopen(req,timeout=90) as response:
        if response.status!=206:
            raise ValueError("Source server does not support bounded HTTP ranges")
        content_range=response.headers.get("Content-Range","")
        if not content_range.startswith(f"bytes {start}-{end}/"):
            raise ValueError("Unexpected Content-Range: "+content_range)
        data=response.read(end-start+2)
        if len(data)!=end-start+1:
            raise ValueError("Incomplete header range")
        return data


def tensor_signature(source:dict)->dict:
    url=source["url"]
    length=struct.unpack("<Q",fetch_range(url,0,7))[0]
    if length<16 or length>MAX_HEADER_BYTES:
        raise ValueError("Unreasonable safetensors header byte count")
    header=json.loads(fetch_range(url,8,8+length-1))
    tensors={k:v for k,v in header.items() if k!="__metadata__"}
    if len(tensors)<300:
        raise ValueError("Too few tensors for full original Anima UNet")
    key_types={key:(v["dtype"],tuple(v["shape"])) for key,v in tensors.items()}
    return {"tensor_count":len(key_types),
            "dtypes":sorted(set(dtype for dtype,_ in key_types.values())),
            "signature":key_types}


def validate()->dict:
    locks=preflight()
    rin=tensor_signature(locks["sources"]["rin"])
    miao=tensor_signature(locks["sources"]["miaomiao"])
    a=rin.pop("signature")
    b=miao.pop("signature")
    keys_a=set(a)
    keys_b=set(b)
    matching_shapes=keys_a==keys_b and all(a[x][1]==b[x][1] for x in keys_a)
    matching_dtypes=matching_shapes and all(a[x][0]==b[x][0] for x in keys_a)
    result={"schema":"asterion.fusion-b.tensor-preflight.v1",
            "rin_tensor_count":rin["tensor_count"],
            "miaomiao_tensor_count":miao["tensor_count"],
            "rin_dtypes":rin["dtypes"],"miaomiao_dtypes":miao["dtypes"],
            "matching_key_and_shapes":matching_shapes,
            "same_tensor_dtypes":matching_dtypes,
            "missing_in_miao":sorted(keys_a-keys_b)[:8],
            "missing_in_rin":sorted(keys_b-keys_a)[:8],
            "direct_merge_ready":matching_shapes and matching_dtypes}
    if not result["direct_merge_ready"]:
        # Existing merge tool requires matching input dtypes. If tensors
        # are shape-identical but BF16 vs FP16, we can add an explicit,
        # audited dtype casting preprocessor in the following change.
        return result
    return result


if __name__=="__main__":
    report=validate()
    print(json.dumps(report,indent=2))
    with open("fusion_b_tensor_preflight.json","w") as output:
        json.dump(report,output,indent=2)

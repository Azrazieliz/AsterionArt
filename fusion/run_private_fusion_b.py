#!/usr/bin/env python3
"""Fusion B: a reproducible, private-only, original-weight Anima merge.

Do NOT run on compiled QNN binary weights, do NOT publish the merged model to
a public repository. This stream-merges compatible original Anima checkpoint
weights and keeps a verifiable lineage manifest.
"""
# Requires: pip install numpy huggingface_hub hf_xet
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import tempfile
from huggingface_hub import HfApi, hf_hub_download
from merge_anima import merge, header
from preflight_sources import preflight


def sha256(path: Path) -> str:
    h=hashlib.sha256()
    with path.open("rb") as handle:
        while chunk:=handle.read(16*1024*1024):h.update(chunk)
    return h.hexdigest()


def download_source(model: dict) -> Path:
    location=Path(hf_hub_download(
        repo_id=model["repo_id"],filename=model["filename"],
        revision=model["commit"],repo_type="model"
    ))
    real=sha256(location)
    if real!=model["sha256"]:
        raise ValueError("Downloaded original checkpoint checksum mismatch: "+model["repo_id"])
    return location


def generate(private_repo:str,alpha:float=0.30)->dict:
    if not private_repo or "/" not in private_repo:raise ValueError("Private model destination required")
    token=os.environ.get("HF_TOKEN")
    if not token:raise ValueError("HF_TOKEN not configured for private upload")
    api=HfApi(token=token)
    info=api.repo_info(repo_id=private_repo,repo_type="model")
    if not info.private:
        raise ValueError("Refusing to upload fused model to a public Hugging Face repository")

    lock=preflight()
    if lock["sources"]["rin"]["sha256"]!=lock["sources"]["miaomiao"]["sha256"]:
        with tempfile.TemporaryDirectory(prefix="asterion-fusion-b-") as work:
            work=Path(work)
            rin=download_source(lock["sources"]["rin"])
            miao=download_source(lock["sources"]["miaomiao"])
            # merge() validates identical tensor keys, shapes, and dtypes before
            # writing the first data block, so incompatible architectures fail closed.
            output=work/"asterion_fusion_b_anima_f16.safetensors"
            stats=merge(rin,miao,output,alpha,chunk_mb=8)
            _,specs=header(output)
            if len(specs)<300:
                raise ValueError("Merged model has too few tensors for Anima")
            lineage={
                "schema":"asterion.fusion-b.provenance.v1",
                "architecture":"Anima, NOT Illustrious SDXL",
                "status":"MERGED_NOT_NPU_CONVERTED_NOT_QUALITY_BENCHMARKED",
                "alpha_miaomiao":alpha,
                "source_models":lock["sources"],
                "result_sha256":stats["sha256"],
                "result_bytes":stats["bytes"],
                "tensor_count":len(specs),
                "hassaku":"SDXL-only reference; not included in tensor arithmetic",
                "licensing":"Personal noncommercial research; no public redistribution",
            }
            (work/"asterion_fusion_b_provenance.json").write_text(json.dumps(lineage,indent=2))
            # Preserve private visibility and original author attribution.
            api.upload_file(path_or_fileobj=str(output),
                path_in_repo=output.name,repo_id=private_repo,repo_type="model",
                commit_message="Add private Anima Fusion B weighted checkpoint")
            api.upload_file(path_or_fileobj=str(work/"asterion_fusion_b_provenance.json"),
                path_in_repo="asterion_fusion_b_provenance.json",
                repo_id=private_repo,repo_type="model",
                commit_message="Record Fusion B input checksums and merge provenance")
            return lineage
    raise ValueError("Source checkpoints are duplicates; no meaningful fusion")


def main():
    p=argparse.ArgumentParser()
    p.add_argument("--private-repo",required=True)
    p.add_argument("--miaomiao-weight",type=float,default=0.30)
    args=p.parse_args()
    if not 0.05<=args.miaomiao_weight<=0.5:
        raise ValueError("MiaoMiao weight outside evaluated search interval")
    result=generate(args.private_repo,args.miaomiao_weight)
    print(json.dumps({"status":result["status"],
         "sha256":result["result_sha256"],
         "size":result["result_bytes"],
         "private_repo":args.private_repo},indent=2))


if __name__=="__main__":main()

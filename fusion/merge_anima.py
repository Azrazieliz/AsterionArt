#!/usr/bin/env python3
"""Stream-merge two compatible original Anima safetensors without loading 4+ GB RAM.

Use with original trainable checkpoint weights, NOT compiled QNN binaries.
Never mix Anima tensors with Illustrious/SDXL tensors.
Authorised private merging only; comply with source model permissions.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import struct

import numpy as np

DTYPE_SIZE = {"F16": 2, "BF16": 2, "F32": 4}


def header(path: Path):
    with path.open("rb") as handle:
        start = handle.read(8)
        if len(start) != 8:
            raise ValueError("Truncated safetensors header")
        length = struct.unpack("<Q", start)[0]
        if length < 2 or length > 100_000_000:
            raise ValueError("Implausible safetensors header length")
        raw = handle.read(length)
        if len(raw) != length:
            raise ValueError("Truncated safetensors metadata")
        data = json.loads(raw)
    base = 8 + length
    tensors = {key: value for key, value in data.items() if key != "__metadata__"}
    if not tensors:
        raise ValueError("No tensor entries")
    spans = []
    for key, spec in tensors.items():
        dtype = spec["dtype"]
        if dtype not in DTYPE_SIZE:
            raise ValueError(f"Unsupported dtype for {key}: {dtype}")
        shape = spec["shape"]
        start_offset, end_offset = spec["data_offsets"]
        n_elements = 1
        for size in shape:
            if not isinstance(size, int) or size <= 0:
                raise ValueError(f"Invalid tensor shape: {key}")
            n_elements *= size
        if end_offset - start_offset != n_elements * DTYPE_SIZE[dtype]:
            raise ValueError(f"Tensor byte size does not match shape: {key}")
        spans.append((start_offset, end_offset))
    spans.sort()
    for prev, following in zip(spans, spans[1:]):
        if prev[1] > following[0]:
            raise ValueError("Overlapping safetensors offsets")
    if max(x[1] for x in spans) + base > path.stat().st_size:
        raise ValueError("Truncated tensor storage")
    return base, tensors


def interpolate(a: bytes, b: bytes, kind: str, alpha: float) -> bytes:
    if len(a) != len(b):
        raise ValueError("Chunk length mismatch")
    if kind == "F16":
        lhs = np.frombuffer(a, dtype="<f2").astype("<f4")
        rhs = np.frombuffer(b, dtype="<f2").astype("<f4")
        return ((1 - alpha) * lhs + alpha * rhs).astype("<f2").tobytes()
    if kind == "F32":
        lhs = np.frombuffer(a, dtype="<f4")
        rhs = np.frombuffer(b, dtype="<f4")
        return ((1 - alpha) * lhs + alpha * rhs).astype("<f4").tobytes()
    if kind == "BF16":
        lhs = (np.frombuffer(a, dtype="<u2").astype("<u4") << 16).view("<f4")
        rhs = (np.frombuffer(b, dtype="<u2").astype("<u4") << 16).view("<f4")
        mixed = ((1 - alpha) * lhs + alpha * rhs).astype("<f4")
        bits = mixed.view("<u4")
        # Round-to-nearest-even before discarding BF16 low bits.
        out = ((bits + 0x7fff + ((bits >> 16) & 1)) >> 16).astype("<u2")
        return out.tobytes()
    raise ValueError("Unsupported dtype")


def merge(rin: Path, miao: Path, dest: Path, alpha: float, chunk_mb: int = 8):
    if not (0 < alpha < 1):
        raise ValueError("Alpha must be strictly between zero and one")
    if rin.resolve() == miao.resolve() or dest.resolve() in (rin.resolve(), miao.resolve()):
        raise ValueError("Input and output paths must be distinct")
    base_a, tensors_a = header(rin)
    base_b, tensors_b = header(miao)
    if tensors_a.keys() != tensors_b.keys():
        raise ValueError("Different tensor keys. Likely incompatible checkpoint architectures")
    for key in tensors_a:
        a, b = tensors_a[key], tensors_b[key]
        if a["dtype"] != b["dtype"] or a["shape"] != b["shape"]:
            raise ValueError(f"Incompatible tensor: {key}")

    offset = 0
    entries = {}
    for key in sorted(tensors_a):
        tensor = tensors_a[key]
        size = tensor["data_offsets"][1] - tensor["data_offsets"][0]
        entries[key] = {
            "dtype": tensor["dtype"],
            "shape": tensor["shape"],
            "data_offsets": [offset, offset + size]
        }
        offset += size
    entries["__metadata__"] = {
        "asterion_fusion": "rin_flanime+miaomiao",
        "alpha_miaomiao": str(alpha),
        "inference_compatibility": "not-tested",
        "quantized": "false"
    }
    raw = json.dumps(entries, separators=(",", ":")).encode()
    raw += b" " * ((-len(raw)) % 8)
    chunk_bytes = max(1, chunk_mb) * 1024 * 1024
    digest = hashlib.sha256()

    dest.parent.mkdir(parents=True, exist_ok=True)
    temporary = dest.with_name(dest.name + ".part")
    try:
        with rin.open("rb") as fa, miao.open("rb") as fb, temporary.open("wb") as output:
            first = struct.pack("<Q", len(raw))
            output.write(first)
            output.write(raw)
            digest.update(first)
            digest.update(raw)
            for key in sorted(tensors_a):
                a_spec, b_spec = tensors_a[key], tensors_b[key]
                kind = a_spec["dtype"]
                word_size = DTYPE_SIZE[kind]
                step = max(word_size, chunk_bytes - (chunk_bytes % word_size))
                apos = base_a + a_spec["data_offsets"][0]
                bpos = base_b + b_spec["data_offsets"][0]
                remain = a_spec["data_offsets"][1] - a_spec["data_offsets"][0]
                fa.seek(apos)
                fb.seek(bpos)
                while remain:
                    size = min(remain, step)
                    x, y = fa.read(size), fb.read(size)
                    if len(x) != size or len(y) != size:
                        raise ValueError("Truncated input tensor")
                    merged = interpolate(x, y, kind, alpha)
                    output.write(merged)
                    digest.update(merged)
                    remain -= size
            output.flush()
        if temporary.stat().st_size != 8 + len(raw) + offset:
            raise ValueError("Merged file size mismatch")
        temporary.replace(dest)
    except BaseException:
        temporary.unlink(missing_ok=True)
        raise
    return {"path": str(dest), "sha256": digest.hexdigest(),
            "alpha_miaomiao": alpha, "bytes": dest.stat().st_size}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--rin", required=True, type=Path)
    parser.add_argument("--miaomiao", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--miaomiao-weight", type=float, default=0.3)
    parser.add_argument("--chunk-mb", type=int, default=8)
    args = parser.parse_args()
    print(json.dumps(merge(args.rin,args.miaomiao,args.output,
                           args.miaomiao_weight,args.chunk_mb), indent=2))


if __name__ == "__main__":
    main()

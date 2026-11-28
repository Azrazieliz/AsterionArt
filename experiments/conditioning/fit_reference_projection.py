"""Fit a compact vision encoder to the SD 1.5 IP-Adapter image input."""

import argparse
import hashlib
import json
import time
from collections import defaultdict
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
import torch
from onnx import TensorProto, helper, numpy_helper
from PIL import Image, ImageOps
from transformers import CLIPVisionModelWithProjection


MEAN = np.array([0.48145466, 0.4578275, 0.40821073], dtype=np.float32)
STD = np.array([0.26862954, 0.26130258, 0.27577711], dtype=np.float32)


def normalise(vectors):
    norms = np.linalg.norm(vectors, axis=-1, keepdims=True)
    return vectors / np.maximum(norms, 1e-12)


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def pixel_values(images):
    pixels = [
        np.asarray(ImageOps.fit(image.convert("RGB"), (224, 224), Image.Resampling.BICUBIC))
        for image in images
    ]
    return ((np.asarray(pixels, dtype=np.float32) / 255 - MEAN) / STD).transpose(0, 3, 1, 2).copy()


def split(rows, limit):
    by_motif = defaultdict(list)
    seen_titles = set()
    seen_digests = set()
    for row in rows:
        if row["license"] != "CC0-1.0" or not row.get("url") or not row.get("sha256"):
            raise ValueError("Every training record must identify a verified CC0 original")
        title = row["title"].strip().casefold()
        if title in seen_titles or row["sha256"] in seen_digests:
            continue
        seen_titles.add(title)
        seen_digests.add(row["sha256"])
        by_motif[row["term"]].append(row)

    training, held_out, unseen = [], [], []
    for motif, examples in sorted(by_motif.items()):
        if len(examples) < 3:
            continue
        examples.sort(key=lambda row: hashlib.sha256(row["id"].encode()).digest())
        if motif in {"orchid", "moth", "lantern"}:
            unseen.extend(examples[:limit])
        else:
            held_out.extend(examples[:2])
            training.extend(examples[2:limit + 2])
    if min(len(training), len(held_out), len(unseen)) == 0:
        raise ValueError("The manifest needs training, held-out, and unseen-motif examples")
    return training, held_out, unseen


def examples(rows, directory, compact, teacher):
    inputs, targets = [], []
    for start in range(0, len(rows), 4):
        images = []
        for row in rows[start:start + 4]:
            path = (directory / row["image"]).resolve()
            if path.parent != directory.resolve():
                raise ValueError("A dataset image must be inside the dataset directory")
            with Image.open(path) as image:
                images.append(image.convert("RGB"))
        pixels = pixel_values(images)
        inputs.append(compact.run(["image_embeds"], {"pixel_values": pixels})[0])
        with torch.inference_mode():
            data = torch.from_numpy(pixels).to(dtype=teacher.dtype)
            targets.append(teacher(pixel_values=data).image_embeds.float().numpy())
        if start % 24 == 0:
            print(f"Encoded {min(start + 4, len(rows))}/{len(rows)} examples", flush=True)
    return np.concatenate(inputs), np.concatenate(targets)


def fit(inputs, targets, regularisation):
    x = normalise(inputs).astype(np.float64)
    mean_x, mean_y = x.mean(axis=0), targets.mean(axis=0)
    centered = x - mean_x
    dual = centered @ centered.T + regularisation * np.eye(len(centered))
    weights = centered.T @ np.linalg.solve(dual, targets - mean_y)
    return weights.astype(np.float32), (mean_y - mean_x @ weights).astype(np.float32)


def evaluate(predictions, targets):
    scores = normalise(predictions) @ normalise(targets).T
    ranks = 1 + (scores > scores.diagonal()[:, None]).sum(axis=1)
    return {
        "samples": len(targets),
        "cosine": float(scores.diagonal().mean()),
        "relative_l2": float(np.mean(np.linalg.norm(predictions - targets, axis=1) / np.linalg.norm(targets, axis=1))),
        "top1_retrieval": float((ranks == 1).mean()),
        "mean_reciprocal_rank": float((1 / ranks).mean()),
    }


def export(path, weights, offset):
    graph = helper.make_graph(
        [
            helper.make_node("LpNormalization", ["image_embeds"], ["normalised"], axis=1, p=2),
            helper.make_node("MatMul", ["normalised", "weights"], ["linear"]),
            helper.make_node("Add", ["linear", "offset"], ["ip_adapter_image_embeds"]),
        ],
        "ReferenceProjection",
        [helper.make_tensor_value_info("image_embeds", TensorProto.FLOAT, ["images", weights.shape[0]])],
        [helper.make_tensor_value_info("ip_adapter_image_embeds", TensorProto.FLOAT, ["images", weights.shape[1]])],
        [numpy_helper.from_array(weights, "weights"), numpy_helper.from_array(offset, "offset")],
    )
    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 17)], ir_version=9)
    onnx.checker.check_model(model)
    onnx.save(model, path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--compact-encoder", type=Path, required=True)
    parser.add_argument("--teacher", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--threads", type=int, default=4)
    parser.add_argument("--examples-per-motif", type=int, default=4)
    parser.add_argument("--regularisation", type=float, default=0.08)
    args = parser.parse_args()
    if args.regularisation <= 0 or args.examples_per_motif < 1 or args.threads < 1:
        parser.error("Use positive values for regularisation, examples, and threads")

    args.out.mkdir(parents=True, exist_ok=True)
    splits = split(json.loads(args.manifest.read_text()), args.examples_per_motif)
    options = ort.SessionOptions()
    options.intra_op_num_threads = args.threads
    options.inter_op_num_threads = 1
    compact = ort.InferenceSession(args.compact_encoder, options, providers=["CPUExecutionProvider"])
    torch.set_num_threads(args.threads)
    teacher = CLIPVisionModelWithProjection.from_pretrained(
        args.teacher, local_files_only=True, use_safetensors=True, torch_dtype=torch.bfloat16,
    ).eval()

    start = time.monotonic()
    split_vectors = [examples(group, args.manifest.parent, compact, teacher) for group in splits]
    training_x, training_y = split_vectors[0]
    weights, bias = fit(training_x, training_y, args.regularisation)
    checkpoint = args.out / "reference_projection.onnx"
    export(checkpoint, weights, bias)
    runner = ort.InferenceSession(checkpoint, options, providers=["CPUExecutionProvider"])

    metrics = {}
    for name, (inputs, targets) in zip(("held_out", "unseen_motif"), split_vectors[1:], strict=True):
        prediction = runner.run(None, {"image_embeds": inputs})[0]
        expected = normalise(inputs) @ weights + bias
        metrics[name] = {
            "learned": evaluate(prediction, targets),
            "teacher_mean": evaluate(np.broadcast_to(training_y.mean(axis=0), targets.shape), targets),
            "onnx_parity_max_abs_error": float(np.max(np.abs(prediction - expected))),
        }
    receipt = {
        "purpose": "Fit an image embedding for the real IP-Adapter input space",
        "model_inputs": {"compact_vision": sha256(args.compact_encoder), "teacher": sha256(args.teacher / "model.safetensors")},
        "manifest_sha256": sha256(args.manifest),
        "artifact_sha256": sha256(checkpoint),
        "sample_ids": {key: [row["id"] for row in group] for key, group in zip(("train", "held_out", "unseen_motif"), splits)},
        "regularisation": args.regularisation,
        "metrics": metrics,
        "elapsed_seconds": round(time.monotonic() - start, 3),
    }
    (args.out / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({**metrics, "elapsed_seconds": receipt["elapsed_seconds"]}, indent=2))


if __name__ == "__main__":
    main()

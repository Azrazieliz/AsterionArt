"""Fit a visual reference token directly in a pinned Anima checkpoint's cross-attention space."""

import argparse
import gc
import json
from pathlib import Path
import sys
import time

import numpy as np
from PIL import Image
from safetensors import safe_open
import torch

from fit_reference_projection import evaluate, export, fit, normalise, pixel_values, sha256, split


def visual_features(rows, manifest, encoder, options):
    import onnxruntime as ort

    vision = ort.InferenceSession(encoder, options, providers=["CPUExecutionProvider"])
    features = []
    for row in rows:
        path = (manifest.parent / row["image"]).resolve()
        if path.parent != manifest.parent.resolve():
            raise ValueError("An image refers outside the dataset")
        with Image.open(path) as image:
            tensor = pixel_values([image])
        features.append(vision.run(["image_embeds"], {"pixel_values": tensor})[0][0])
    return np.stack(features)


def native_targets(rows, comfy_source, text_encoder, checkpoint):
    captions = [f"illustration of {row['term']}, {row['title']}" for row in rows]
    return native_context_targets(captions, comfy_source, text_encoder, checkpoint, 1)[:, 0]


def native_context_targets(captions, comfy_source, text_encoder, checkpoint, token_slots):
    sys.path.insert(0, str(comfy_source.resolve()))
    sys.argv = [sys.argv[0], "--cpu", "--disable-all-custom-nodes"]
    import comfy.options
    comfy.options.enable_args_parsing()
    import comfy.model_management
    import comfy.ops
    import folder_paths
    import nodes
    from comfy.ldm.anima.model import LLMAdapter

    folder_paths.add_model_folder_path("text_encoders", str(text_encoder.parent.resolve()), is_default=True)
    clip, = nodes.CLIPLoader().load_clip(text_encoder.name, device="cpu")
    encoded = []
    with torch.inference_mode():
        for i, caption in enumerate(captions):
            conditions, = nodes.CLIPTextEncode().encode(clip, caption)
            hidden, metadata = conditions[0]
            encoded.append((hidden.to(dtype=torch.bfloat16), metadata["t5xxl_ids"].long().unsqueeze(0)))
            if (i + 1) % 20 == 0:
                print(f"Encoded {i + 1}/{len(captions)} native captions", flush=True)
    comfy.model_management.unload_all_models()
    del clip
    gc.collect()

    prefix = "net.llm_adapter."
    with safe_open(checkpoint, framework="pt", device="cpu") as handle:
        adapter_weights = {
            key.removeprefix(prefix): handle.get_tensor(key)
            for key in handle.keys()
            if key.startswith(prefix)
        }
    if not adapter_weights:
        raise ValueError("The pinned checkpoint does not contain an Anima text adapter")
    adapter = LLMAdapter(
        device="cpu", dtype=torch.bfloat16, operations=comfy.ops.disable_weight_init,
    ).eval()
    adapter.load_state_dict(adapter_weights, strict=True)
    del adapter_weights
    with torch.inference_mode():
        contexts = []
        for hidden, tokens in encoded:
            adapted = adapter(hidden, tokens).float()
            slots = torch.nn.functional.adaptive_avg_pool1d(adapted.transpose(1, 2), token_slots)
            contexts.append(slots[0].T.contiguous().numpy())
        return np.stack(contexts)


def main():
    import onnxruntime as ort

    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("manifest", "compact-encoder", "comfy-source", "text-encoder", "checkpoint", "out"):
        parser.add_argument(f"--{key}", type=Path, required=True)
    parser.add_argument("--expected-checkpoint-sha256", required=True)
    parser.add_argument("--threads", type=int, default=2)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    if sha256(args.checkpoint) != args.expected_checkpoint_sha256:
        parser.error("The checkpoint digest did not match")
    start = time.monotonic()
    torch.set_num_threads(args.threads)
    torch.set_num_interop_threads(1)
    options = ort.SessionOptions()
    options.intra_op_num_threads = args.threads
    rows = split(json.loads(args.manifest.read_text()), limit=3)
    all_rows = rows[0] + rows[1] + rows[2]
    vectors = visual_features(all_rows, args.manifest, args.compact_encoder, options)
    print(f"Encoded {len(vectors)} visual references", flush=True)
    targets = native_targets(all_rows, args.comfy_source, args.text_encoder, args.checkpoint)

    training_size = len(rows[0])
    validation_end = training_size + len(rows[1])
    coefficients, bias = fit(vectors[:training_size], targets[:training_size], 0.08)
    artifact = args.out / "anima_reference_projection.onnx"
    export(artifact, coefficients, bias, "anima_reference_tokens", sequence_axis=True)
    graph = ort.InferenceSession(artifact, options, providers=["CPUExecutionProvider"])
    measures = {}
    for name, selected in (
        ("held_out", slice(training_size, validation_end)),
        ("unseen_motif", slice(validation_end, None)),
    ):
        predicted = graph.run(None, {"image_embeds": vectors[selected]})[0][:, 0, :]
        expected = normalise(vectors[selected]) @ coefficients + bias
        reference = targets[selected]
        mean_target = np.broadcast_to(targets[:training_size].mean(axis=0), reference.shape)
        measures[name] = {
            "learned": evaluate(predicted, reference),
            "constant": evaluate(mean_target, reference),
            "onnx_parity_max_abs_error": float(np.max(np.abs(predicted - expected))),
        }
    result = {
        "artifact_sha256": sha256(artifact),
        "checkpoint_sha256": args.expected_checkpoint_sha256,
        "training_samples": training_size,
        "output_shape": ["images", 1, int(targets.shape[-1])],
        "manifest_sha256": sha256(args.manifest),
        "text_encoder_sha256": sha256(args.text_encoder),
        "compact_encoder_sha256": sha256(args.compact_encoder),
        "metrics": measures,
        "seconds": round(time.monotonic() - start, 3),
    }
    (args.out / "receipt.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result), flush=True)


if __name__ == "__main__":
    main()

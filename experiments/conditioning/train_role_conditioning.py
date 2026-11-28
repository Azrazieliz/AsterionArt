"""Fit shared visual features and independent Anima motif and surface heads."""

import argparse
import json
from pathlib import Path
import time

import numpy as np
from onnx import checker
from onnx.reference import ReferenceEvaluator
from PIL import Image
from safetensors import safe_open
from safetensors.torch import save_file
import torch
from transformers import CLIPVisionModelWithProjection, Dinov2Model

from fit_anima_reference_projection import native_context_targets
from fit_reference_projection import evaluate, pixel_values, sha256


ROLES = ("motif", "surface")
SLOTS = 4
WIDTH = 1024


class ReferenceAuthor(torch.nn.Module):
    def __init__(self, width, lifts, means):
        super().__init__()
        rank = lifts.shape[1]
        self.shared = torch.nn.Sequential(torch.nn.Linear(width, 128), torch.nn.GELU(), torch.nn.LayerNorm(128))
        self.motif = torch.nn.Linear(128, rank)
        self.surface = torch.nn.Linear(128, rank)
        self.register_buffer("lifts", torch.from_numpy(lifts))
        self.register_buffer("means", torch.from_numpy(means))

    def codes(self, sources):
        shared = self.shared(torch.nn.functional.normalize(sources, dim=-1))
        return torch.stack((self.motif(shared), self.surface(shared)), dim=1)

    def forward(self, image_embeds):
        codes = self.codes(image_embeds)
        motif = codes[:, 0] @ self.lifts[0] + self.means[0]
        surface = codes[:, 1] @ self.lifts[1] + self.means[1]
        return motif.reshape(-1, SLOTS, WIDTH), surface.reshape(-1, SLOTS, WIDTH)


def reduce_targets(targets, training_size):
    flattened = targets.reshape(len(targets), len(ROLES), -1)
    means = flattened[:training_size].mean(axis=0)
    residual = flattened - means
    factors, explained, targets_in_basis = [], [], []
    for role in range(len(ROLES)):
        _, values, vectors = np.linalg.svd(residual[:training_size, role], full_matrices=False)
        rank = min(32, training_size - 1)
        scale = np.maximum(values[:rank] / np.sqrt(training_size), 0.1)
        factors.append(vectors[:rank] * scale[:, None])
        targets_in_basis.append(residual[:, role] @ vectors[:rank].T / scale)
        explained.append(float(np.sum(values[:rank] ** 2) / np.sum(values ** 2)))
    return np.stack(factors).astype(np.float32), means, np.stack(targets_in_basis, axis=1), explained


def caption(role, row):
    return row["visible"][role]


def anime_partitions(records):
    names = ("train", "held_out", "unseen_motif")
    groups, hashes, ids = {}, set(), set()
    partitions = {name: [] for name in names}
    for row in records:
        if row["domain"] != "anime" or not row["rights"].strip() or not row["provenance"].strip():
            raise ValueError("Anime model fitting requires Anime source evidence and documented permission")
        partition, group = row["partition"], row["source_group"]
        if partition not in partitions or not group or groups.setdefault(group, partition) != partition:
            raise ValueError("Keep the complete source group within one valid data partition")
        if row["sha256"] in hashes or row["id"] in ids or any(not caption(role, row).strip() for role in ROLES):
            raise ValueError("Anime evidence must be unique and annotated for both visual roles")
        hashes.add(row["sha256"])
        ids.add(row["id"])
        partitions[partition].append(row)
    if any(len(rows) < 2 for rows in partitions.values()) or len({row["source_group"] for row in partitions["train"]}) < 2:
        raise ValueError("Provide distinct Anime evidence for fitting, held-out images and held-out motifs")
    return tuple(partitions[name] for name in names)


class VisionReadout(torch.nn.Module):
    def __init__(self, path, family):
        super().__init__()
        self.family = family
        model = CLIPVisionModelWithProjection if family == "clip" else Dinov2Model
        self.model = model.from_pretrained(
            path, local_files_only=True, use_safetensors=(path / "model.safetensors").is_file(), weights_only=True
        ).eval()

    def forward(self, pixel_values):
        if self.family == "clip":
            return self.model(pixel_values=pixel_values).image_embeds
        vision = self.model(pixel_values=pixel_values).last_hidden_state
        patches = vision[:, 1:]
        return torch.cat((vision[:, 0], patches.mean(1), patches.std(1, unbiased=False)), dim=-1)


def source_features(rows, manifest, vision):
    output = []
    for start in range(0, len(rows), 8):
        images = []
        for row in rows[start:start + 8]:
            path = (manifest.parent / row["image"]).resolve()
            if path.parent != manifest.parent.resolve() or sha256(path) != row["sha256"]:
                raise ValueError(f"Visual training source {row['id']} changed")
            with Image.open(path) as image:
                images.append(image.convert("RGB"))
        with torch.inference_mode():
            pixels = pixel_values(images)
            if vision.family == "dinov2":
                pixels = (pixels * np.array([0.26862954, 0.26130258, 0.27577711], dtype=np.float32)[None, :, None, None]
                          + np.array([0.48145466, 0.4578275, 0.40821073], dtype=np.float32)[None, :, None, None])
                pixels = (pixels - np.array([0.485, 0.456, 0.406], dtype=np.float32)[None, :, None, None]) / np.array(
                    [0.229, 0.224, 0.225], dtype=np.float32
                )[None, :, None, None]
            output.append(vision(torch.from_numpy(pixels)).numpy())
    return np.concatenate(output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("manifest", "vision-model", "comfy-source", "text-encoder", "checkpoint", "out"):
        parser.add_argument(f"--{key}", required=True, type=Path)
    parser.add_argument("--expected-checkpoint-sha256", required=True)
    parser.add_argument("--detached-adapter-sha256")
    parser.add_argument("--expected-vision-sha256", required=True)
    parser.add_argument("--vision-family", choices=("clip", "dinov2"), default="clip")
    parser.add_argument("--threads", type=int, default=3)
    args = parser.parse_args()
    start = time.monotonic()
    checkpoint_digest = sha256(args.checkpoint)
    if args.detached_adapter_sha256:
        if checkpoint_digest != args.detached_adapter_sha256:
            parser.error("The detached Anima adapter digest did not match")
        with safe_open(args.checkpoint, framework="pt", device="cpu") as adapter:
            parent = (adapter.metadata() or {}).get("original_diffusion_sha256")
            if parent != args.expected_checkpoint_sha256 or any(not key.startswith("net.llm_adapter.") for key in adapter.keys()):
                parser.error("The detached Anima adapter has a different source checkpoint")
    elif checkpoint_digest != args.expected_checkpoint_sha256:
        parser.error("The checkpoint digest did not match")
    vision_source = args.vision_model / "model.safetensors"
    if not vision_source.is_file():
        vision_source = args.vision_model / "pytorch_model.bin"
    if sha256(vision_source) != args.expected_vision_sha256:
        parser.error("The local vision checkpoint digest did not match")
    torch.manual_seed(1457)
    torch.set_num_threads(args.threads)
    torch.set_num_interop_threads(1)
    records = json.loads(args.manifest.read_text())
    splits = anime_partitions(records)
    rows = splits[0] + splits[1] + splits[2]
    vision = VisionReadout(args.vision_model, args.vision_family).eval()
    features = source_features(rows, args.manifest, vision)
    del vision
    print(f"Encoded {len(features)} authorized Anime references", flush=True)
    # Reuse identical annotations across different references.
    prompts = [[caption(role, row) for row in rows] for role in ROLES]
    unique = list(dict.fromkeys(prompts[0] + prompts[1]))
    encoded = native_context_targets(unique, args.comfy_source, args.text_encoder, args.checkpoint, SLOTS)
    by_caption = dict(zip(unique, encoded, strict=True))
    targets = np.stack([np.stack([by_caption[item] for item in values]) for values in prompts], axis=1)

    training_size, valid_size = len(splits[0]), len(splits[1])
    lifts, means, desired_codes, variance = reduce_targets(targets, training_size)
    artist = ReferenceAuthor(features.shape[1], lifts, means)
    optimizer = torch.optim.AdamW(artist.parameters(), lr=0.002, weight_decay=0.02)
    source = torch.from_numpy(features)
    supervision = torch.from_numpy(desired_codes.astype(np.float32))
    validation = slice(training_size, training_size + valid_size)
    best = None
    for step in range(350):
        indices = torch.randperm(training_size)[:32]
        optimizer.zero_grad()
        noise = torch.randn_like(source[indices]) * 0.01
        loss = torch.nn.functional.smooth_l1_loss(artist.codes(source[indices] + noise), supervision[indices])
        loss.backward()
        optimizer.step()
        if (step + 1) % 10 == 0:
            with torch.inference_mode():
                score = torch.nn.functional.mse_loss(artist.codes(source[validation]), supervision[validation]).item()
                if best is None or score < best[0]:
                    best = score, step + 1, {key: value.clone() for key, value in artist.state_dict().items()}
    artist.load_state_dict(best[2])
    artist.eval()
    args.out.mkdir(parents=True, exist_ok=True)
    model = args.out / "reference_author.onnx"
    state = args.out / "reference_author.safetensors"
    save_file(artist.state_dict(), state)
    outputs = [f"{role}_tokens" for role in ROLES]
    torch.onnx.export(
        artist, (source[:2],), model, dynamo=False, opset_version=17, input_names=["image_embeds"],
        output_names=outputs, dynamic_axes={key: {0: "references"} for key in ("image_embeds", *outputs)},
    )
    checker.check_model(model)
    graph = ReferenceEvaluator(str(model))
    measured = {}
    for name, selected in (("held_out", validation), ("unseen_motif", slice(training_size + valid_size, None))):
        predicted = graph.run(outputs, {"image_embeds": features[selected]})
        with torch.inference_mode():
            pytorch = [value.numpy() for value in artist(source[selected])]
        measured[name] = {"onnx_max_abs_error": max(float(np.max(np.abs(a - b))) for a, b in zip(predicted, pytorch))}
        for index, role in enumerate(ROLES):
            teacher = targets[selected, index].reshape(-1, SLOTS * WIDTH)
            reference_mean = np.broadcast_to(means[index], teacher.shape)
            measured[name][role] = {
                "learned": evaluate(predicted[index].reshape(teacher.shape), teacher),
                "constant": evaluate(reference_mean, teacher),
            }
    receipt = {
        "domain": "anime",
        "vision_family": args.vision_family,
        "author_sha256": sha256(model),
        "diffusion_sha256": args.expected_checkpoint_sha256,
        "teacher_weights_sha256": checkpoint_digest,
        "text_encoder_sha256": sha256(args.text_encoder),
        "visual_encoder_sha256": args.expected_vision_sha256,
        "state_sha256": sha256(state),
        "manifest_sha256": sha256(args.manifest),
        "targets": dict(zip(ROLES, variance)),
        "token_shape": [SLOTS, WIDTH],
        "samples": dict(zip(("train", "held_out", "unseen_motif"), [[row["id"] for row in group] for group in splits])),
        "selected_step": best[1],
        "held_out_code_mse": best[0],
        "metrics": measured,
        "seconds": round(time.monotonic() - start, 3),
    }
    (args.out / "training-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({key: value for key, value in receipt.items() if key != "samples"}), flush=True)


if __name__ == "__main__":
    main()

"""Compare text-only, full encoder, and learned compact reference conditioning."""

import argparse
import json
import time
from pathlib import Path

import numpy as np
import onnxruntime as ort
import torch
from diffusers import DPMSolverMultistepScheduler, StableDiffusionPipeline
from PIL import Image
from transformers import CLIPVisionModelWithProjection

from fit_reference_projection import normalise, pixel_values, sha256


DESIGN = (
    "anime fashion illustration, full-length adult heroine, orchid-and-moth-inspired couture, "
    "elegant ivory silk evening coat, violet petal collar, translucent dusky rose sleeves, "
    "calm standing pose, white editorial background, refined line art, one person"
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for field in ("foundation", "ip-adapter", "compact-encoder", "projection", "reference", "out"):
        parser.add_argument(f"--{field}", type=Path, required=True)
    parser.add_argument("--seed", type=int, default=1457)
    parser.add_argument("--steps", type=int, default=12)
    parser.add_argument(
        "--region", nargs=4, type=float, metavar=("LEFT", "TOP", "RIGHT", "BOTTOM"),
        help="Test learned attention confined to a rectangle expressed as fractions of the image.",
    )
    args = parser.parse_args()
    if args.region:
        left, top, right, bottom = args.region
        if not (0 <= left < right <= 1 and 0 <= top < bottom <= 1):
            parser.error("Use a rectangle within the image")
    args.out.mkdir(parents=True, exist_ok=True)
    torch.set_num_threads(5)
    torch.set_num_interop_threads(1)
    options = ort.SessionOptions()
    options.intra_op_num_threads = 4
    options.inter_op_num_threads = 1
    vision = ort.InferenceSession(args.compact_encoder, options, providers=["CPUExecutionProvider"])
    project = ort.InferenceSession(args.projection, options, providers=["CPUExecutionProvider"])

    with Image.open(args.reference) as source:
        pixels = pixel_values([source])
    encoded = vision.run(["image_embeds"], {"pixel_values": pixels})[0]
    compact = project.run(["ip_adapter_image_embeds"], {"image_embeds": encoded})[0]
    teacher = CLIPVisionModelWithProjection.from_pretrained(
        args.ip_adapter / "models/image_encoder", local_files_only=True, use_safetensors=True,
        torch_dtype=torch.bfloat16,
    ).eval()
    with torch.inference_mode():
        full = teacher(pixel_values=torch.from_numpy(pixels).to(dtype=teacher.dtype)).image_embeds.float().numpy()
    del teacher

    pipe = StableDiffusionPipeline.from_pretrained(
        args.foundation, local_files_only=True, variant="fp16", use_safetensors=True,
        torch_dtype=torch.bfloat16, safety_checker=None, feature_extractor=None, requires_safety_checker=False,
    )
    pipe.scheduler = DPMSolverMultistepScheduler.from_config(pipe.scheduler.config, algorithm_type="dpmsolver++")
    pipe.set_progress_bar_config(disable=True)
    pipe.load_ip_adapter(
        str(args.ip_adapter), subfolder="models", weight_name="ip-adapter_sd15.safetensors",
        image_encoder_folder=None,
    )
    result = {
        "request": DESIGN,
        "seed": args.seed,
        "resolution": [384, 512],
        "steps": args.steps,
        "reference_sha256": sha256(args.reference),
        "projection_sha256": sha256(args.projection),
        "projection_teacher_cosine": float((normalise(full) * normalise(compact)).sum()),
        "variants": [],
    }
    variants = [("text-only", full, 0.0), ("full-reference", full, 0.75), ("learned-reference", compact, 0.75)]
    attention = {}
    if args.region:
        mask = torch.zeros((1, 1, 512, 384), dtype=pipe.dtype)
        mask[..., round(top * 512):round(bottom * 512), round(left * 384):round(right * 384)] = 1
        attention["ip_adapter_masks"] = [mask]
        pipe.set_ip_adapter_scale([[0.75]])
        result["region"] = args.region
        variants = [("regional-reference", compact, 0.75)]
    for label, embedding, scale in variants:
        tokens = torch.from_numpy(np.stack([np.zeros_like(embedding), embedding])).to(dtype=pipe.dtype)
        if not args.region:
            pipe.set_ip_adapter_scale(scale)
        start = time.monotonic()
        with torch.inference_mode():
            image = pipe(
                DESIGN, height=512, width=384, num_inference_steps=args.steps, guidance_scale=6,
                ip_adapter_image_embeds=[tokens], generator=torch.Generator().manual_seed(args.seed),
                cross_attention_kwargs=attention,
            ).images[0]
        elapsed = time.monotonic() - start
        path = args.out / f"{label}.png"
        image.save(path)
        image_vector = vision.run(["image_embeds"], {"pixel_values": pixel_values([image])})[0]
        measurement = {
            "variant": label, "seconds": round(elapsed, 3), "sha256": sha256(path),
            "reference_cosine": float((normalise(encoded) * normalise(image_vector)).sum()),
        }
        result["variants"].append(measurement)
        print(json.dumps(measurement), flush=True)
    (args.out / "comparison.json").write_text(json.dumps(result, indent=2) + "\n")


if __name__ == "__main__":
    main()

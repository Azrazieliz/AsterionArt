"""Render an ordinary creation case with pinned local Anima weights for comparison."""

import argparse
import gc
import hashlib
import json
from pathlib import Path
import sys
import time


CASES = {
    "outfit": (
        "anime fashion illustration, full-length adult heroine, orchid-and-moth-inspired couture, "
        "elegant ivory silk evening coat, violet petal collar, translucent dusky rose sleeves, "
        "calm standing pose, white editorial background, refined line art, one person"
    ),
    "character-board": (
        "premium anime character design reference board, one adult heroine who embodies a fusion "
        "of silk moth and orchid in her body, pale lilac petal hair and woven pearlescent skin, "
        "defined wing roots grown from the shoulder blades, graceful body silhouette, "
        "dominant uncropped full body true front, exact side profile and full body rear, "
        "same proportions and features, neutral standing, white analytical background"
    ),
}


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def visual_tokens(reference, encoder_path, projection_path, threads):
    import numpy as np
    import onnxruntime as ort
    from PIL import Image

    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "conditioning"))
    from fit_reference_projection import pixel_values

    settings = ort.SessionOptions()
    settings.intra_op_num_threads = threads
    encoder = ort.InferenceSession(encoder_path, settings, providers=["CPUExecutionProvider"])
    projection = ort.InferenceSession(projection_path, settings, providers=["CPUExecutionProvider"])
    with Image.open(reference) as image:
        source = encoder.run(["image_embeds"], {"pixel_values": pixel_values([image])})[0]
    tokens = projection.run(["anima_reference_tokens"], {"image_embeds": source})[0]
    if tokens.ndim != 3 or tokens.shape[0] != 1 or tokens.shape[-1] != 1024 or not np.isfinite(tokens).all():
        raise ValueError("The graph did not return Anima reference tokens")
    return tokens


def append_conditioning(model, conditions, reference, strength):
    import torch

    combined = []
    for hidden, source_details in conditions:
        details = source_details.copy()
        ids = details.pop("t5xxl_ids").unsqueeze(0)
        weights = details.pop("t5xxl_weights").unsqueeze(0).unsqueeze(-1)
        dtype = model.get_dtype_inference()
        existing = model.diffusion_model.preprocess_text_embeds(
            hidden.to(dtype=dtype), ids, t5xxl_weights=weights.to(dtype=dtype),
        )
        added = torch.as_tensor(reference, dtype=existing.dtype).expand(existing.shape[0], -1, -1) * strength
        result = torch.cat((existing, added), dim=1)
        if not torch.equal(result[:, :existing.shape[1]], existing):
            raise RuntimeError("Anima lost existing context while attaching the reference")
        combined.append([result, details])
    return combined


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ("comfy-source", "checkpoint", "text-encoder", "vae", "out"):
        parser.add_argument(f"--{key}", type=Path, required=True)
    parser.add_argument("--expected-checkpoint-sha256", required=True)
    parser.add_argument("--case", choices=CASES, default="outfit")
    parser.add_argument("--seed", type=int, default=1457)
    parser.add_argument("--steps", type=int, default=30)
    parser.add_argument("--width", type=int, default=384)
    parser.add_argument("--height", type=int, default=512)
    parser.add_argument("--threads", type=int, default=6)
    parser.add_argument("--reference", type=Path)
    parser.add_argument("--compact-encoder", type=Path)
    parser.add_argument("--reference-projection", type=Path)
    parser.add_argument("--projection-receipt", type=Path)
    parser.add_argument("--reference-strength", type=float, default=1.0)
    args = parser.parse_args()
    if digest(args.checkpoint) != args.expected_checkpoint_sha256:
        parser.error("Checkpoint digest did not match the comparison pin")
    if args.width <= 0 or args.height <= 0 or args.width % 16 or args.height % 16:
        parser.error("The Anima test render needs positive multiples of 16")
    reference_files = (args.reference, args.compact_encoder, args.reference_projection)
    if any((*reference_files, args.projection_receipt)):
        if not all((*reference_files, args.projection_receipt)):
            parser.error("Provide the reference, encoder, trained projection and its receipt together")
        receipt = json.loads(args.projection_receipt.read_text())
        expected = {
            "checkpoint_sha256": args.expected_checkpoint_sha256,
            "artifact_sha256": digest(args.reference_projection),
            "compact_encoder_sha256": digest(args.compact_encoder),
            "text_encoder_sha256": digest(args.text_encoder),
        }
        if any(receipt.get(key) != value for key, value in expected.items()):
            parser.error("The learned projection is bound to different source components")
    args.out.mkdir(parents=True, exist_ok=True)
    reference = visual_tokens(*reference_files, args.threads) if args.reference else None
    sys.path.insert(0, str(args.comfy_source.resolve()))
    sys.argv = [sys.argv[0], "--cpu", "--bf16-unet", "--disable-all-custom-nodes"]

    import comfy.options
    comfy.options.enable_args_parsing()
    import comfy.model_management
    import folder_paths
    import nodes
    import torch
    from PIL import Image

    torch.set_num_threads(args.threads)
    torch.set_num_interop_threads(1)
    for role, model in (
        ("diffusion_models", args.checkpoint),
        ("text_encoders", args.text_encoder),
        ("vae", args.vae),
    ):
        folder_paths.add_model_folder_path(role, str(model.parent.resolve()), is_default=True)
    started = time.monotonic()
    print("Loading the reference text encoder", flush=True)
    clip, = nodes.CLIPLoader().load_clip(args.text_encoder.name, device="cpu")
    with torch.inference_mode():
        positive, = nodes.CLIPTextEncode().encode(clip, CASES[args.case])
        negative, = nodes.CLIPTextEncode().encode(clip, "")
    context = list(positive[0][0].shape)
    print("Text encoder tensor", context, flush=True)
    comfy.model_management.unload_all_models()
    del clip
    gc.collect()

    print("Loading the pinned Anima renderer", flush=True)
    model, = nodes.UNETLoader().load_unet(args.checkpoint.name, "default")
    if model is None or type(model.model).__name__ != "Anima":
        raise RuntimeError("Checkpoint did not load as an Anima diffusion model")
    print("Denoising", flush=True)
    before_render = time.monotonic()
    with torch.inference_mode():
        if reference is not None:
            positive = append_conditioning(model.model, positive, reference, args.reference_strength)
            negative = append_conditioning(model.model, negative, reference, 0)
            print("Conditioned Anima tensor", list(positive[0][0].shape), flush=True)
        latent = {"samples": torch.zeros((1, 16, 1, args.height // 8, args.width // 8))}
        image_latent, = nodes.KSampler().sample(
            model, args.seed, args.steps, 4.0, "euler", "beta", positive, negative, latent,
        )
    comfy.model_management.unload_all_models()
    del model
    gc.collect()

    print("Decoding the Anima result", flush=True)
    vae, = nodes.VAELoader().load_vae(args.vae.name)
    with torch.inference_mode():
        decoded, = nodes.VAEDecode().decode(vae, image_latent)
    frame = Image.fromarray((decoded[0].clamp(0, 1) * 255).byte().cpu().numpy(), "RGB")
    output = args.out / f"{args.case}.png"
    frame.save(output)
    result = {
        "case": args.case,
        "source_sha256": args.expected_checkpoint_sha256,
        "text_encoder_sha256": digest(args.text_encoder),
        "vae_sha256": digest(args.vae),
        "render_sha256": digest(output),
        "text_encoder_shape": context,
        "renderer_context_shape": list(positive[0][0].shape) if args.reference else None,
        "visual_reference_sha256": digest(args.reference) if args.reference else None,
        "reference_projection_sha256": digest(args.reference_projection) if args.reference_projection else None,
        "seed": args.seed,
        "steps": args.steps,
        "resolution": [args.width, args.height],
        "renderer_seconds": round(time.monotonic() - before_render, 3),
        "total_seconds": round(time.monotonic() - started, 3),
    }
    (args.out / f"{args.case}.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result), flush=True)


if __name__ == "__main__":
    main()

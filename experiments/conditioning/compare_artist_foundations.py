"""Measure separate candidate roles on the same authorized Anime evaluation case."""

import argparse
import hashlib
import json
from pathlib import Path
import resource
import time

from PIL import Image, ImageOps
import torch
from transformers import AutoImageProcessor, AutoModelForCausalLM, AutoModelForImageTextToText, AutoProcessor, AutoTokenizer, Dinov2Model


def sha256(path):
    with path.open("rb") as data:
        return hashlib.file_digest(data, "sha256").hexdigest()


def generate(args):
    if args.kind == "visual-language":
        processor = AutoProcessor.from_pretrained(args.checkpoint, local_files_only=True, trust_remote_code=False)
        model = AutoModelForImageTextToText.from_pretrained(
            args.checkpoint, local_files_only=True, torch_dtype=torch.float32, trust_remote_code=False,
        ).eval()
        images = [{"type": "image", "image": str(reference.resolve())} for reference in args.reference]
        question = {"type": "text", "text": args.question}
        inputs = processor.apply_chat_template(
            [{"role": "user", "content": images + [question]}],
            add_generation_prompt=True, tokenize=True, return_dict=True, return_tensors="pt",
        )
        decoder = processor
    else:
        decoder = AutoTokenizer.from_pretrained(args.checkpoint, local_files_only=True, trust_remote_code=False)
        model = AutoModelForCausalLM.from_pretrained(
            args.checkpoint, local_files_only=True, torch_dtype=torch.float32, trust_remote_code=False,
        ).eval()
        inputs = decoder.apply_chat_template(
            [{"role": "user", "content": args.question}],
            add_generation_prompt=True, tokenize=True, return_dict=True, return_tensors="pt",
        )
    input_tokens = inputs["input_ids"].shape[-1]
    with torch.inference_mode():
        start = time.perf_counter()
        output = model.generate(**inputs, max_new_tokens=args.tokens, do_sample=False)
        elapsed = time.perf_counter() - start
    tokens = output[0, input_tokens:]
    return {
        "parameters": sum(weight.numel() for weight in model.parameters()),
        "reference_count": len(args.reference),
        "input_tokens": input_tokens,
        "output_tokens": len(tokens),
        "generation_seconds": elapsed,
        "answer": decoder.decode(tokens, skip_special_tokens=True),
    }


def visual(args):
    processor = AutoImageProcessor.from_pretrained(args.checkpoint, local_files_only=True, trust_remote_code=False)
    model = Dinov2Model.from_pretrained(args.checkpoint, local_files_only=True, use_safetensors=True).eval()
    images = []
    for path in args.reference:
        with Image.open(path) as image:
            rgb = image.convert("RGB")
            images.extend((rgb, ImageOps.mirror(rgb)))
    inputs = processor(images=images, return_tensors="pt")
    with torch.inference_mode():
        t0 = time.perf_counter()
        output = model(**inputs).last_hidden_state
        elapsed = time.perf_counter() - t0
        vectors = torch.nn.functional.normalize(output[:, 0], dim=-1)
    return {
        "parameters": sum(weight.numel() for weight in model.parameters()),
        "reference_count": len(args.reference),
        "shape": list(output.shape),
        "encode_seconds": elapsed,
        "same_image_mirrored_cosine": [float(vectors[i] @ vectors[i + 1]) for i in range(0, len(vectors), 2)],
        "between_first_two_references_cosine": float(vectors[0] @ vectors[2]),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kind", required=True, choices=("text", "visual-language", "visual"))
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument("--sha256", required=True)
    parser.add_argument("--reference", type=Path, action="append", default=[])
    parser.add_argument("--question", default="")
    parser.add_argument("--tokens", type=int, default=96)
    parser.add_argument("--threads", type=int, default=3)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    if sha256(args.checkpoint / "model.safetensors") != args.sha256:
        parser.error("Checkpoint digest mismatch")
    if (args.kind == "text" and args.reference) or (args.kind != "text" and len(args.reference) < 2):
        parser.error("Use text-only input or at least two observed Anime references as applicable")
    torch.set_num_threads(args.threads)
    torch.set_num_interop_threads(1)
    start = time.perf_counter()
    result = visual(args) if args.kind == "visual" else generate(args)
    result.update(kind=args.kind, sha256=args.sha256, threads=args.threads, elapsed_seconds=time.perf_counter() - start,
                  peak_rss_kib=resource.getrusage(resource.RUSAGE_SELF).ru_maxrss)
    args.out.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result), flush=True)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Offline, opt-in Qwen3 correction-language LoRA from explicit Grading records.

This specializes TEXT reasoning to user-authored feature corrections and
preservation instructions. It DOES NOT teach a text-only Qwen model to see
pixel evidence, does not train diffusion/vision, and does not run automatically.

Default --validate uses Python standard library and never fetches weights.
--train only uses the caller's configured local/free compute; no paid jobs.
Input: app-private grading/training/explicit-visual-grades.jsonl.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def grade_examples(path: Path, max_examples: int):
    result = []
    seen = set()
    with path.open(encoding="utf-8") as file:
        for lineno, raw in enumerate(file, 1):
            if not raw.strip():
                continue
            try:
                item = json.loads(raw)
            except ValueError as exc:
                raise ValueError(f"Bad JSON at line {lineno}") from exc
            if item.get("schema") != "asterion.grading-evidence.v1":
                raise ValueError(f"Unsupported Grading schema at line {lineno}")
            feature = str(item.get("target_feature") or "").strip()
            correction = str(item.get("correct_exact") or "").strip()
            preserved = str(item.get("preserve_exact") or "").strip()
            mode = str(item.get("creation_mode") or item.get("render_mode") or "")
            concepts = item.get("concepts") or []
            if not isinstance(concepts, list):
                raise ValueError(f"Invalid canonical concept list at {lineno}")
            if not (correction or preserved):
                continue  # numeric scores alone are not text teaching examples
            key = (mode, tuple(concepts), feature, correction, preserved)
            if key in seen:
                continue
            seen.add(key)
            instruction = (
                "You are Asterion's design-revision reasoning assistant. "
                "Use only the user's explicit correction/preservation authority; "
                "do not invent other failures or change the Original's methodology.\n"
                "Mode: " + mode + "\n"
                "Resolved concepts: " + ", ".join(map(str, concepts)) + "\n"
                "Feature assessed: " + feature + "\n"
                "Provide a precise scoped revision contract."
            )
            response = (
                "PRESERVE EXACTLY:\n" + (preserved or "(nothing specified)") +
                "\nCORRECT EXACTLY:\n" + (correction or "(nothing specified)")
            )
            result.append((instruction, response))
            if len(result) >= max_examples:
                break
    return result


def train(args, examples):
    import torch
    import torch.nn.functional as F
    from peft import LoraConfig, TaskType, get_peft_model
    from transformers import AutoTokenizer, AutoModelForCausalLM

    if args.model not in ("Qwen/Qwen3-0.6B", "Qwen/Qwen3-1.7B"):
        raise ValueError("Unsupported Qwen3 adapter base")
    torch.manual_seed(20261027)
    on_gpu = torch.cuda.is_available()
    tokenizer = AutoTokenizer.from_pretrained(args.model, trust_remote_code=False)
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token
    model = AutoModelForCausalLM.from_pretrained(
        args.model, torch_dtype=torch.float16 if on_gpu else torch.float32,
        trust_remote_code=False
    )
    model.config.use_cache = False
    model = get_peft_model(model, LoraConfig(
        task_type=TaskType.CAUSAL_LM, r=args.rank,
        lora_alpha=2 * args.rank, lora_dropout=0.05,
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj",
                        "gate_proj", "up_proj", "down_proj"], bias="none"
    ))
    device = "cuda" if on_gpu else "cpu"
    model.to(device)
    model.train()
    optimizer = torch.optim.AdamW(
        (p for p in model.parameters() if p.requires_grad),
        lr=args.lr
    )
    processed = 0
    total = 0.0
    for _ in range(args.epochs):
        for question, desired in examples:
            user_text = tokenizer.apply_chat_template(
                [{"role": "user", "content": question}],
                tokenize=False, add_generation_prompt=True, enable_thinking=False
            )
            prefix = tokenizer.encode(user_text, add_special_tokens=False)
            answer = tokenizer.encode(desired, add_special_tokens=False)
            remaining = args.context - len(prefix) - 1
            if remaining < 24 or not answer:
                continue  # invalid capacity, do not label a prompt as trained
            answer = answer[:remaining] + [tokenizer.eos_token_id]
            ids = torch.tensor([prefix + answer], device=device)
            logits = model(input_ids=ids).logits[:, :-1, :].float()
            labels = ids[:, 1:].clone()
            labels[:, :len(prefix) - 1] = -100  # train only correction text
            loss = F.cross_entropy(
                logits.reshape(-1, logits.size(-1)),
                labels.reshape(-1), ignore_index=-100
            )
            if not torch.isfinite(loss):
                raise ArithmeticError("Nonfinite adapter training loss")
            optimizer.zero_grad(set_to_none=True)
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            processed += 1
            total += loss.item()
    if not processed:
        raise ValueError("No examples fit the chosen model context")
    args.output.mkdir(parents=True, exist_ok=True)
    model.save_pretrained(args.output, safe_serialization=True)
    tokenizer.save_pretrained(args.output)
    manifest = {
        "schema": "asterion.grade-conditioned-qwen3-lora.v1",
        "training_kind": "TEXT_CORRECTION_SFT_ONLY",
        "base_model": args.model,
        "total_explicit_examples": len(examples),
        "trained_steps": processed,
        "epochs": args.epochs,
        "rank": args.rank,
        "mean_training_loss": total / processed,
        "image_pixel_conditioning": False,
        "visual_semantic_model_trained": False,
        "diffusion_model_modified": False,
        "holdout_visual_quality_verified": False,
        "gguf_conversion_needed": True
    }
    (args.output / "asterion_training_manifest.json").write_text(
        json.dumps(manifest, indent=2), encoding="utf-8"
    )
    return manifest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--model", default="Qwen/Qwen3-0.6B")
    parser.add_argument("--output", type=Path, default=Path("grade-lora-output"))
    parser.add_argument("--max-examples", type=int, default=2500)
    parser.add_argument("--context", type=int, default=2048)
    parser.add_argument("--epochs", type=int, default=1)
    parser.add_argument("--rank", type=int, default=8)
    parser.add_argument("--lr", type=float, default=8e-5)
    parser.add_argument("--train", action="store_true")
    args = parser.parse_args()
    if args.max_examples not in range(1, 10001):
        raise ValueError("Invalid example limit")
    if args.context not in (1024, 2048, 4096):
        raise ValueError("Context not supported by this training recipe")
    if args.rank not in (4, 8, 16, 32) or args.epochs not in range(1, 11):
        raise ValueError("Rank or epochs out of safe bounds")
    examples = grade_examples(args.dataset, args.max_examples)
    print(json.dumps({
        "valid_user_authored_corrections": len(examples),
        "trained": False, "cost_eur": 0, "model": args.model
    }))
    if not args.train:
        print("Validation only. No weights downloaded or trained.")
        return
    if not examples:
        raise ValueError("No user-authored preserve/correct text available")
    print(json.dumps(train(args, examples), indent=2))


if __name__ == "__main__":
    main()

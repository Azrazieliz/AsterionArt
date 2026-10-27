#!/usr/bin/env python3
"""Asterion Qwen3 preference LoRA: explicit opt-in, zero paid services.

Data: app-private qwen3-dpo-preferences.jsonl, exported by TrainingCorpusStore.
All network/model downloads occur only if --train is explicitly passed.
The default --validate path uses only Python's standard library.
Does NOT train the image or video model and does NOT increase their native
text-encoder dimensions. It trains a real PEFT adapter for the design LLM.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path


def load_pairs(path: Path, max_examples: int):
    examples = []
    seen = set()
    for line_no, raw in enumerate(path.open(encoding="utf-8"), start=1):
        if not raw.strip():
            continue
        try:
            item = json.loads(raw)
        except ValueError as e:
            raise ValueError(f"{path}:{line_no}: invalid JSON: {e}") from e
        if item.get("schema") != "asterion.preference-pair.v1":
            raise ValueError(f"{path}:{line_no}: unsupported schema")
        values = tuple(str(item.get(k, "")).strip() for k in
                       ("prompt", "chosen", "rejected", "modality"))
        if any(not v for v in values):
            raise ValueError(f"{path}:{line_no}: missing required text")
        if values[1] == values[2]:
            continue
        key = (values[3], values[1], values[2])
        if key in seen:
            continue
        seen.add(key)
        examples.append(values)
        if len(examples) >= max_examples:
            break
    return examples


def training_args():
    p = argparse.ArgumentParser()
    p.add_argument("--dataset", type=Path, required=True)
    p.add_argument("--model", default="Qwen/Qwen3-0.6B")
    p.add_argument("--output", type=Path, default=Path("adapter-output"))
    p.add_argument("--max-examples", type=int, default=6000)
    p.add_argument("--max-length", type=int, default=2048)
    p.add_argument("--epochs", type=int, default=1)
    p.add_argument("--lr", type=float, default=8e-5)
    p.add_argument("--rank", type=int, default=8)
    p.add_argument("--beta", type=float, default=0.4)
    p.add_argument("--preference-weight", type=float, default=0.7)
    p.add_argument("--train", action="store_true",
                   help="Explicitly train and download a base model locally.")
    return p.parse_args()


def train(args, pairs):
    import torch
    import torch.nn.functional as F
    from transformers import AutoTokenizer, AutoModelForCausalLM
    from peft import LoraConfig, get_peft_model, TaskType

    if args.model not in ("Qwen/Qwen3-0.6B", "Qwen/Qwen3-1.7B"):
        raise ValueError("Only vetted compatible Qwen3 base architectures")
    torch.manual_seed(20261027)
    device = "cuda" if torch.cuda.is_available() else "cpu"
    dtype = torch.float16 if device == "cuda" else torch.float32
    tokenizer = AutoTokenizer.from_pretrained(args.model, trust_remote_code=False)
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token
    base = AutoModelForCausalLM.from_pretrained(
        args.model, torch_dtype=dtype, trust_remote_code=False)
    base.config.use_cache = False
    model = get_peft_model(base, LoraConfig(
        r=args.rank, lora_alpha=2 * args.rank,
        lora_dropout=0.05, bias="none",
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj",
                        "gate_proj", "up_proj", "down_proj"],
        task_type=TaskType.CAUSAL_LM))
    model.to(device)
    model.train()
    optimizer = torch.optim.AdamW(
        (p for p in model.parameters() if p.requires_grad),
        lr=args.lr, weight_decay=0.001)

    def log_probability(question: str, answer: str):
        # Explicitly compute conditional answer-token likelihood; do not
        # reward the model for predicting prompt/system tokens.
        introduction = tokenizer.apply_chat_template(
            [{"role": "system", "content":
              "Produce concrete, physically coherent visual design specifications."},
             {"role": "user", "content": question}],
            tokenize=False, add_generation_prompt=True, enable_thinking=False)
        prompt_ids = tokenizer.encode(introduction, add_special_tokens=False)
        answer_ids = tokenizer.encode(answer, add_special_tokens=False)
        available = args.max_length - len(prompt_ids) - 1
        if available <= 8:
            raise ValueError("Prompt consumes model context, cannot train this pair")
        target = answer_ids[:available] + [tokenizer.eos_token_id]
        ids = prompt_ids + target
        input_ids = torch.tensor([ids], device=device)
        logits = model(input_ids=input_ids).logits[0, :-1].float()
        labels = input_ids[0, 1:]
        token_ll = -F.cross_entropy(logits, labels, reduction="none")
        answer_start = len(prompt_ids) - 1
        answer_logp = token_ll[answer_start:].mean()
        return answer_logp

    history = []
    for epoch in range(args.epochs):
        for index, (instruction, chosen, rejected, modality) in enumerate(pairs):
            question = modality + "\n" + instruction
            optimizer.zero_grad(set_to_none=True)
            kept_lp = log_probability(question, chosen)
            rejected_lp = log_probability(question, rejected)
            # Supervised preference + margin objective (single-model ORPO-like).
            # The model's PEFT weights receive gradients; rejected candidates
            # get lower likelihood on the same instruction.
            preference_loss = F.softplus(
                -args.beta * (kept_lp - rejected_lp))
            loss = (-kept_lp) + args.preference_weight * preference_loss
            if not torch.isfinite(loss):
                raise ArithmeticError("Non-finite LoRA training loss")
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            history.append(float(loss.detach().cpu()))
    args.output.mkdir(parents=True, exist_ok=True)
    model.save_pretrained(args.output, safe_serialization=True)
    tokenizer.save_pretrained(args.output)
    metadata = {
        "schema": "asterion.qwen3-lora-training.v1",
        "base_model": args.model,
        "examples": len(pairs),
        "epochs": args.epochs,
        "rank": args.rank,
        "max_length": args.max_length,
        "mean_training_loss": sum(history) / len(history),
        "trained_weights": True,
        "image_model_modified": False,
        "video_model_modified": False,
        "evaluation_status": "UNVALIDATED",
        "conversion": "llama.cpp convert_lora_to_gguf.py --outtype f16 "
                      "--outfile <adapter.gguf> <adapter-output>",
    }
    (args.output / "asterion_training_manifest.json").write_text(
        json.dumps(metadata, indent=2), encoding="utf-8")
    return metadata


def main():
    args = training_args()
    if args.max_examples < 1 or args.max_length not in (1024, 2048, 4096):
        raise SystemExit("Unsupported size or context")
    if args.epochs not in range(1, 11) or args.rank not in (4, 8, 16, 32):
        raise SystemExit("Invalid bounded LoRA training settings")
    pairs = load_pairs(args.dataset, args.max_examples)
    print(json.dumps({
        "validated_pairs": len(pairs), "modalities": sorted({p[3] for p in pairs}),
        "training_started": args.train,
        "cost_eur": 0, "base": args.model
    }))
    if not args.train:
        print("Validation only. Pass --train to run local PEFT training.")
        return
    if not pairs:
        raise SystemExit("No comparable explicit Keep/Reject pairs to train")
    print(json.dumps(train(args, pairs), indent=2))


if __name__ == "__main__":
    main()

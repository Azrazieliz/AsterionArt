#!/usr/bin/env python3
"""Asterion -> Local Dream on-device execution adapter (Python stdlib only).

Requires Local Dream host mode bound to the device and a validated Asterion job
JSON. Never silently substitutes a model, bypasses readiness, or pretends NPU
LoRA files can be injected dynamically. Designed to run on Android Termux too.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from uuid import uuid4

CONTROL = "http://127.0.0.1:8808"
GENERATION = "http://127.0.0.1:8081"


class Blocked(RuntimeError):
    pass


def query(base: str, path: str, payload: dict | None = None, timeout: float = 15) -> dict:
    if base not in (CONTROL, GENERATION) or not path.startswith("/"):
        raise Blocked("External/non-loopback endpoints are not permitted")
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    request = Request(base + path, data=body, headers={"Content-Type": "application/json"})
    try:
        with urlopen(request, timeout=timeout) as response:
            return json.load(response)
    except (HTTPError, URLError, OSError, json.JSONDecodeError) as exc:
        raise Blocked(f"Local Dream {path} unavailable: {exc}") from exc


def contains_keyword(text: str, keyword: str) -> bool:
    if not keyword.strip():
        return False
    escaped = re.escape(keyword.strip())
    return bool(re.search(r"(?<![\w])" + escaped + r"(?![\w])", text, re.IGNORECASE))


def select_variant(job: dict) -> tuple[str, dict[str, float]]:
    checkpoint = job["checkpoint_id"]
    family = job["model_family"]
    prompt = job["positive_prompt"]
    items = sorted(
        (entry for entry in job.get("lora_library", [])
         if entry.get("enabled", True) and any(
             contains_keyword(prompt, word) for word in entry.get("trigger_words", entry.get("triggers", [])))),
        key=lambda entry: entry["id"],
    )
    incompatible = [
        item["id"] for item in items if
        item.get("model_family", item.get("family", "")).lower() != family.lower() or
        (item.get("checkpoint_ids", item.get("checkpoints")) and
         checkpoint not in item.get("checkpoint_ids", item.get("checkpoints", [])))
    ]
    if incompatible:
        raise Blocked(f"Incompatible triggered LoRA(s): {', '.join(incompatible)}")

    requested = {item["id"]: float(item.get("weight", 1.0)) for item in items}
    if not requested:
        return checkpoint, requested
    for variant in job.get("converted_variants", []):
        if not variant.get("ready", False) or variant.get("checkpoint_id") != checkpoint:
            continue
        installed = {name: float(value) for name, value in variant.get("lora_weights", {}).items()}
        if installed.keys() == requested.keys() and all(
            abs(installed[name] - weight) < 1e-4 for name, weight in requested.items()
        ):
            return variant["model_id"], requested
    raise Blocked("LoRA keywords matched but no exact, ready premerged NPU variant is registered")


def validate_job(job: dict) -> None:
    if job.get("compiler_validation") != "GENERATED":
        raise Blocked("Compiler has not confirmed a valid generation plan")
    if not job.get("workflow_versions"):
        raise Blocked("Authoritative methodology provenance is missing")
    if job.get("core_required", False) and job.get("core_readiness") != "READY":
        raise Blocked("Asterion Core prerequisite is not READY")
    if not job.get("positive_prompt", "").strip():
        raise Blocked("Empty positive prompt")
    if not job.get("checkpoint_id") or not job.get("model_family"):
        raise Blocked("Missing checkpoint identity or model family")


def wait_until_serving(model_id: str, width: int, height: int, seconds: int = 180) -> dict:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        status = query(CONTROL, "/status", timeout=10)
        if status.get("state") == "error" and (
            status.get("error_model_id") in (None, model_id)
        ):
            raise Blocked(f"Local Dream model activation failed: {status.get('message')}")
        if status.get("state") == "running" and status.get("serving_model_id") == model_id and (
            status.get("width") == width and status.get("height") == height
        ):
            return status
        time.sleep(0.75)
    raise Blocked(f"Timed out waiting for exact model/resolution: {model_id} {width}x{height}")


def validate_tokens(prompt: str, label: str) -> dict:
    info = query(GENERATION, "/tokenize", {"prompt": prompt})
    if info.get("overflow_offset", -1) >= 0:
        raise Blocked(f"{label} exceeds the active backend tokenizer context")
    if info.get("max_length", 0) and info["count"] > info["max_length"]:
        raise Blocked(f"{label} contains {info['count']} tokens; active maximum is {info['max_length']}")
    return info


def generate(job: dict) -> tuple[bytes, dict]:
    payload = {
        "prompt": job["positive_prompt"],
        "negative_prompt": job.get("negative_prompt", ""),
        "steps": int(job.get("steps", 20)),
        "cfg": float(job.get("cfg", 7.0)),
        "scheduler": job.get("scheduler", "dpm"),
        "seed": int(job.get("seed", 0)),
        "width": int(job.get("width", 1024)),
        "height": int(job.get("height", 1024)),
        "output_format": "png",
        "show_diffusion_process": False,
    }
    request = Request(
        GENERATION + "/generate",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json", "Accept": "text/event-stream"},
    )
    try:
        with urlopen(request, timeout=3600) as response:
            event = ""
            data_lines: list[str] = []
            for raw in response:
                line = raw.decode("utf-8").rstrip("\r\n")
                if line.startswith("event:"):
                    event = line[6:].strip()
                elif line.startswith("data:"):
                    data_lines.append(line[5:].strip())
                elif not line and data_lines:
                    result = json.loads("\n".join(data_lines))
                    if event == "error":
                        raise Blocked(result.get("message", "Local Dream generation failed"))
                    if event == "complete":
                        if result.get("format") != "png":
                            raise Blocked("Unexpected image format from generator")
                        encoded = result.get("image", "")
                        if encoded.startswith("data:image/png;base64,"):
                            encoded = encoded.split(",", 1)[1]
                        return base64.b64decode(encoded, validate=True), result
                    event = ""
                    data_lines = []
    except (HTTPError, URLError, OSError) as exc:
        raise Blocked(f"Generation stream failed: {exc}") from exc
    raise Blocked("Generator ended without a complete event")


def write_atomic(path: Path, data: bytes) -> None:
    with tempfile.NamedTemporaryFile(prefix=".asterion-", dir=path.parent, delete=False) as temporary:
        temporary.write(data)
        temp_name = temporary.name
    try:
        os.replace(temp_name, path)
    except BaseException:
        Path(temp_name).unlink(missing_ok=True)
        raise


def execute(job: dict, output_root: Path) -> Path:
    validate_job(job)
    target, weights = select_variant(job)
    info = query(CONTROL, "/info")
    if info.get("app") != "localdream":
        raise Blocked("Unexpected generation control service")
    catalog = query(CONTROL, "/models")
    installed = {model.get("id") for model in catalog.get("models", [])}
    if target not in installed:
        raise Blocked(f"Required checkpoint/LoRA variant is not installed: {target}")
    width, height = int(job.get("width", 1024)), int(job.get("height", 1024))
    query(CONTROL, "/select", {"model_id": target, "width": width, "height": height})
    status = wait_until_serving(target, width, height)
    # If the custom native adapter is installed, also cross-check its model ID.
    try:
        capabilities = query(GENERATION, "/asterion/capabilities", timeout=5)
    except Blocked:
        capabilities = {}
    if capabilities and capabilities.get("model_id") != target:
        raise Blocked("Local Dream reported a different loaded model than requested")
    if weights and capabilities and capabilities.get("lora_weights"):
        declared = {k: float(v) for k, v in capabilities["lora_weights"].items()}
        if declared != weights:
            raise Blocked("Loaded NPU variant does not match the requested LoRA recipe")
    positive = validate_tokens(job["positive_prompt"], "Positive prompt")
    negative = validate_tokens(job.get("negative_prompt", ""), "Negative prompt")
    image, metadata = generate(job)

    job_id = job.get("job_id") or uuid4().hex
    if not re.fullmatch(r"[a-zA-Z0-9_-]{1,90}", job_id):
        raise Blocked("Invalid job ID")
    output_dir = output_root / job_id
    output_dir.mkdir(parents=True, exist_ok=False)
    image_name = output_dir / "image_0001.png"
    write_atomic(image_name, image)
    provenance = {
        "schema_version": 1,
        "job_id": job_id,
        "model_id": target,
        "checkpoint_id": job["checkpoint_id"],
        "lora_weights": weights,
        "workflow_versions": job["workflow_versions"],
        "compiler_validation": job["compiler_validation"],
        "core_readiness": job.get("core_readiness"),
        "positive_tokens": positive,
        "negative_tokens": negative,
        "backend_status": status,
        "backend_capabilities": capabilities,
        "generation": {key: value for key, value in metadata.items() if key != "image"},
        "sha256": hashlib.sha256(image).hexdigest(),
        "size_bytes": len(image),
    }
    write_atomic(output_dir / "provenance.json", json.dumps(provenance, indent=2).encode())
    write_atomic(output_dir / "prompt.txt", (job["positive_prompt"] + "\n\nNEGATIVE:\n" +
                                            job.get("negative_prompt", "")).encode())
    return output_dir


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("job", type=Path)
    parser.add_argument("--output-dir", type=Path, default=Path("Illustrations"))
    args = parser.parse_args()
    try:
        job = json.loads(args.job.read_text(encoding="utf-8"))
        result = execute(job, args.output_dir)
    except (Blocked, OSError, ValueError, KeyError) as exc:
        parser.exit(2, f"BLOCKED: {exc}\n")
    print(result)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

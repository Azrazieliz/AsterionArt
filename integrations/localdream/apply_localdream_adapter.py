#!/usr/bin/env python3
"""Apply the Asterion capability/identity protocol to xororz/local-dream.

Usage: python3 apply_localdream_adapter.py /path/to/local-dream
The upstream repo is CC BY-NC 4.0; preserve its copyright/license notices.
No model binaries are rewritten and no additional prompt context is claimed.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ANCHOR = '  if (text_encoder) registerTokenizeEndpoint(svr, text_encoder.get());'
MARKER = '  // ASTERION_CAPABILITIES_V1'
ENDPOINT = r'''
  // ASTERION_CAPABILITIES_V1: reports backend identity to prevent running a
  // compiled prompt or merged-LoRA recipe against the wrong NPU context.
  // Never send absolute model paths or private file system details.
  svr.Get("/asterion/capabilities",
          [&opts, &text_encoder](const httplib::Request &,
                                  httplib::Response &res) {
    nlohmann::json capability;
    capability["schema_version"] = 1;
    capability["model_id"] =
        std::filesystem::path(opts.model_dir).filename().string();
    capability["backend"] = opts.isAnima() ? "anima"
        : opts.isSdxl() ? (opts.isMnn() ? "sdxlmnn" : "sdxl")
        : opts.isDit() ? "dit"
        : opts.isMnn() ? "sd15cpu" : "sd15npu";
    capability["npu"] = !opts.isMnn();
    capability["context_positions"] = text_encoder
        ? (text_encoder->isAnima() ? anima_text_seq_len
           : text_encoder->max_chunks_ == 0 ? 0
           : text_encoder->max_chunks_ * 77)
        : 0;
    capability["dynamic_lora"] = false;
    capability["lora_mode"] = "premerged_model_variant";
    capability["variant_verified"] = false;
    auto variant_path = std::filesystem::path(opts.model_dir) /
                        "asterion_variant.json";
    if (std::filesystem::is_regular_file(variant_path)) {
      try {
        std::ifstream input(variant_path);
        nlohmann::json variant = nlohmann::json::parse(input);
        if (variant.is_object() &&
            variant.value("model_id", std::string()) ==
                capability["model_id"].get<std::string>() &&
            variant.contains("lora_weights") &&
            variant["lora_weights"].is_object()) {
          capability["lora_weights"] = variant["lora_weights"];
          capability["base_checkpoint_id"] =
              variant.value("base_checkpoint_id", std::string());
          // The sidecar attests metadata consistency, not model authenticity.
          capability["variant_verified"] = true;
        }
      } catch (const std::exception &) {
        capability["variant_error"] = "Invalid variant metadata";
      }
    }
    res.set_header("Cache-Control", "no-store");
    res.set_content(capability.dump(), "application/json");
  });
'''.strip('\n')


def patch(root: Path) -> str:
    source = root / 'app/src/main/cpp/src/main.cpp'
    if not source.is_file():
        raise FileNotFoundError(f"Upstream native engine missing: {source}")
    text = source.read_text(encoding='utf-8')
    if MARKER in text:
        return "already-patched"
    if text.count(ANCHOR) != 1:
        raise RuntimeError("Upstream code changed; refusing an uncertain patch")
    changed = text.replace(ANCHOR, ANCHOR + '\n\n' + ENDPOINT)
    source.write_text(changed, encoding='utf-8')
    return "patched"


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('localdream_repo', type=Path)
    args = parser.parse_args()
    print(patch(args.localdream_repo))

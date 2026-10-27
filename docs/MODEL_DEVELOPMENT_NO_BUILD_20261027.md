# Asterion Art model development — 2026-10-27

Development branch: research/model-capabilities-no-build-20261027
User constraints: EUR 0; GitHub-only operations; NO APK builds before all model gates are met.

## Implemented in source, not compiled

- 32K native Qwen3 planning capacity and three reasoning stages. On-device correctness remains unverified.
- Complete 455-concept taxonomy, native-tokenizer-based LOSSLESS sequential conditioning scheduler.
- The native image encoder tensor limits HAVE NOT BEEN INCREASED. Sequential passes are not simultaneous long-context attention.
- Keep/Reject actions and their text-only preference ranker are removed from active learning.
- Advanced Grading now stores separately assessed dimensions, actual image/target cell/region evidence PNG + SHA256, explicit preserve/correct notes, and source identity.
- A small offline per-mode, per-grading-axis pixel-feature model is retrained from explicit scores. This is real parameter training, NOT visual semantic understanding or a fine-tuned vision model.
- Qwen and the dedicated Character Creation author receive only user-authored preservation/correction notes from matching mode/concept/target contexts; numerical ratings alone invent no creative rules.
- Private grade-evidence JSONL is exported without cloud upload; updates/deletions retrain the local model without stale feedback.
- Locally importable/activatable GGUF Qwen3 LoRA and rollback to immutable base model.
- Optional Qwen3 LoRA training recipe requiring user evidence and compute. No foundation-model LoRA has been trained yet.
- Video continuation reuses the preceding clip's decoded last frame. No guarantee of seam-free video.

## Missing release gates

1. Real native image-model 1024/2048/4096+ token support via architecture changes, trained/exported encoder and device validation, rather than a numeric flag.
2. Standalone Wan-family inference in this APK, without a separate Nightmare Mobile process and with NPU capability detection and licensed weight installer.
3. Identity, anatomy, cross-view, concept adherence and temporal coherence benchmarks on generated outputs, not just technical contrast scores.
4. Trained and evaluated foundation-model LoRA weights; optional diffusion/video adapters only where conversions are actually supported.
5. Full practical model/device tests, verified persistent release signing, then update-safe APK builds after the user authorizes them.

## Free training path

GradingCorpusStore exports app-private grading/training/explicit-visual-grades.jsonl, including crop paths/checksums, exact user notes and separate scores.
Validate without external dependencies using: python training/train_qwen3_grading_lora.py --dataset INPUT.jsonl.
The optional --train action fine-tunes Qwen3-0.6B/1.7B on USER-WRITTEN CORRECTION TEXT ONLY. It does not make Qwen able to see pixels or train the image/vision backend. No paid jobs or cloud uploads.
Convert trained PEFT weights with upstream ggml-org/llama.cpp convert_lora_to_gguf.py; validate exact base compatibility.
Do not claim trained adapter weights exist until produced and evaluated on holdout generations.

## Evidence and licenses

Qwen3 0.6B/1.7B base weights: Apache 2.0. Wan2.2 TI2V 5B: Apache 2.0.
llama.cpp GGUF runtime and LoRA converter: MIT.
Nightmare Mobile video source: CC BY-NC 4.0; candidate for native port, NOT yet embedded.
Local Dream native still image runtime: CC BY-NC 4.0. Qualcomm model binaries may have additional license conditions.

All results on this branch remain unbuilt and unverified until explicitly authorized.

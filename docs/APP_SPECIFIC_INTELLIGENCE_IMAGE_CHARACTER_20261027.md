# Asterion Art — approved priorities 1–3 (source implementation, 2026-10-27)

Owner scope: (1) app-specific visual intelligence/self-learning, (2) improved image inference, (3) approved Character Creation Phase 1 end-to-end. No changes to other Originals or rich canonical taxonomy. EUR 0; GitHub only; NO builds until separately authorized.

## (1) Visual intelligence / corrected self-learning

- The optional Qwen3-VL-2B-Instruct GGUF language model + matching Q8 mmproj are fetched directly from the Apache-2.0 upstream repository by explicit one-tap model installation. Two upstream SHA-256 digests are verified. No model weight files live in Git.
- The pinned `llama.cpp` v0.7.0 `mtmd` C API is compiled alongside the existing JNI runtime. `asterion-vision` is an independent Android-native CPU inference engine and consumes actual RGB pixels. It never sends images to a remote provider.
- User-approved Advanced Grading remains the only labeled training source. Per-image-region numeric grading trains a small local score model by mode/dimension, with no false claims of semantic understanding. Keep/Reject are not used.
- A new multilayer Qwen3-VL visual audit inspects actual generated pixels and each frozen character's anatomy, physical concept fusion, view identity and illustrated 2D grammar separately, preserving all frozen authority across pages. It distinguishes visible evidence, concerns, unknowns and inconclusive responses. It never issues an invented grade or overwrites the frozen design.
- Generated output SHA-256 is checked against the saved generation provenance before analysis.
- Machine observations appear beside Grading. User can explicitly review them as an UNSAVED grading draft. Only actual Save adds a human-approved training example.
- Explicit Preserve and Correct text from the grade can launch masked local inpainting for the graded region; original pixels outside the mask remain preserved. The revision job carries exact correction, preservation and original grade ID.
- A free/optional Qwen3 text LoRA training script exists separately; no trained adapters or newly trained image VLM are claimed.

## (2) Native image inference improvements

- Character Creation renders each independently frozen character as its own three-view clinical reference instead of asking one short-context image encoder to attend to five different character payloads at once.
- Native image tokenizer measurements confirm every pass fits the actual selected model. When needed, sequential passes carry the frozen character ID, every concept name and a no-identity-change anchor. This is NOT an expanded simultaneous diffusion text encoder.
- Completed identity triptychs are saved in per-character checkpoints. Five are assembled into a single 16:9 continuous white analytical sheet without artificial bars or surrounding captions. Nonwhite tile backgrounds fail rather than silently becoming decorated panels.
- Rendered sheets and per-identity records remain traceable to frozen specifications, and the vision model can be scheduled only AFTER the NPU image engine releases its memory.
- The 32K Qwen reasoning decoder is fixed to expose native token counting and explicit conversation reset. Both prompt overflow and generation-time context exhaustion stop cleanly instead of silently shifting away earlier methodology authority.

## (3) Character Creation Phase 1

- 1–30 sheets selected by user; five original anatomical identities per sheet, all independently authored and frozen before the user authorizes any image generation.
- Full physical pairwise concept fusion, higher-order fusion for three-plus concepts, independent female character authorship, clinical unclothed anatomy, distinct design, aesthetic quality, specific hand-illustrated 2D anime/manhwa language and true-front/exact-profile/direct-rear references remain mandatory.
- Character-specific model-facing prompts shown to the user before Validate & Generate now correspond to the actual individual render path.
- Design Direction, sub-concepts and former character outfit/weapon/assembly phases remain absent. Outfit and Weapon are separate modes.

## Integrity and limitations

- NO APK/build or test has been run since this branch diverged from main. Source-level changes and test cases exist, not runtime proof.
- The downloadable Qwen3-VL requires approximately 1.6 GB for two checkpoints and significant additional runtime memory; mobile throughput is unmeasured.
- `llama.cpp` multimodal `mtmd` is an experimental API and native compatibility with this device/model remains to be demonstrated.
- A Qwen3-VL visual observation is a probabilistic model output, not guaranteed anatomical verification. It is explicit review evidence, not an unreviewed quality grade.
- Separate per-identity rendering improves the conditioning problem structurally but does not prove that a generative diffusion model can render three geometrically consistent views.
- Real simultaneous native image-context expansion would require changes to and tested exports of the underlying image text-encoder and diffusion graphs. This project currently provides VERIFIED SEQUENTIAL conditioning, not a new longer-context checkpoint.
- Neural LoRA adaptation and benchmark-based visual-quality improvements remain unverified until usable training data, compute and image generation are available; no paid GPU is authorized.
- No user methodology rules were discarded or added by these engineering changes.

## Next verification gates before release builds

1. Android CMake/JNI compilation and Kotlin unit-test execution **only when the owner lifts the no-build restriction**.
2. Import/install both Qwen3-VL files, start/stop real native vision inference, and measure memory/latency.
3. Generate an approved 1-sheet/five-character Phase-1 test; verify all 15 actual front/side/rear images, anatomical correctness, backgrounds, concept embodiment and differentiation.
4. Submit detailed user-approved grades for failing regions; verify the exact masked correction was applied while unmasked pixels remain bit-identical.
5. Benchmark multiple generation attempts against prior output before claiming higher visual quality or release readiness.

# Self-contained Asterion Art neural model packaging (source-only)

**Status (2026-10-27): code committed, package NOT built, NOT device tested.**

The owner requires the AI models to be contained in the installed app, not fetched from Hugging Face by the phone. The default package spec embeds *exactly* these pinned Apache-2.0 components:

| Role | Model | Exact expected bytes |
|---|---|---:|
| Design / interpretation reasoning | Qwen3 1.7B Q4_K_M GGUF | 1,107,409,472 |
| Real image interpretation | Qwen3-VL 2B Q4_K_M GGUF | 1,107,409,952 |
| Image-to-visual-embeddings projector | Qwen3-VL Q8_0 mmproj GGUF | 445,053,216 |
| **Total** | | **2,659,872,640** |

These GGUF checkpoints are NOT interchangeable with the separate image-generation model. The neural image-generation QNN engine is already packaged as native runtime code; its actual multi-GB diffusion checkpoint is **not** included. This remains a separate blocking deliverable.

## Source-authoritative integration

- `modelpack/models.json` is the immutable three-model source contract: exact filenames, repository, length and SHA-256.
- `modelpack/prepare_embedded_models.py` downloads and SHA-256 checks those weights **on the authorized GitHub packaging runner only**. No weights enter the repository. Each weight is partitioned into 64 MiB `.chunk` assets in `app/build/generated/embeddedModelsAssets/asterion_bundle/`.
- `app/build.gradle.kts` includes the generated assets and refuses to assemble a release with the model manifest absent. Automatic debug builds remain lightweight and need no model data.
- `EmbeddedModelInstallWorker` starts at app launch when a full offline pack is present. It reads the local APK asset chunks, verifies SHA-256 per chunk and complete GGUF, and atomically installs into app-owned files / supported SD-card app storage. It never contacts any server or asks the user to select a model file.
- Models already installed with matching metadata are reused; on interrupted extraction incomplete `.partial` files are reset and reconstructed exclusively from the same APK, without network.
- Reasoning and Advanced Grading UIs show embedded-model preparation and readiness, not Get from Hugging Face.
- `modelpack/verify_apk_embedded.py` validates the ACTUAL APK payload against pinned source checksums, independent of Android compilation.
- `.github/workflows/embedded-model-packaging.yml` is a **manual-only** verification workflow, gated by explicit owner authorization, with **no release or APK upload**. It has NOT been dispatched.

## Distribution and installation are unresolved

1. The three selected neural weights occupy 2.66 GB alone. The APK will be bigger after Kotlin, native QNN, RealSR and other resources. We have NOT verified that Android will accept this single APK.
2. GitHub Releases permits only assets smaller than 2 GiB each. Packaging an APK does not magically make a single downloadable release file under that limit. A tested distribution mechanism is necessary.
3. On-device first-launch materialization requires additional writable space for native GGUF file paths. The app prefers its available app-owned external/SD storage when possible; no user-managed file downloads are needed.
4. The device's available RAM and performance have not been validated for Qwen3-VL. CPU inference is the current conservative default; no false claim of Qualcomm acceleration.
5. Long-image-prompt cross-attention remains limited by whichever actual diffusion checkpoint is installed. This model pack does NOT implement a larger diffusion text encoder.
6. No compatible, license-approved, *device-proven* converted SDXL/Anima/DiT checkpoint was previously approved for redistribution as the app's default. For example, published QNN checkpoints commonly exceed 3 GB EACH and require platform-version compatibility validation. Do not attach arbitrary model weights or pretend the runtime itself is an image model.
7. Persistent release signing needs the existing trusted key, which is not available in the GitHub repository. Do not silently create a new signing identity and call it an upgrade.
8. No money spent. No PC access. No Android build performed.

## Packaging preparation (not yet authorized to run)

```sh
python3 -m unittest discover -s modelpack -p 'test_*.py' -v
python3 modelpack/prepare_embedded_models.py --output app/build/generated/embeddedModelsAssets
python3 modelpack/prepare_embedded_models.py --output app/build/generated/embeddedModelsAssets --verify-only
# Build requires explicit owner's authorization:
# gradle --no-daemon :app:assembleRelease
# python3 modelpack/verify_apk_embedded.py --apk app/build/outputs/apk/release/app-release-unsigned.apk
```

A PASS from these scripts verifies package bytes, not model inference quality or installability.

## Required next engineering gates

- Choose and verify image-generation QNN checkpoint licensing, chipset and model file layout. Decide safe packaging method for multi-GB assets.
- Verify Android native compilation, Runtime `mtmd` operation and image-model decode on actual device (only after build restriction is lifted).
- Make a demonstrably installable, persistently signed offline distribution that fits the publishing channel's limits.
- Run at least one full clinical Character Creation 5-character, 15-view sheet; inspect real visual fidelity and validate targeted correction.

## Additional source integrity gate (2026-10-27)

- Android now treats a manifest without its declared model chunks as **not packaged**, instead of announcing that the neural models are included. This lightweight presence check is not an SHA-256 test; complete checksums are verified on extraction.
- Before any manually authorized release asset merge, Gradle invokes the stdlib-only offline `--verify-only` packager to hash every staged chunk and reconstruct the pinned complete-model SHA-256. Configure `ASTERION_PYTHON` only if the packaging host does not provide `python3`. No model download or Android build is initiated by this document or commit.
- Post-assembly verification refuses misleading claims that image diffusion weights have been included when only the reviewed reasoning/VL/projector baseline has been staged.
- This is **not** a complete integrated image-generation distribution. Installing a QNN checkpoint still requires a separate engineering and licensing decision, and final delivery must be tested against Android/publishing-channel size limits before any release.

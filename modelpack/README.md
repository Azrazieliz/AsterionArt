# Asterion Art — embedded, private-use offline models

**Source-only state: NO APK build, NO installed model, NO runtime test.**

The app is being prepared for the owner's private Android use. It does not
require distribution through Play or a public model-sharing service. The
installed program must not download its own models or call remote inference.

## Pinned actual weights

| Component | Upstream exact file | Upstream digest |
|---|---|---|
| Planning | Qwen3-1.7B-Q4_K_M.gguf (1,107,409,472 bytes) | `b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897` |
| Pixel-conditioned vision language | Qwen3VL-2B-Instruct-Q4_K_M.gguf (1,107,409,952 bytes) | `089d75c52f4b7ffc56ba998ffc50aae89fcafc755f9e7208aacca281dca6c2ae` |
| Vision projector | mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf (445,053,216 bytes) | `f9a68fabba69c3b81e153367b2c7521030b0fa8bb0de400c9599c8e6725f9c82` |
| Image generation | xororz/anima-qnn/rin_flanime_v1_turbo_qnn2.28_8gen3.zip (approximately 4.85 GB) | `e99dc63e7f2d6c125a44dbf0a347c011395fc60adc9a796cc5d02f1ee9e75b8c` |

The precise image ZIP byte count will be measured by the packager (never
fabricated from the upstream rounded size). The three GGUF files total
2,659,872,640 bytes. The image checkpoint is the existing preferred Rin
Flanime Anima QNN model, **not** the untested Fusion B merge. No image-quality
or on-device compatibility claim is made by selecting this package.

The Qwen GGUFs identify Apache-2.0 upstream licenses. The converted Rin QNN
repository does not publish a model card with definitive downstream reuse and
redistribution terms; it is treated as a **private-use integration only**.
Do not turn this source package into a public redistribution without resolving
model and Qualcomm runtime license terms.

## Build-host preparation (NO APK built)

```sh
python3 -m unittest discover -s modelpack -p 'test_*.py' -v
python3 modelpack/prepare_embedded_models.py --output app/build/generated/embeddedModelsAssets
python3 modelpack/prepare_embedded_models.py --output app/build/generated/embeddedModelsAssets --verify-only
python3 modelpack/prepare_embedded_image.py --output app/build/generated/embeddedModelsAssets
python3 modelpack/prepare_embedded_image.py --output app/build/generated/embeddedModelsAssets --verify-only
```

These scripts retrieve weights on the build host, verify their exact published
SHA-256, and create model chunks **inside generated Android assets**, never in
Git source. Existing Qwen assets are kept under `asterion_bundle/`. Image QNN
archive chunks are staged under `asterion_image_bundle/`. Neither workflow
nor script executes Gradle or downloads anything onto the installed device.

The Android release asset-merge task now verifies **both** full staged model
sets and fails closed if either is incomplete. `verify_apk_embedded.py` will
independently inspect both sets inside an actual APK **only after the no-build
restriction is lifted**.

## First launch, no manual file management

The existing worker reads the packaged Qwen models, reconstructs verified
filesystem GGUFs, then invokes the embedded Anima installer. The image archive
is read sequentially from the packaged 64 MiB chunks and streamed directly
into the existing `NativeModelLibrary` QNN tensor directory. Its complete
SHA-256 is checked **before** atomic publication. No temporary full ZIP copy,
content-provider chooser, cloud connection or separate model download.

Interruption or a wrong digest deletes the unpublished partial native
checkpoint. A successfully registered model is reused on the next launch.
The existing optional user-driven import library is preserved.

## Unavoidable installation-engineering blocker

A private application containing these mandatory model weights needs more
than 7 GB of packaged assets before executable/native code. **This is too large
to assume a single normal Android APK is installable.** The code here prepares
the mandatory data, but does NOT prove that Android's APK ZIP processing,
installer, Gradle packaging, device storage or signing can handle such a
single file. APK packaging and installation strategy remains technically
unresolved; it cannot be hidden by a build-time checksum manifest.

A non-Play, fully offline split-install package is a candidate; it must be
investigated and verified before adopting its installation behavior. The
owner's requirement remains that all weights are supplied with installation,
not fetched later. There is no assumption of publishing/sharing the app.

## Remaining quality gates

- Compilation and actual Android installation, only after owner permits builds.
- Real decoding with the pinned Anima QNN package on the target handset; correct
  routing, runtime libraries, thermal/storage/RAM requirements.
- Actual Qwen3-VL multimodal vision analysis and truthful machine uncertainty.
- Visually inspect five authored characters and all fifteen front/side/rear
  views in a complete Phase-1 sheet; evaluate anatomy, fusion, identity, style.
- Train, compare and validate genuine compatible adapters before claiming
  semantic self-learning or improved aesthetic inference.
- Preserve the original rich taxonomy and other Originals' methodologies.

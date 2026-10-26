# Source architecture, actual implementation, build and integration boundaries

## Snapshot and lineage
Current `main` `88206baa9e2a33d41195834783023a9fb0c7e0dc`, archived legacy `db68e11c411ae120b7d08e5e8aa5914608a7a404`; keep all other branches and PRs intact. Current app Gradle `applicationId=com.asterion.art`, `namespace=com.asterion.art`, `versionName=0.2.0`, `versionCode=2`, compile/target SDK 35, min SDK 29, Kotlin 2.0.21, Compose BOM 2025.04.01, WorkManager 2.10.1, ML Kit Text Recognition 16.0.1, ARM64 filter; source archive provides exact values.

## Modules and files
- `app/src/main/java/com/asterion/art/ui/AsterionArtApp.kt` (large Compose shell, Home, Create, Sheets, Models, Queue, Grading, parameters); `CreativeRoutes.kt` routes Image/Animation and Artwork/Defaults/Sheets; `MethodologyStudioV3.kt` displays method-specific concepts/directions, input picker, staged catalogues/preview; `NativeModelLibraryScreen.kt` model manager with source type/tag.
- `methodology/DetailedMethodologies.kt` authoritative typed contracts: `OutputGeometry`, `MethodologyContract`, `MethodologyRequest`, `SourcePolicy`, `StageAction`; v3 Character 3×(5 identity designs) geometry; all default workflows protected systems/allowed changes. `MethodologyRegistry.kt` older registry, check current usage before changing.
- `app/src/main/assets/methodology/concepts_v3.json`: 455 canonical selectable concepts. `model/ArtModels.kt`, `model/SheetLayouts.kt`, `data/ArtRepository.kt` (persist jobs, sheets, ratings, settings/selection), `render/SheetRenderer.kt` (deterministic source-visual grids; not AI hallucinated microtext).
- `engine/GenerationPlanner.kt` compiles sources and 5-pass structured specifications, `SheetTextReader.kt` OCR, `SourceReferenceEncoder.kt` reference conditioning, `LocalDreamBridge.kt` HTTP/SSE to native inference, `nativeengine/EmbeddedNativeRunner.kt` starts same-app ARM64 native process, `EmbeddedNpuEngine.kt`, `NativeModelLibrary.kt` large ZIP installer/markerless handling, `OriginalCheckpointLibrary.kt` classifies raw source weights without claiming QNN runnable, `ModelOutputRouting.kt` model tags.
- `work/GenerationWorker.kt` candidate branch, img2img multi-pass, foreground status, persistent checkpoints, thermal handling, quality score and Core sync, `NativeModelImportWorker.kt` extraction progress, `SelectiveEditWorker.kt` user-mask inpaint, `SheetRenderWorker.kt`, `VideoGenerationWorker.kt`; enhancements in `NeuralEnhancementWorker.kt`/`ImageToolboxWorker.kt`.
- `enhancement/EmbeddedRealSr.kt` MIT-licensed neural super-resolution invoked as embedded executable with models, `enhancement/AsterionImageToolbox.kt` licensed-style CPU color/tone/sharpen implementation. **Neither is the exact proprietary SuperImage Pro engine**. `integrations/InstalledImageEnhancers.kt` public Android intent handoff to installed apps.
- `video/NightmareVideoBridge.kt` authorized local Wan/Neodragon image-to-video request, `VideoPlanStore.kt`, `Mp4SegmentJoiner.kt`, `VideoResultStore.kt`, `NoWorldExporter.kt`; current video backend may require separate process and port/auth. Do not claim wholly offline embedded diffusion video.
- `core/CoreSheetBridge.kt`, `core/CoreResultUploader.kt`: signature-restricted ContentProvider contracts with Asterion Core. Core's companion provider lives on separate `asterion-art-sheet-bridge-20261023` branch and may not be present in installed Core APK. Matching signing identities required.
- `fusion/merge_anima.py` streaming tensor merge in compatible FP16/BF16 Anima, `fusion/check_remote_tensors.py`, `fusion/preflight_sources.py`, `fusion/fusion_b_sources.json`, `fusion/run_private_fusion_b.py`. Private writable model repo and benchmarking remain unverified.
- `native/prepare_native_runtime.py` pins Local Dream nonfiltered official v3.0.0-alpha.6 APK SHA-256 `bec75b588200eb83aedd82b0a26ed8c5de4cfd923d0f51e1e65d241fccc11f82`; extracts signed/pinned binaries to package. `native/verify_apk_native.py` verifies packaged checksums, `native/prepare_realsr.py` bundles RealSR with notices. License obligations apply: Local Dream CC BY-NC 4.0; other QNN/RealSR/Toolbox license conditions must be retained.

## Source dependencies and external references
- Upstream Local Dream https://github.com/xororz/local-dream
- Nightmare-compatible mobile backend https://github.com/AbrahamPaulJ/nightmare-mobile
- Hugging Face Rin source `nyanpoko/public` at revision `3d37cda6e49af7a3e75db585a0ca735f3e884119`, path `Anima/Model/RIN Flanime Anima/v1.4/rinFlanimeAnima_v14_pruned_fp16.safetensors`, sha `a28d1e04787dcc58507feb7921e8fcbffc051dcf86d8685cf1049eedfc317be2`.
- MiaoMiao `RunningHubAI/rh-miaomiao-harem-v16-unet`, revision `e5ce41bc93f331a417193e7800e78183a7ae87cc`, file `MiaoMiaoHarem_Anima_1.6.safetensors`, sha `6bbb6b6785b4eb0df467658ce47f5f52f1d11fe992b1375b3c67ff02becf193b`.
- Hassaku XL Illustrious v3.4 `SiE69/Illoustrious_Checkpoint_Collection` at `5e00f87fc9c63bcd402272deba59c7806daeea3a`, file `hassakuXLIllustrious_v34.safetensors`, sha `1618edb443d9c641fb01c4961f5875d5f01b1a851fd9f2ea64623ceac82257e0`. **Separate SDXL family; no direct Anima fusion.**

## Verified / unverified
**Verified:** public Git commit existence/paths, CI building and running 23 unit tests on latest artifact, source ZIP and Core source snapshot downloaded, actual Methodology.zip / image references available to user. Original 24 Oct device ADB inventory is earlier-session evidence; reverify connection in next session.
**Not verified:** runtime 5-pass image output on S26 Ultra; loaded QNN model; model files surviving app update; SuperImage Pro embedded API (not authorized); multi-keyframe native video; simultaneous/parallel 6–7GB model memory behavior; Core signatures/round-trip; actual Fusion B weights; user acceptance of UI.

## Device and update constraints
Samsung SM-S948B S26 Ultra, Android 17 (as observed 24 Oct); remote laptop `LAPTOP-1LI4VRCJ` via Remote Desktop Commander when permitted. Earlier observed installed app package IDs: `com.asterion.art`, `com.ailm.android`, `io.github.xororz.localdream`, `com.zhenxiang.superimage.pro` v3.22.0, `ru.tech.imageresizershrinker` v4.2.0-rc07, `com.tumuyan.ncnn.realsr` v1.13.2. Android ACTION_SEND image/png target detection verified previously, not deep access to proprietary code. Existing QNN model library ~6–7GB per model; Core image library up to 300,000+ assets.

## Signing risk (record from prior continuation)
Different ephemeral CI debug certificates in prior builds:
- Build `92285d2`: SHA256 signer `f66500485b0a1051a101d1b2b4846f8f15a989507808142965593e9c230924c6`.
- Earlier UX build: `624c52a651bfed7268e2b6ced5a5e72f6c0fa029cb0b1b96b36d7a7caaa1b40e`.
- First Art build: `2e665c746d5c6f4b759542db39648eba37e68dab8100e7f4703630a0a99af9ee`.
Do not uninstall/update until signer checked on phone and stable signing arranged. These fingerprints were recorded in a prior handoff; no live ADB validation took place during this migration-only task.

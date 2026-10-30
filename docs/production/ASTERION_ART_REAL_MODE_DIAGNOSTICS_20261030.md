# ASTERION ART — REAL MODE ON-DEVICE RETEST (2026-10-30)

**Scope:** Diagnostics of installed owner-signed `com.asterion.art` v0.2.3 (`versionCode=5`) on Samsung SM-S948B / Android 17, using authorized laptop ADB. **No sphere tests.** This report records observed failures, not speculative fixes or successful production synthesis. No APK installation, application data clearing, signing change, model deletion, checkpoint rewrite, native-engine replacement or optimization was made.

## Observed tests

### 1. Creature Creation — Anime / Route A / Alicorn (five distinct bodies in one 16:9 image)

- Canonical concept: Alicorn (`SC631`), preserved selection.
- Actual user-facing direction entered: `Five distinct Alicorn creatures with coherent anatomy feathered wings spiraling horns unique silhouettes vivid gemstone palettes full body anime illustration`.
- Model: installed Qwen3 1.7B Q4_K_M and existing Anima QNN image checkpoint (image checkpoint not reached).
- The previous full-production **16K** test had Android ANR during Qwen pre-render, with no non-sphere image; see `ASTERION_ART_V023_REAL_PRODUCTION_DEVICE_AUDIT_20261030.md`.
- **4K** new execution: saved task `c9e18c69-0b30...` failed in pre-render with the exact visible error: `Complete owner methodology and reference evidence exceed the installed reasoning model's measured context; do not silently truncate or recursively rewrite authority.`
- **8K**: changed local Brain preference to 8192, verified `Context: 8192 native LLM tokens` in Models. Requeued the existing failed job using `Resume image from saved checkpoints`. The Android foreground service began around 22:52:11 and stopped around 22:52:13. After refresh, the task still displayed the same full-authority context-capacity failure. No new actual creature image was produced. A repeat 16K hang was deliberately avoided.

**Boundary:** The 4K attempt is definitively insufficient for this job. The UI-confirmed 8K preference did not clear the block, but is **not proof that inference actually received 8K**: `AsterionReasoner.contextForRam()` silently caps runtime context to 4096 tokens when `ActivityManager.MemoryInfo.availMem < 2 GiB`, and to 8192 when free memory is below 4 GiB. The earlier audit saw only ~930 MiB free. Capture actual `contextSize` and free RAM for each attempt before interpreting a 4K/8K comparison. Neither failure proves diffusion step count, image-model quality, or a suitable longer-context architecture. The earlier 16K ANR remains an unresolved separate problem.

### 2. Theme Creation — real text-only five-theme request

- Used the actual Theme Creation text workflow, B/E/T values displayed as 1/1/1, keywords `crystalline alicorn,storm,ruined citadel`, and the normal `Create five independent text themes` action. No image generator was requested.
- **FATAL ANDROID APPLICATION CRASH at 2026-10-30 22:54:57.698 CET**, PID 10576, triggered when WorkManager's `SystemForegroundService` attempted foreground promotion for `ThemeTextWorker`.
- Captured exact exception:
  `java.lang.IllegalArgumentException: foregroundServiceType 0x00002000 is not a subset of foregroundServiceType attribute 0x00000001 in service element of manifest file`.
- Confirmed directly in source: `app/src/main/AndroidManifest.xml` declares `androidx.work.impl.foreground.SystemForegroundService` with only `android:foregroundServiceType="dataSync"`, while `ThemeTextWorker.show()` requests `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING` on API 35+.
- This source-to-runtime configuration mismatch prevents reaching the Theme reasoning stage. It is a confirmed coding defect, **not evidence of Qwen failure**.
- Android offered `Clear cache`; this action was **not** performed. The dialog was dismissed via Cancel; the same installed app reopened. Crash evidence is saved locally, without committing private device dumps.

### 3. Character Creation — real Phase 1 design preparation

- Entered dedicated Character Creation Route A with the previously selected canonical concept slots `Black Hole`, `Asteroid`, and `Bismuth`; original existing FAILED population (Gemma request timed out at first character) was preserved, not deleted.
- Started a *new* Phase 1 population through `Resolve all characters before generation`: 1 requested 16:9 sheet, 5 independent female character records, full front/90-degree profile/rear views (15 views in one image as eventual output).
- Android confirmed `CharacterPreparationService` running as a foreground service, type `0x2000`. It was **not** blocked by the WorkManager manifest mismatch, which is specific to WorkManager's separate service declaration.
- During the observation window the UI remained `DESIGNING · 0/5 complete frozen anatomical identities`; no produced identity or image certified. **Active/unverified, not a pass, do not falsely mark failure if it later completes.**
- Source `CharacterBrainResolver` uses the shared `BrainModelStore.selected()` but error/progress strings hard-code `Gemma 3`, making runtime model identity ambiguous until exact selection and telemetry are inspected. Individual semantic calls have a 3-minute timeout; output and prefill timing require real evidence.

## Engineering findings and next gate

1. **P0 / certain:** WorkManager foreground-service declaration lacks `mediaProcessing` while ThemeTextWorker requests it on Android 17. Repair requires matching manifest and worker types and then a new appropriately signed APK/device validation. **No repair or install in this diagnostic pass.**
2. **P0 / confirmed:** Full Creature methodology cannot fit the tested 4K context; the requested 8K attempt remained blocked, but RAM-based runtime clamping may have reduced it to 4K again. Log actual context, raw token count, output reserve, and free RAM. **Do not truncate binding owner authority, split it into incoherent diffusion calls, or confuse Qwen planning with denoising.** Compare measured token counts and memory constraints, plus diagnosis of the previously observed 16K ANR, before choosing an architecture.
3. **P0 / still unverified:** Character has a live preparatory service but no first frozen character in the observation window. Check saved stage/TTFT/native load, avoid simultaneous competing NPU workloads, and do not relabel waiting as success or as failure.
4. **Coverage:** Artwork requires Character, Theme and Art Style source sheets; other Originals require their documented sources; Wan 2.2 video lacks its required model components. Absence of prerequisites is not mode correctness or failure of a generation engine.
5. **New rule:** use real task/methodology/output contracts only. No blue sphere, smoke-test outputs, mock prompt previews, repeated full-context crashes, speculative model tuning, data wipes, or unsigned/mismatched APKs.

## Evidence location

All collected ADB crash/events and runtime logs are on the **authorized laptop only** under `D:\AsterionArtSigning\audit-v023-real-modes-4k-20261030\`, including:
- `alicorn-4k-logcat.txt`
- `theme-text-8k-crash-buffer.txt`
- `theme-text-8k-events.txt`
- `theme-text-8k-logcat.txt`

These logs may contain other app/system information and must not be blindly published. Existing installed certificate and original 5.73 GB of restored private model/data bytes were not modified by testing. This report is documentation only; PR remains draft.

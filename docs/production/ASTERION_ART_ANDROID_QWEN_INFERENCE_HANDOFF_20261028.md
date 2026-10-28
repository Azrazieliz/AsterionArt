# Asterion Art — Android Qwen inference continuation (2026-10-28)

## Operational authority and scope

Repository: `Azrazieliz/AsterionCompiler`
Branch: `research/model-capabilities-no-build-20261027`

**This is not a restart.** Preserve existing repository and all user character plans, QNN model files, generated assets, source references and methodology. Do not run `adb uninstall`, `pm clear`, delete any `files/asterion-qnn-models` files, replace signing identity, or reinstall across mismatched certificate signatures. The owner is using an Android phone. PC access beyond expressly requested phone/ADB diagnostics is not authorized.

User's observation: Character Creation and other design/planning tasks spend unacceptable time in **Qwen3 / LLM reasoning before image generation has begun**. Do NOT attribute this reasoning delay to diffusion step count. Local Dream used 30 image-generation steps successfully; that comparison is a separate image inference path.

## Actual ADB evidence (not an assumption)

ADB device: Samsung SM-S948B, Android 17. At the time of inspection the installed `com.asterion.art` was version `0.2.0`, installed 2026-10-27 20:27, `minSdk=29` (older than current GitHub release source). Device had ~5.3 GiB Anima QNN model data and ~198 MiB runtime libs. WorkManager database could be read with `run-as com.asterion.art`; no app reset was made.

Saved independent **character reasoning plans**, all with `frozen=0`:
- `4dcd165b-17a7-45e6-a7bc-af911116fc46.json`: FAILED, `No value for pairwise`.
- `776a4e90-452b-4c2e-925c-a7041a0b645e.json`: FAILED, `No value for pairwise`.
- `ace49395-8954-4cf6-8b8a-d3b0f3fe9f03.json`: FAILED, truncated/unterminated Qwen3 JSON, nested design fields.
- `bde21b5f-72c7-45af-b51c-07ed7655f6e2.json`: FAILED, `No value for face_eyes_hair` (the screenshot; 3 sheets).
- `c14646b3-83d0-4da7-8ef7-b854ae91b294.json`: FAILED, `No value for pairwise`.

The phone had ~26 minutes of an Asterion foreground work session (18:59–19:25) and reported at least one job timeout. Android recorded an older low-memory process termination. These do not establish the cause of each failed plan.

**Separate finding, not the current bottleneck:** `cache/asterion-native-last.log` shows embedded QNN image diffusion graph execution through UNet step 11 and VAE initialization during a standalone model smoke test. That does **not** prove the character reasoning path succeeded, and its ~26 seconds per image diffusion step must NOT be cited as the user's reported *pre-image* delay.

## Changes already committed on this branch (not yet phone-verified)

- `4773a50c`: CharacterBrainResolver splits flat eight-field identity JSON from separately generated exact pairwise anatomical mechanisms, repairs absent/short fields by targeted **real Qwen3 inference**, and bypasses redundant authority re-summarization when complete authority fits Qwen context. No invented/template-based anatomy.
- `bdf07c10`: AsterionReasoner general planner directly passes through complete source authority if it fits comfortably, avoiding a needless CPU LLM summarization pass; retains original source evidence.
- `73481af2`: Persist `progress_stage` in CharacterPlanStore, refresh in Character Original UI, and push specific model-stage updates from CharacterPreparationWorker. Does not fake percent completion.
- `c5542ce2`: CharacterBrainResolver now respects the shared DeviceInferenceGate, preventing heavy CPU Qwen3 reasoning from concurrently loading another huge QNN model; clean-up releases the gate on cancellation/failure.

Run GitHub Android integration/unit build for HEAD; previous prior-to-patch successful workflow #40751622815 at `17cd75bd` **does not validate newer commits**. Always inspect latest HEAD/CI.

## Critical remaining work

1. Confirm **new HEAD** compiles and unit tests succeed. Fix any Kotlin/Android errors. Add actual test coverage for incomplete Qwen3 structured output (missing face, missing pairs, truncated/nested JSON), robust pairwise coverage, cancellation, and persisted stage.
2. Measure real on-device **Qwen3** inference latency per stage (model load, canonical evidence, design synthesis, field generation and pairwise mechanisms), using non-sensitive telemetry. Unit tests/CI alone never validate speed.
3. Test one **real** Character Creation population of five on phone end-to-end: complete five frozen identities, no schema omissions, valid physical pairwise interactions, no template/copy substitutions, actual elapsed durations. Only then test 3+ sheets. Avoid a 15-person default stress test as the first diagnosis.
4. Inspect other Qwen3 routes using `AsterionReasoner.compile` for unnecessary reprocessing and malformed structured output. Do not sacrifice binding methodology or canonical knowledge merely to suppress errors.
5. Avoid confusing the latest newly signed **GitHub APK** with the app on phone: the latest published new-key release is based on `b5d7c8b9`, not these patches. The newly generated release key is private and is **not** in GitHub. An APK signed by a different certificate cannot update the currently installed package without a safe data migration; **never uninstall without approval**.
6. Continue finishing app; report implementation, CI, and **device runtime validation** as distinct claims.

## Source paths

- `app/src/main/java/com/asterion/art/brain/CharacterBrainResolver.kt`
- `app/src/main/java/com/asterion/art/brain/AsterionReasoner.kt`
- `app/src/main/java/com/asterion/art/work/CharacterPreparationWorker.kt`
- `app/src/main/java/com/asterion/art/methodology/CharacterPlanStore.kt`
- `app/src/main/java/com/asterion/art/ui/CharacterOriginalScreen.kt`
- `app/src/main/java/com/asterion/art/nativeengine/DeviceInferenceGate.kt`
- `app/src/main/java/com/asterion/art/nativeengine/EmbeddedNativeRunner.kt`

## Immediate next action

Check latest GitHub CI and repair any failing implementation. Keep the response concise and factual. Test the **Qwen3 reasoning task**, not the unrelated 30-step image diffusion task. Do not claim the app works until an actual Qwen3 character task completes on the device.

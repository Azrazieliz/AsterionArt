# ASTERION ART v0.2.3 — Real-generation failure inventory (device, 2026-10-30)

**STATUS: DIAGNOSTIC EVIDENCE ONLY. NO CODE PATCHES, NO MODEL CHANGES, NO APK INSTALL/UNINSTALL/RESET.**

Tested **the already installed owner-signed `com.asterion.art` v0.2.3** on Samsung SM-S948B, Android 17, with existing Qwen3 1.7B Q4_K_M and local Anima image checkpoint. Installed APK source: Android CI `60da233ef4eebd49023aea065940855322d3feb3`. This report distinguishes an actual generation result, a UI/precondition inspection, and untested/blocked functionality.

## Evidence: actual non-sphere Creature Creation

1. Navigate `Create → Image → Originals → Creature Creation → A · Direct`. Select canonical `Alicorn` (`SC631`), anime.
2. Submission: `Five fully original alicorn creatures with distinct body architectures crystalline feathered wings spiraling horns vivid jewel tones and detailed flowing manes full body illustrations on a white background`. Contract requires **one 16:9 image containing five original distinct creatures**, not sphere diagnostic. The source illustration is optional for Creature.
3. Tap `Start in background`. Home displayed `STILL IMAGE · Loading Qwen3 1.7B Q4_K_M with 16384 tokens`, subsequently `One bounded Qwen3 1.7B ... compilation of complete original authority`.
4. Android repeatedly showed `Asterion Art isn't responding`. Event buffer records **22:09:53.172**: `am_anr` for PID `24042` with **`Input dispatching timed out ... Waited 10000ms for MotionEvent`** for `com.asterion.art/.MainActivity`.
5. ANR dialogs persisted minutes; Android event buffer records process **killed 22:15:12.992 after user request following error dialog**. This is NOT evidence of an uncaught `java.lang.OutOfMemoryError` in Asterion. RAM snapshot around period: ~11,084 MiB total, ~10,154 MiB used, ~930 MiB free; a memory contribution is suspected **but not proved**.
6. App later reopened, Home showed 0 active queue and no new non-sphere output; only earlier sphere diagnostics visible. **Real mode result: FAIL before native image output.** The state of the original failed job cannot be established solely from Home's counter.
7. A separate attempt to compare 4K vs 16K context was initiated (the 4K control was visibly selected) but **was not executed to inference**, due foreground-app changes during setup. No comparative performance conclusion is warranted; original 16K selection was tapped in a context-restoration procedure. The audit did not alter model files or app code.

Raw evidence on the authorized laptop:
`D:\AsterionArtSigning\audit-v023-no-patches\events-audit.txt`,
`real-creature-logcat.txt`,
`post-crash-events.txt`,
`filtered-app-log.txt`, and selected named screenshots/UI XML. These contain OS diagnostic data and are **not uploaded to public GitHub**.

## On-device acceptance and prerequisites observed (not substitutes for mode generation)

- Existing Testing Center acceptance verified `Sheet output | PASS` with four reference sheet roles rendered/published/reopened/dimension-checked, and `Protected editing / Toolbox | PASS` with selected-region edits preserving untouched pixels and PNG export. These are functional low-level media tests, **not image diffusion or visual fidelity**.
- Native image executable and Qualcomm QNN libraries: reported `PRESENT`. Installed image model: `rin_flanime_v1_turbo_qnn228_8gen3 [anima]`, reported `PRESENT`; **presence does not establish compatibility**.
- Local Qwen3 1.7B Q4_K_M and Qwen3-VL projector: reported present, not a proof of authoring or VLM result.
- Asterion Core provider: `NOT CONNECTED`; character-sheet import through Core cannot be verified.
- Wan 2.2 video: `NEEDS MODEL` (required diffusion/VAE/UMT5 missing); genuine video output cannot start. UI says model files required.
- Artwork page initially has zero completed reference sheets. The approved Artwork workflow requires Character, Theme, Art Style sheets (or approved role-appropriate reference images). Cannot execute the five-phase pipeline from missing sources.
- The existing diagnostic image smoke worker hardcodes a **blue ceramic sphere**, so a sphere from that worker must NEVER be counted as a full image-production mode test.
- Source audit confirms Route B exists for non-Character modes and Artwork via `ModeTextBoard`; no actual complete Route B native render has been verified.
- Earlier photo of Creature screen showed Alicorn retained as selected after restart but the additional visual instructions field was empty; this may be expected reset or a draft-persistence defect — **not diagnosed**.
- Observed UI awkwardness: 455-item flat concept selector requires alphabet filter/long scrolling; it is not a block on the actual test with Alicorn. Do not call this a proven defect without quantitative UX threshold.

## Mode-by-mode status (do not equate screen presence with working production)

| Mode | Scope | Verified status | Why |
|---|---|---|---|
| Creature Creation — Route A, Anime | Real output: five different creatures | **FAIL · Android ANR** | Valid prompted real generation was queued; Qwen pre-render froze; no image |
| Creature Creation — Route B / Realistic / Cosplay | Same image-production modes | **NOT TESTED** | No successful shared native model/brain path yet |
| Character Creation | Five distinct anatomical characters per sheet, A and B | **NOT TESTED to image** | Requires own Qwen freeze/validation; shared original-generation blocker |
| Outfit Creation | Ten different outfits, source identity preserved | **BLOCKED BY SOURCE / NOT TESTED to image** | No character source or saved Character Sheet selected |
| Weapon Creation | Ten isolated designs | **BLOCKED BY SOURCE / NOT TESTED to image** | Character / selected frozen outfit prerequisites |
| Transformation | Five variants of same protected identity | **BLOCKED BY SOURCE / NOT TESTED to image** | Character source missing |
| Mutation | Five structurally mutated variants | **BLOCKED BY SOURCE / NOT TESTED to image** | Character source missing |
| Creature (other output types) | Five originals | **NOT TESTED** | Shared original pipeline already demonstrated ANR in Anime |
| Alternate Universe | Five original-world adaptations | **BLOCKED BY SOURCE / NOT TESTED to image** | Character source missing |
| Feminisation | One fully redesigned 9:16 female | **BLOCKED BY SOURCE / NOT TESTED to image** | Source image required |
| Art Style Correction | Re-render preserving source using target style sheet | **BLOCKED BY SOURCE / NOT TESTED to image** | Source illustration and Art Style Sheet required |
| Artwork A/B, Anime/Realistic/Cosplay | Combine references, 5 image phases, 1 winner/phase, automatic enhancement | **BLOCKED BY REFERENCES / NOT TESTED** | No completed Character/Theme/Art Style sheets available |
| Theme Creation text-only | Five independent themes B/E/T/roulette/approval, separate Theme Sheet image bridge | **NOT TESTED END-TO-END** | Text-mode native Qwen output not verified; not direct image generation |
| Sheet Creation | Source-to-reference-sheet via actual AI | **PARTIAL** | Media roundtrip PASS, true AI sheet synthesis/method fidelity not demonstrated |
| Animation/Wan video | Real neural 10–30 s | **BLOCKED: missing model components** | Runtime contains engine but model setup 0/3 |
| Core integration | Character source retrieval | **BLOCKED: provider absent** | Testing screen reported NOT CONNECTED |
| Image Toolbox/Selective edit | Protected pixel/edit export | **PASS limited media contract** | Does not verify AI interpretation of scene/reference |

## Defects confirmed vs unknown causes

**P0 confirmed**: Native Original pipeline hangs at Qwen pre-render and produces Android **ANR** on a real five-creature request. A full, usable generated image was NOT produced.

**P1 confirmed prerequisite/integration gaps**: Asterion Core provider not connected; Wan model prerequisites missing; the Artwork/Style routes have no required owner-created sheets currently available. These may be missing user assets rather than a code defect, so report as **blocked**.

**P1 delivery/diagnostic limitation**: Testing Center's hard-coded sphere smoke test proves only a minimal picture. It does not probe character fidelity, five-figure layout, reference preservation or full phase methodology. The real production test exposes that coverage gap.

**UNDETERMINED**: Whether 16K Qwen KV/context allocation, Java main-thread blockage, device thermal throttling, the active memory budget, foreground/background service design, or native Qwen model loading caused the ANR. The 10s input dispatcher timeout is directly evidenced. A real Java OOM or SIGSEGV was **not** evidenced in the available event/crash buffers for Asterion.

## Owner instruction

**DO NOT PATCH ANYTHING** until the owner reviews the defects. No architecture additions, speculative taxonomy changes, reinstall, model pruning, key rotation, data reset, or model conversion may be done as part of this audit. Continue real production tests only with bounded failure handling and with explicit source prerequisites; never mark untaken tests as PASS.

**No private data, model or release-key secret is included in this report.**

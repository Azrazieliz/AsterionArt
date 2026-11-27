# PROPOSAL — Asterion Art reference conditioning, sheets and LoRAs (2026-11-27)

Status: proposal only; not approved methodology changes or new mandatory controls. Binding decisions: ../production/OWNER_APPROVED_NEURAL_RECONSTRUCTION_20261127.md and ../production/OWNER_APPROVED_TAILORED_MODEL_ARCHITECTURE_20261127.md. Original sheet presentation and Original methodologies remain authoritative pending owner review. No new style taxonomy.

## Rationale
Observed Qwen-enabled authoring prevented native image generation; deterministic bypass reduced latency but produced formulaic creative text and repetitive/incorrect character views. The observed outputs alone do not isolate every native engine fault. Separate learned design, evidence, validation and rendering. Never confuse a successful diagnostic sphere with production acceptance.

## 1. Reference package (proposed internal representation)
Keep each of the four existing sheet roles and its human-readable board. Store immutable source URIs/hashes, role, board/version, source facts and confidence, owner corrections, approved/generated reconstruction provenance, meaningful visual crops/masks and optional model-specific conditioning caches. Machine artifacts are separate from the human-readable board, not a substitute. Retain original pixels and map each visual observation to its evidence; distinguish observed, inferred, generated, owner-approved, and unknown. Hash caches by asset, model/encoder, preprocessing and methodology versions. A new generator must be able to rebuild its own reference cache. Character Sheet V5 retains one final native generation call and complete portrait board; do not silently substitute stitched panels. Any future change to that contract requires explicit review.

- Character: identity and consistent external appearance; reusable view/face/outfit detail evidence where appropriate.
- Art Style: linework, edge/shading/color/material rendering grammar; isolate style from subject identity. No style taxonomy or mandatory new LoRA.
- Theme: environment, spatial relationships, composition, mood, light and physical interaction logic; not identity.
- Element: selected object/feature geometry, material, appearance and permitted variations.
- Reuse an exact selected Original candidate as an independent sheet reconstruction source; do not pass its whole multi-design board as the candidate or use text in place of its image.

## 2. Learned adapters and model registry
Three independent production image models (Anime, Cosplay, Realistic). Asterion Brain/Vision architecture is selected by measured comparisons. Adapter types may include general rendering/style, mode competency, identity, pose/view, and correction; these are technical roles, not a new owner taxonomy. LoRA is optional for immediate sheet use. For recurring reference sets, evaluate a trained adapter only if available suitable permitted data and repeated use justify it. Do not claim a one-image adapter reliably learns an entire style or unseen views.

Registry entries record base checkpoint and architecture, target modules, tensor formats, lineage, hash, training data provenance, validated runtime support, quality/latency evaluation, and license. Never attach an incompatible adapter. Dynamic weights are backend-specific; no arbitrary universal strength, no automatic hard count limit in this proposal. Detect conflicts by held-out image comparisons, measured memory and actual inference support. Route with transparent override and logged configuration. Compare runtime loading versus validated premerged derivative variants if mobile native graphs cannot dynamically load LoRA. Preserve originals and rollback. Text/vision adapters are distinct from image/video adapters.

## 3. Conditioning and execution
Normalize selected references -> cache or compute visual evidence -> reason over exact mode authority -> freeze structured design -> compile semantic and actual supported image conditioning (text, image embeddings, spatial masks/pose/depth when available) -> validate compatibility, exact tokenizer limits, coverage and memory -> native generation -> mode-specific quality evaluation -> approved refinement/enhancement -> publish provenance. Role separation prevents applying theme identity or copying Art Style source characters. Do not silently drop unsupported reference roles. Resolve conflicting binding sources visibly; explicit owner edits take precedence within their authorized scope. Automatic repair is localized, bounded, and must preserve frozen identity. Do not claim multiple passes extend simultaneous text context.

Existing Artwork five-phase one-winner protocol remains. Other modes keep their own contracts, including Theme text-only and existing Theme-only B/E/T. No new controls or universal five-pass process are approved. User-selected quality should adjust supported settings/optimization without silently dropping required output content; exact presets remain open.

## 4. Development and validation
Use existing PC/free resources, verify hardware and training feasibility first. Compare unmodified bases, compatible merges, task-specific adapters, feasible fine-tuning, distillation and targeted architecture modifications. Prefer lightweight experiments with frozen holdouts, then expand only if measured gains justify cost. No paid compute or unapproved data use. Evaluate base alone, sheets alone, LoRA alone, and combined, using matched seeds/settings where comparable; identity/style leakage, novel-reference adherence, diverse designs, view accuracy, composition, thermal/memory and end-to-end time. Use real Asterion tasks, not sphere tests. Keep independent anime/cosplay/realistic acceptance evidence. Verify background execution, checkpoint recovery and model handoff on the target phone. Quality references are evaluation targets, not training consent.

## 5. Learning
Only explicitly saved owner grades become human labels. Automatic small updates may include validated routing/preferences or narrowly scoped adapter candidates; exact automation/promotion boundary awaits owner decision. Major changes reviewed. Keep held-out regression sets, model/version provenance and rollback. No silent replacement of base weights, no self-generated approval labels.

## Open decisions
- Meaning of owner's phrase "not the single restriction".
- Whether sheet presentation or one-shot Character Sheet contract should change after individual review.
- Whether repeated characters/styles may trigger optional adapter-training proposals or automatic small updates within approved boundaries.
- Whether external compatible LoRA import and user-visible manual override are desired, in addition to internal automatic routing.
- Exact quality controls, update promotion thresholds, training-data permissions and mode-specific controls. Review each Original's methodology before changing it.

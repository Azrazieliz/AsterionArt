# PROPOSAL — Native intelligence engineering contract v0
2026-11-27. Not approved implementation canon. Supersedes the technical direction of runtime methodology prompting, consistent with the owner correction. This is a shared-system contract, NOT a completed clause-by-clause translation of the Original methodologies. No source methodology changes.

## 1. Decisions and missing evidence
Binding owner decisions: independent Anime/Cosplay/Realistic generators; PC/free-resource training; background execution; quality/time optimization without fixed storage or video-time ceiling; small automatic updates/major reviewed updates; review each Original methodology before altering it; no runtime reading/interpreting methodology text. Existing source/data/signing preserved. LoRA necessity undecided. Current PC GPU/VRAM, available training corpus, deployment kernels and true image reference capacity have not been established in this review. Exact model architectures and weights cannot be certified without experiments. Do not select candidates on general chatbot benchmarks alone.

## 2. Definition of the intelligence
A mode is a versioned executable function F_mode(input, source evidence, seed, learned parameters) -> structured design(s) and, when authorized, output artifact(s). Its executable graph defines permitted inputs, operations, data ownership, required outputs, dependencies, state transitions and invariants. Learned parameters generate the creative decisions and visual representations; they do not choose whether required operations happen. Methodology text is a development/training/test artifact, never a production model input. A user-supplied creative directive is task data, not a substitute methodology. No generic chat loop or generic sentence-template authorship.

Separate the following:
- Mode graph: statically defined operator sequence and type contracts, compiled into app/runtime; versioned, no runtime text interpretation.
- Learned design operators: encode concepts and references, sample alternative structures, fuse relevant attributes, generate coherent design attributes and resolve conflicts. Operate on typed inputs and learned representations; output structured fields and/or visual features, not necessarily natural-language prose.
- Visual encoder: source-pixel features and observed/inferred/unknown evidence; explicit roles and provenance.
- Native renderer: consumes model-specific text embeddings where useful, visual embeddings, spatial controls and selected design features; no claim that an arbitrary latent is compatible with a pretrained diffusion model.
- Evaluator: structural checks in code, learned visual comparisons, owner approval as separate evidence. Machine scores never equal owner grades.

## 3. Shared typed interfaces (proposed minimum)
JobInput {jobId, modeId, modeVersion, route, renderFamily?, seed, selectedCanonicalIds[], sourceRefs[], userDirective?, modeParameters, qualityPolicy}
SourceRef {assetId, role, contentHash, sourceUri, region?, revision, evidenceStatus}
DesignRecord {designId, parentIds[], sourceAnchorRefs[], conceptIds[], modeSpecificPayload, provenance, immutableFieldPaths[], evidenceStatus}
ConditionBundle {designId, rendererId, rendererRevision, textTokens?, visualInputs[], spatialInputs[], modelSpecificFeatures?, mandatoryFieldCoverage, compatibilityReport}
RenderArtifact {artifactId, designId, modelRevision, exactConditionHash, seed, nativeSettings, imageUri, dimensions, runtimeMetrics, parentArtifactIds[]}
ValidationReport {structuralFailures[], visualConcerns[], measuredMetrics, unknowns[], ownerGradeRef?}
Mode-specific payloads are separate schemas, not universal strings; unknown fields cannot be silently filled with fictitious observations. A renderer adapter may use only input channels it actually supports. If a mandatory field cannot be represented, report unsupported capability rather than omit it. No arbitrary common token limit.

## 4. Executable graph and state machine
DRAFT -> READY -> QUEUED -> AUTHORING -> DESIGN_VALIDATION -> FROZEN -> CONDITIONING -> RENDERING -> OUTPUT_VALIDATION -> ENHANCEMENT (if applicable) -> REVIEW_READY.
Route B inserts EDITABLE_DESIGN before explicit authorization to render. Text-only Theme terminates in its own validated text output; a Theme Sheet request is a separate job. Each stage is durably checkpointed and has input/output hashes, timing, memory, failure reason, retry class. Cancellation or interruption preserves authorized job and prior artifacts. Resource scheduler serializes competing heavy workloads and frees unused model memory. Failure never automatically substitutes a different model or mode. Resume only if input/model/graph versions match; otherwise create a new revision. Not all stages apply to all modes: graph defines legal transitions. Artwork alone retains five phases, N candidates each, one selected continuation.

## 5. Mode mapping — first inventory, NOT full methodology translation
Repository source headings and canonical architecture establish the following starter mappings. Each must receive complete clause coverage before implementation acceptance, including exact layout, command equivalents, counts, protections and precedence. Do not assume chat-specific phrasing is removable without review.
- Character: canonical selector -> independent character design and concept fusion -> freeze identities -> required views -> output sheet. Exact owner-approved 1–5 sheets, five independent records per sheet, front/90-degree side/rear. Maintain all original design requirements; compare source versions/owner addendums. Current source characters_creation.md is 713,637 characters; complete formal mapping is not done.
- Creature: creature-body regime, role, silhouette, support/locomotion, anatomy, concept symbiosis, material/color hierarchy, distinct catalogue; not character creation with cosmetic changes.
- Outfit: frozen identity/body, garment design-space search, distinct catalogue, fit, materials/color and selected preview. Identity freeze is explicit, not a similarity-score suggestion.
- Alternate Universe: immutable/adaptable/AU-replaceable anchors, world logic and systemic visual reauthoring, not costume swap.
- Transformation: immutable/flexible/transformable anchors, structural change budget, whole-body propagation and same-identity recognition.
- Mutation: protected/stable/flexible/open anchors, mutation vectors, local/systemic scope, retention and structural diversity; not transformation.
- Feminisation: source recognition anchors, independent character reconstruction, required presentation, source integrity and documented validation.
- Art Style Correction: two-source governance (content vs target style); no content drift or source-style/target-subject leakage.
- Weapon Creation: fictional visual catalogue and source identity/style relationships only; no new functional construction requirements inferred.
- Theme: text-only independent themes, existing theme-specific B/E/T and roulette decisions; atom universe and parameter definitions remain open.
- Sheets: preserve four separate semantics, source pixels and approved presentations. Internal conditioning representation is proposed; one-shot Character Sheet V5 remains binding. Full original sheet methodology documents were not present in the inspected repository asset tree; recover and verify exact sources before claiming full mapping.
No new specialized user controls are approved.

## 6. Model architecture experiments (no candidate selected)
A: shared learned design backbone with compiled specialist entry points and output heads.
B: independently trained mode-specific learned design modules.
C: hybrid shared concept/vision encoder and distinct learned creative modules.
Use matched real-task examples and held-out owner-reviewed cases. Compare end-to-end semantic correctness, diversity, image conditioning success, latency, peak memory, thermal stability, and device background recovery. Training may use compatible pretrained weights, supervised tuning, distillation, architecture changes and compatible merging. Training from scratch is a candidate only if feasible on measured free hardware. Adapters may be an internal training technique; runtime LoRAs optional and not a prerequisite for methodology or sheet use. Three image families benchmark separately; checkpoint compatibility is verified by architecture, tensor layout, encoder and VAE, not name. No fixed fusion ratios or model sizes inferred.

## 7. Data and learning
Training record links typed inputs, authoritative mode version, approved target design/artifact or labeled correction, exact source rights/provenance, model configuration and split group. Group train/test by identity, reference series and near-duplicate provenance to avoid leakage. Owner-reviewed labels separate from teacher/synthetic labels; quality examples not consent for training. Loss candidates: schema/field accuracy, source feature preservation, semantic design alignment, independent diversity, view consistency, image reconstruction/conditioning, preference comparisons. Calibrate per mode; no invented numeric thresholds. Freeze baseline, log experiment reproducibly, evaluate holdouts and compare raw and enhanced output. Small-update boundary and promotion threshold still open; no automatic major weight replacement.

## 8. Acceptance contract
Hard checks: no runtime methodology-document input, no deterministic authored-prose fallback, complete required schema, correct counts, output dimensions/view slots, immutable source/identity fields, valid asset hashes, no omitted required conditioning, no silent fallback or falsely completed job. Unit tests alone cannot establish visual identity, exact views, aesthetics or concept fusion. Physical device tests must cover each real mode, each applicable family and route, background/interrupt/resume, native decode, model loading and handoff. Compare against previous failed outputs and frozen baseline; human-reviewed aesthetic/semantic rubric calibrated with owner. No sphere acceptance. CI, source implementation, device execution and quality acceptance are distinct states. A model candidate cannot be declared selected without measured comparative results.

## 9. Required traceability artifact per mode
For every source clause: source file/hash and section -> approved precedence -> input fields -> operator / learned task -> hard invariant or qualitative target -> output fields -> test case/evidence -> status (mapped, ambiguous, missing source, owner review, implemented, tested, accepted). Preserve conflicts rather than silently reconcile. Implementation cannot claim full methodology compliance until mapping is complete and owner-approved revisions recorded. Start with Character and Theme because real failures were observed, but all modes are within scope. This draft does not change sources, launch training, or authorize deployment.

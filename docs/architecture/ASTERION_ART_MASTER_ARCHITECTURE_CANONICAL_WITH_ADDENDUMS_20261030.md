**ASTERION**

**ART**

**CANONICAL PRODUCT ARCHITECTURE**

**AND TAILORED-MODEL DEVELOPMENT SPECIFICATION**

29 October 2026 • Version 1.0 • Documentation only

**Purpose**

Define the intended Asterion Art application as the owner specified it: a phone-native, multi-mode image and video generator with internal reference-sheet construction, distinct Originals, automatic pre-render compilation, automatic enhancement, human grading, comparative learning and genuinely tailored local models.

| **AUTHORITY** Owner-provided requirements prevail over code, speculative implementations and historical assistant-written summaries. This document does not change the repository or authorize a build. |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

**COMPATIBILITY / STATUS**

Normative product specification. Not a claim that the present APK works. Exact previously approved details that cannot be recovered are identified explicitly, not silently replaced.

# Reading convention and decision authority

Each statement in this document belongs to one of three categories. This separation prevents repository drift and unauthorized reinterpretation.

| **Code**    | **Meaning**                                                                                      | **How it is used**                                                                               |
|-------------|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| OWNER       | Directly defined or corrected by the owner in this conversation or established directions.       | Binding. No assistant or codebase reinterpretation.                                              |
| RECOVERED   | Recorded in older implementation, repository contracts, or prior audit.                          | Historical evidence, not a replacement for the owner-authored original.                          |
| ENGINEERING | A proposed technical boundary or verification mechanism required to implement the owner purpose. | Subject to compatibility with OWNER requirements; not an independently authorized design change. |

| **IMPORTANT** The original names, procedures and detailed acceptance rules of the five Artwork-only critical phases are not available in the supplied conversation. They are intentionally NOT renamed, reconstructed from defective code, or assigned invented semantics. |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

# Executive definition

**Asterion Art is an end-to-end, self-contained Android creative generation system.** Its two primary functional families are still-image creation and video creation. Image generation supports Anime, Cosplay and Realistic modes. Still-image creation contains Artwork Creation, internal near-deterministic Sheet Creation, and distinct Originals methodologies. After user validation, one Start action launches the selected job in the background, carrying it through genuine local generation, applicable enhancement, storage, review and user grading. Optional prompt and parameter editing remains available at every appropriate pre-start point. The product must be capable of learning comparatively from explicit owner evaluations, supported by a model family actually adapted to its own taxonomy, generation tasks and workload.

## Document map

| **Part**   | **Subject**                                                                       |
|------------|-----------------------------------------------------------------------------------|
| 01–03      | Product mission, non-negotiable rules, system decomposition                       |
| 04–06      | Image modes, sheet creation, Artwork Creation and Originals                       |
| 07–09      | Pre-render compiler, inference, background automation and enhancement             |
| 10–12      | Video creation, library, grading and comparative learning                         |
| 13–17      | Tailored model family, model fusion, real context extension, training, evaluation |
| 18–21      | Data, reliability, integration boundaries, acceptance and restoration rules       |
| Appendices | Canonical traceability, unresolved original detail, GitHub source references      |

# 01 Product mission and exclusions

**Mission.** Generate complete, detailed, high-quality artwork and video from the owner’s references, canonical sheets, concepts and selected methodology; automate otherwise repetitive work without depriving the owner of final control.

- **The app is a generator**, not primarily a prompt-writing app, reasoning demonstrator, architecture research shell or collection of disconnected utilities.

- **The app is strictly unrestricted by design.** Do not impose invented creative categories, arbitrary application-level suppression, reinterpret requested outputs, or disable a requested workflow to suit a convenient model. Actual hardware/model incompatibilities must be reported plainly rather than disguised as user restrictions.

- **The owner controls starts and approvals.** Once the relevant inputs have been validated and the owner presses Start, no routine additional confirmation screens should block autonomous completion.

- **The app must work on the phone.** Its primary supported functions may require user-installed local models; they must not secretly depend on a PC or a separately running third-party companion app.

- **Local after model acquisition.** Network connectivity is permitted where the owner directs model search, import or download; local generation, review and learning must not require an external inference service.

- **Original methodology stays original.** Neither technical convenience nor existing repository code can supersede a methodology specification, its output contract or the owner’s approved references.

# 02 Non-negotiable invariants

| **ID** | **Requirement**                                                | **Verification property**                                        |
|--------|----------------------------------------------------------------|------------------------------------------------------------------|
| INV-01 | Still-image modes: Anime, Cosplay, Realistic.                  | Selectable and runnable, each producing an actual decoded image. |
| INV-02 | Image family: Artwork, Sheet Creation, Originals.              | Each is independently accessible without collapsing its purpose. |
| INV-03 | Artwork uses 3 mandatory sheets + optional Element.            | Character + Theme + Art Style required; Element optional.        |
| INV-04 | Sheet Creation reconstructs from reference(s), ordinarily one. | Never relabel copied crops as generated missing views.           |
| INV-05 | Five critical Artwork phases belong ONLY to Artwork.           | No shared five-phase injection into Originals, Sheets, Video.    |
| INV-06 | Pre-render layers produce coherent final image-model input.    | No fragmenting one specification into diffusion repaints.        |
| INV-07 | One Start after validation runs unattended.                    | Prompt/parameter edit is optional, not a recurring blocker.      |
| INV-08 | Image Toolbox, RealSR and SuperImage are integrated.           | Automatic enhancement before user review.                        |
| INV-09 | Owner grades actual outputs and comparisons.                   | Only explicit owner grades authorize supervised learning.        |
| INV-10 | Video produces requested 10–30-second result.                  | Single image or true frame-referenced workflow.                  |
| INV-11 | Taxonomy, models and context are tailored, not pretended.      | Measured native text capacity and validated adapted checkpoints. |
| INV-12 | Preserve installed model data, plans, assets and signing.      | Safe update, no reset, no incompatible signing deployment.       |

# 03 Logical system architecture

This is a **contractual component decomposition**, not permission to replace existing implementations. Recover and reuse sound historical components where they implement these contracts.

| **Layer**                                | **Sole responsibility / public contract**                                                                                                                                        |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Interaction / Project UI                 | Modes, references, project history, validation, single Start, optional advanced controls, progress, review.                                                                      |
| Asterion Core integration                | Canonical identities, sheets, source references, metadata and secure import/export. Core remains authoritative when used.                                                        |
| References + Sheet Studio                | Import/interpret original pixels; construct four near-deterministic reference-master sheet types.                                                                                |
| Methodology registry                     | Load precise owner-defined Originals independently; preserve versioned output requirements.                                                                                      |
| Original Compiler / Specification engine | Validated typed visual specifications, ordered evidence, constraint reconciliation and deterministic compilation. Historical Compiler functionality is recovered, not discarded. |
| Pre-render intelligence                  | Layered visual, semantic and design synthesis; output a checked, single coherent final model-facing specification.                                                               |
| Artwork five-phase engine                | Execute the owner’s exact five Artwork-only quality phases. Its scope must never leak to other workflows.                                                                        |
| Inference routing / native runtimes      | Mode-to-model routing, verified native text tokenization, reference conditioning, diffusion/video execution and decoding.                                                        |
| Background orchestrator                  | Durable task states, checkpoints, sequencing, cancellation, recovery, resource isolation, notifications.                                                                         |
| Enhancement coordinator                  | Automatic integrated Image Toolbox, RealSR, SuperImage stages; protect source and provenance.                                                                                    |
| Library, Grading, Learning               | Store originals and enhanced assets; detailed owner review; labeled comparisons; validated model adaptation.                                                                     |
| Model development / registry             | Import, provenance, tokenizer and compatibility metadata, fusion, training, evaluation, QNN packaging and release control.                                                       |

| **SEPARATION RULE** Reasoning may produce a high-quality prompt, but it is not diffusion. A saved score may train a ranking head, but it is not proof of model improvement. Model downloading is not model fusion. A successful CI APK is not a successful render. |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

# 04 Still-image creation: mode and task model

**Render mode** (Anime / Cosplay / Realistic) describes the intended visual synthesis and eligible trained model route. **Creation task** (Artwork / Sheet / chosen Original) defines source authority, methodology, output geometry, and downstream controls. These are separate dimensions; selecting a render mode must not erase the task-specific contract.

| **Task**         | **Inputs**                                                                                        | **Generated artifact**                                                               |
|------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| Artwork Creation | Character Sheet + Theme Sheet + Art Style Sheet; optional Element Sheet; optional user direction. | One or more finished illustrations that faithfully reconcile all chosen sheets.      |
| Sheet Creation   | One primary reference normally; optional additional evidence; selected sheet role.                | New role-specific analytical reference master; missing views genuinely inferred.     |
| Originals        | The chosen fixed methodology’s concepts, reference(s), canonical identity and settings.           | Exactly its defined output type, count, layout, identity and structural constraints. |

The same user’s source artwork may support image editing, variation or Original workflows, but it must not be silently converted into a different task. Any default selection of model, render dimensions, scheduler or enhancement recipe must be transparent and overridable where appropriate.

# 05 Near-deterministic Sheet Creation

**Purpose:** create reliable, structured references for later artwork and Originals. Sheet Creation is not a requirement for the user to manually provide multiple views or pre-cropped source panels. One original image is the normal input.

| **Sheet**       | **Canonical content and responsibility**                                                                                                                      |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Character Sheet | Preserve the character’s true identity, anatomy, outfit/physical traits and consistent alternative perspectives or analytic details.                          |
| Theme Sheet     | Extract scene/world logic, visual theme, spatial, compositional, mood and related reference information.                                                      |
| Art Style Sheet | Analyze and reconstruct style grammar: drawing approach, line, rendering, palette, shading, material treatment; do not transfer depicted identities as style. |
| Element Sheet   | Isolate useful specified elements, morphology, materials, application and variation without inventing irrelevant elements.                                    |

- **Input:** owner-picked image(s), selected role and optionally Asterion Core character/source authority; import must preserve original pixels and URI ownership.

- **Interpretation:** classify source identity, geometry and visual facts; make missing/uncertain aspects explicit; do not hallucinate absent canonical attributes as if observed.

- **Construction:** generate new missing angles/details from the actual reference, apply role-specific analytic layout and accepted visual presentation; do not fill panels by repeatedly cropping the supplied picture.

- **Near-deterministic contract:** fixed required panels, labels and structural fields, reproducible inputs/settings and provenance. Where generative reconstruction is stochastic, the app must verify the layout and identity rather than claim mathematical determinism.

- **Acceptance:** new analytical content is genuinely created, source consistency and intended information role verified, no blank/duplicated panels and no falsely completed board.

- **Preserve references:** original and intermediate images stay intact; document a generated master separately, with explicit differences and model details.

The previously approved clean sheet-presentation references (e.g. Francesca-style analytical hierarchy) remain reference authorities, not permission to replace the owner’s exact sheet designs. The four types share presentation quality while retaining different semantics.

# 06 Artwork Creation — unique five-phase quality protocol

Artwork Creation begins with Character, Theme and Art Style sheets and optionally an Element sheet. It must fuse their distinct authorities into a coherent finished illustration; it is not a five-design reference catalogue.

**Important boundary:** the owner has explicitly defined **five critical quality phases for Artwork Creation and only Artwork Creation**. Their exact original per-phase texts were not present in the accessible conversation or retrieved source. They must be recovered verbatim from the approved source and treated as immutable stage definitions. The present repository’s five generic diffusion refinements are **not** authoritative substitutes.

| **Canonical slot** | **Binding implementation treatment**                                                          | **Prohibited substitution**                             |
|--------------------|-----------------------------------------------------------------------------------------------|---------------------------------------------------------|
| Artwork phase 1    | Execute original owner-defined phase 1; preserve its exact input, output and transition gate. | Generic “first pass” invented from planner code.        |
| Artwork phase 2    | Execute original owner-defined phase 2 within Artwork only.                                   | Global diffusion stage applied to Originals.            |
| Artwork phase 3    | Execute original owner-defined phase 3 within Artwork only.                                   | A text chunk of an overflowing prompt.                  |
| Artwork phase 4    | Execute original owner-defined phase 4 within Artwork only.                                   | Unapproved partial repaint or identity drift.           |
| Artwork phase 5    | Execute original owner-defined phase 5 within Artwork only.                                   | A generic cleanup phase substituted for owner protocol. |

The image-model instruction remains a **complete unified specification at every inference invocation requiring it**. Five owner-defined quality phases do not license dividing an overlong canonical prompt into unrelated context windows. If the actual approved protocol includes several image operations, each operation must meet its own complete specification and continuity contract; their exact count and semantics come from owner records, not this document.

# 07 Originals — independent methodologies

An Original is a **native default application mode implementing a unique methodology**, not a drop-down prompt preset, not an Artwork derivative, and not a recipient of Artwork’s five-phase process. Each Original owns its validation, source policy, authored reasoning, generated output count/geometry and selection/refinement flow.

| **Original**         | **Recovered output contract to reconcile with owner original**                                                                                                                                                                                              |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Character Creation   | Phase 1: 1–5 sheets; 5 independent frozen original identities per 16:9 sheet; each identity true front, exactly 90° profile and direct rear, with consistent anatomical construction and concept fusion. No other Original’s outfit/weapon phases imported. |
| Feminisation         | One recognizably related, independently reconstructed female design with its documented exact output framing and identity-conservation rules.                                                                                                               |
| Alternate Universe   | Same source identity reinterpreted into the required alternate-universe variants, not superficial costume swaps.                                                                                                                                            |
| Transformation       | Original identity preserved through its approved coherent alternative forms; exact count and board structure from methodology.                                                                                                                              |
| Mutation             | Structural deviations under the approved source-preservation rule; not merely visual recoloring.                                                                                                                                                            |
| Creature Creation    | Distinct, coherent bodies, locomotive/sensory structures, concept-material fusion and specified output composition.                                                                                                                                         |
| Outfit Creation      | Independently constructed garments on preserved character identity/body, required catalogue and selected design preview.                                                                                                                                    |
| Weapon Creation      | Distinct complete weapons with approved isolated layout, character-specific geometry and selection preview.                                                                                                                                                 |
| Art Style Correction | Source illustration is content authority, target Style sheet is style authority; re-render style without replacing subject, scene, pose or framing.                                                                                                         |

| **EVIDENCE STATUS** This table identifies the nine modes present in the repository and the recoverable broad contracts. It is not a reconstruction of the missing full methodology documents. Their exact user-authored text, output grids, role-specific rules and reference images must be preserved as authority when recovered. |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

- **No generic fallback:** a failed specialist methodology must not silently become ordinary Artwork or a generic “five designs” prompt.

- **Each Original owns its own output schema:** count, layout, view constraints, frozen attributes, permitted mutation, use of reference pixels, and preview/edit relations.

- **Validation precedes Start:** missing sources or impossible model capabilities must be reported before consuming long inference time.

- **Independent tests:** passing Character Creation does not establish that Feminisation, Alternate Universe, Transformation or other Originals work.

# 08 Layered pre-render intelligence and compilation

**Purpose:** do the difficult reasoning before rendering, then deliver the appropriate native inference engine a single coherent, sufficiently detailed model-facing specification. Layered planning and the five Artwork-only quality phases are separate concepts.

| **Contracted stage**            | **Required input and output**                                                                                                                           |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 / Ingest and normalize        | Resolve selected mode, task, sheets, actual images, optional owner instructions and settings into typed, traceable input.                               |
| 2 / Load binding authority      | Read exact methodology, canonical taxonomy, frozen character identities and source evidence without silently losing fields.                             |
| 3 / Interpret visual references | Extract visually grounded evidence from actual pixels; mark unknowns and conflicts for resolution.                                                      |
| 4 / Synthesize design           | Resolve physical interactions, composition, materials, aesthetic constraints and source relationships for the chosen task.                              |
| 5 / Compile final specification | Merge mandatory attributes, explicit exclusions, geometry, quality targets and owner overrides into a single checked model-facing prompt/specification. |
| 6 / Verify before inference     | Measure real selected-native-tokenizer capacity; check references and model conditioning; either proceed faithfully or fail transparently.              |

These **six architectural responsibilities** describe the required pre-render compiler and are \*not\* an alternative definition of the five owner-approved Artwork phases.

- **One source of truth:** preserve ordered references to canonical assets and the exact user methodology; owner edits form a versioned overlay instead of rewriting canonical data.

- **No speculative infinite reasoning:** a deterministic compiler performs operations that do not need model inference; call the LLM only for genuinely semantic work. Deduplicate evidence, reuse valid cached analyses and checkpoint completed results.

- **Bounded latency:** measure and surface time spent loading the model, interpreting references, synthesizing, tokenizing and generating. Prevent uncontrolled nested Qwen calls, repeated full restarts or endless field-repair loops.

- **Complete output schema:** a broken/missing field does not become a fake placeholder; repair only demonstrably missing fields where possible and preserve already validated material.

- **Native context is real:** the 32K Qwen reasoning context is distinct from the image generator’s CLIP/T5/Qwen diffusion-text encoder. A long planning prompt cannot be declared supported merely because the reasoner accepts it.

- **If too long:** use an actually compatible, validated longer-context image model or compile compact semantics that preserve every mandatory requirement; never split one prompt into multiple denoising calls as an ersatz context extension.

# 09 Native still-image inference

Rendering is performed by the actual locally installed and verified still-image backend. The inference engine must support the selected render style, source/reference images, output geometry, scheduler, diffusion architecture and final specification. Backend adapters implement capabilities; they do not redefine product modes.

| **Engine capability**  | **Required invariant**                                                                                                                                |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| Model recognition      | Identify exact architecture, checkpoint identity, tokenizer, VAE, control formats, quantization, supported image sizes and native platform.           |
| Reference conditioning | Use the actual Character/Theme/Art Style/Element input pixels as supported by the model; do not drop them and pretend prose is equivalent.            |
| Full prompt acceptance | Count using the actual native image tokenizer, with truthful limits and measured overflow. No silent truncation or sequential partial-prompt repaint. |
| Output decoding        | Check decoded PNG/video dimensions, non-corruption, expected geometry, sufficient nonblank detail and faithful result provenance.                     |
| Performance            | Benchmark loading, prefill, diffusion steps, VAE, memory pressure and thermal throttling separately.                                                  |
| Failure behavior       | A blurred field, missing subject, incorrect geometry, decode error or model-incompatible request is a failed candidate, not success.                  |

The five blurry outputs supplied by the owner establish a genuine user-visible image failure. They do not independently identify the exact faulty native component. Diagnostics must distinguish wrong checkpoint/routing, conditioning, schedule/denoise, tensor shape, VAE decode, low-memory state and upstream reference quality.

# 10 One-click background execution

The default UX is **select → validate → Start once → autonomous completion**. The user may inspect or edit prompts, references and parameters before Start, but these are optional advanced actions. Routine prompt-approval dialogs should not be mandatory once the job has been validated.

| **Job state**               | **Meaning / durable behavior**                                                                                  |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------|
| DRAFT                       | Owner composes sources and mode; no expensive inference starts.                                                 |
| VALIDATED                   | All required sources/methodology/mode/model capabilities pass validation; optional adjustments remain possible. |
| STARTED / PLANNING          | A durable background job begins. Distinct reasoning stages, elapsed times and actual work are logged.           |
| GENERATING                  | Native inference runs with recorded full conditioning; check-pointed outputs are persisted as soon as useful.   |
| ENHANCING                   | Eligible generated originals run through the integrated postprocessing chain; source versions remain untouched. |
| REVIEW_READY                | Only the verified generated and enhanced results are presented for detailed human review.                       |
| GRADED                      | Explicit owner grades and corrections are stored with image pixels and exact lineage.                           |
| LEARNING_ELIGIBLE           | Owner-approved examples may be assembled for validated training and comparative updates.                        |
| FAILED / PAUSED / CANCELLED | Accurate cause and stage, model/resources preserved, safe recovery or explicit retry; never claim completion.   |

- **Durability:** atomic state transitions, idempotent job IDs, asset snapshots, durable checkpoints and restart recovery after Android process termination.

- **Resource control:** Qwen reasoning, native image diffusion, Wan video and large upscalers must not compete for unavailable GPU/NPU/RAM capacity; release resources between stages.

- **Progress:** show model load, reasoning, actual image/video generation, enhancement and output publication separately; never confuse Qwen pre-render delay with number of diffusion steps.

- **Background constraints:** the app should use appropriate supported Android execution services and restore resumable work when interrupted; failures or OS limits must be surfaced honestly.

- **No PC dependency:** tasks, temporary files, model access, callbacks and progress are managed by the Android app itself after the owner starts the job.

# 11 Automatic enhancement before review

Every eligible image result automatically enters an **integrated post-generation enhancement workflow** using the intended Image Toolbox, RealSR and SuperImage capabilities before the owner receives the review-ready output. The exact chaining order and per-mode treatment are configuration/protocol questions to derive from approved originals; no arbitrary new order is declared here.

| **Capability**     | **Required behavior**                                                                                                                                                               |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Image Toolbox      | Local image adjustments, restoration or corrections as selected by the approved enhancement recipe; it is part of the automatic pipeline, not a manual external-app sharing detour. |
| RealSR             | Actual neural upscaling/detail restoration; verify genuine installed runtime/model, output resolution, model-specific suitability and artifacts.                                    |
| SuperImage         | Integrate the intended SuperImage enhancement capability inside the Asterion workflow, rather than relying on the user to manually export/import each creation.                     |
| Coordinator        | Compute compatible order, keep the original, avoid repeated destructive processing, persist stage outputs, resume safely and log each result.                                       |
| Review publication | Present raw vs enhanced versions when useful, with exact enhancement provenance; the postprocessed version is the default review target.                                            |

Neither an absent upscaler nor an enhancement exception may silently convert an unfinished pipeline into a completely successful enhanced result. The owner may decide whether an available raw rendering should still be shown as a clearly marked incomplete output.

# 12 Review, grading and comparative self-learning

The user’s explicit review is the authoritative feedback signal. **Automatic vision observations and technical image metrics are diagnostics, not owner decisions.** Self-learning must compare failed and successful reviewed outputs and improve the future generation process across the relevant task dimensions.

| **Feedback dimension**      | **What is learned or evaluated**                                                                                 |
|-----------------------------|------------------------------------------------------------------------------------------------------------------|
| Visual quality              | Sharpness, coherent detail, color, finish, artifacts; not merely image variance or edge contrast.                |
| Identity/reference fidelity | Character continuity, outfit/source features, style, theme, element integration.                                 |
| Anatomy/structure           | Body logic, silhouettes, appendage roots, concept fusion, proportion, camera/views.                              |
| Methodology correctness     | Correct output type, count, views, format, differentiation and required preserved anchors.                       |
| Generation configuration    | Relative effects of checkpoints, adapters, seed, sampler, CFG, steps, dimensions and denoise.                    |
| Pre-render intelligence     | Which source interpretations, prompt structures and semantic decisions lead to better or worse reviewed results. |
| Video quality               | Temporal continuity, pose motion, frame reference adherence, artifacting, transition seams.                      |
| Enhancement quality         | Measured improvement or damage introduced by specific enhancement tools/recipes.                                 |

- **Review interface:** optional detailed grades by image and region/feature; explicit Preserve and Correct entries, and comparisons between versions or previously reviewed successes/failures.

- **Traceable examples:** raw input, references, canonical methodology version, final prompt, model+adapter, seed and settings, raw/enhanced output, grade, correction and time are linked.

- **Training signal integrity:** only saved owner grades become labeled data. Missing grades and machine-proposed grades are not silently treated as approval.

- **Learning stages:** collect -\> group comparable cases -\> isolate causal variables -\> adapt prompt/compiler/model routing/parameters -\> train adapters or model candidates -\> evaluate held-out examples -\> propose adoption only with measurable benefit.

- **Generalization boundaries:** do not use a correction about a particular outfit or Original to overwrite unrelated canonical taxonomy; scope recommendations by task, mode, character, feature and reference family.

- **Not enough to store numbers:** an RGB/edge regression head can suggest technical trends but does not by itself establish anatomy, style, concept comprehension or improved base model weights.

- **Reversibility:** keep baseline models and previous adapters; every learned variant needs model identity, provenance, validation and rollback.

# 13 Tailored model ecosystem: responsibility model

Asterion Art requires a **cohesive app-specific family of models**, not necessarily one monolithic checkpoint. Tailoring is a production requirement: model capability must improve the actual methods, precision, conditioning, output fidelity, performance and ability to learn. The system must never be simplified merely to fit the weakest readily available encoder.

| **Model family**                                        | **Work product / tuning target**                                                                                                                              |
|---------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Reasoning / planning (Qwen3 family)                     | Owner methodology comprehension, physical concept integration, prompt compilation, schemas, low-latency token-budgeted inference and stable context handling. |
| Vision / reference understanding (e.g. Qwen3-VL family) | Actual pixel evidence from original sources and generated outputs; identity/style/anatomy observations, uncertainty and graded-region support.                |
| Still-image generative models                           | Actual high-quality Anime, Cosplay, Realistic rendering with complete prompt-conditioning, real reference conditioning and required resolutions.              |
| Specialist image adapters                               | Mode-specific and methodology-specific visual competencies, learned only where model capability and output evaluation support them.                           |
| Video generative model                                  | Frame-aware 10–30s generation, temporal consistency, locally decoded video and coherent motion semantics.                                                     |
| Enhancement models                                      | Resolution recovery and fine-detail enhancement with known fidelity/quality tradeoffs.                                                                        |
| Ranking / preference and training controller            | Comparison-aware feedback, evaluation, checkpoint/model/adaptor selection and rollback.                                                                       |

The owner imports/selects the desired model files from inside the application, including compatible Hugging Face repositories. Already installed QNN archives, local GGUF files, generated media, selections and LoRAs are persistent assets, not disposable cache.

# 14 Base-model sourcing, fusion and alteration

**Goal:** build or adapt checkpoints whose actual behavior fits Asterion’s task families, benchmarked on original owners’ references and prompts. “Fusion” is an experimental model-development operation, not a guarantee of improvement or an excuse to replace existing selected models.

- **Inventory existing assets.** Identify every installed model by SHA-256, architecture, variant, tokenizer, encoder, VAE, quantization, supported resolution, expected memory and source license/provenance. Never overwrite an existing original checkpoint.

- **Separate compatible families.** Direct weight blending requires compatible tensor architectures, parameter names, shapes, latent and text-conditioning conventions. Anima-compatible merges and SDXL/Illustrious models are not arbitrarily interchangeable.

- **Recover the existing Fusion B research.** The repository describes a candidate Rin Flanime Anima 70% + MiaoMiao Anima 30%, not a user-approved final model. Treat the ratio, source pins and architecture gates as historical research evidence requiring real render comparisons.

- **Evaluate all intended mode families.** The model plan must cover Anime, Cosplay, Realistic and video separately; a good Anime model is not automatically a Realistic or video backbone.

- **Alter where needed.** Adapter training, fine-tuning, reference-conditioning upgrades, encoder-context changes, architecture-compatible merges and quantization-aware conversion may be appropriate, but only when measurable and technically validated.

- **Preserve base lineages.** The derived checkpoint is versioned, signed/hashed and independent of base weights so regression, rollback and future comparisons remain possible.

- **Phone deployment.** Run actual models through a compatible Android native runtime; conversion must preserve supported graph structures, encoder, VAE and quantization calibration. CI tensor tests are not equivalent to successful QNN inference.

- **No silent model switch.** Automatically selected models must follow explicit owner configuration and measured capabilities; fallback must never degrade the task to a different purpose without a visible failure state.

# 15 Real extended tokenization and context inference

**Two different contexts must be engineered and verified:** (A) the reasoning model’s context used for layered planning and (B) the image/video model’s actual conditioning context. Extending one does **not** extend the other.

| **Subsystem**                | **Proper technical work**                                                                                                                                                                                                  | **False shortcut explicitly rejected**                                           |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Qwen3 pre-render reasoning   | Confirm tokenizer, actual context-window metadata, positional/rope behavior, KV-cache memory, output schemas, caching and throughput. Train/validate any longer-context adaptation instead of changing a numeric constant. | A long token setting without model memory/performance validation.                |
| Image model prompt encoder   | Adapt compatible text encoder and model cross-attention/conditioning architecture, positional rules and trained weights where required. Validate new limits with the real native tokenizer and on-device graph.            | Split one prompt into successive diffusion calls and call that extended context. |
| Reference-image conditioning | Increase or optimize supported image reference slots/embeddings, visual-token budget and cross-attention capability; measure identity fidelity and signal preservation.                                                    | Omit some references or replace source pixels with unverified prose.             |
| Video temporal context       | Model truly ordered frame anchors, motion state, temporal dependencies, long-video memory and conditioning constraints.                                                                                                    | Join unrelated short clips and claim true multi-keyframe inference.              |

**Acceptance for any “extended context” claim:** real model artifact/encoder modification where needed; measured native max token count; full-specification inference without truncation; no attention-boundary errors; successful render with mandatory detail retention; comparable or improved inference performance on the target phone; repeatable QA results.

# 16 Training pipeline and learning deployment

The final product should close the loop from reviewed artifacts to useful, validated model behavior without treating a local score formula as a trained multimodal intelligence.

1.  **Corpus formation.** Save fully traceable human-reviewed pairs: failed/successful outputs, actual image crops, source context, methodology, prompt, model and settings. Group by consented supervised labels and mode.

2.  **Privacy and fidelity.** Keep the corpus private on device unless the owner explicitly chooses export/training elsewhere. Maintain hashes, attribution, signed versions and deletion/update synchronization.

3.  **Error taxonomy.** Classify observed failures by anatomy, identity, style, composition, reference fidelity, temporal continuity, enhancement and model/encoder faults. Distinguish user labels from machine observations.

4.  **Train candidate specializations.** Start with repeatable prompt/planner improvements, then train appropriate text LoRA, vision models/adapters, diffusion adapters or video adapters where actual training tooling and data make them technically possible.

5.  **Evaluate independent evidence.** Compare against frozen baseline using held-out original owner-reviewed examples. Evaluate both effectiveness and regression across all modes and Originals, not solely mean technical score.

6.  **Promote only verified gains.** Activate a candidate when its measured benefits exceed its regressions and owner-approved acceptance. Keep exact rollback and model-selection provenance.

7.  **Continual operation.** Re-run comparative evaluations when new grades arrive; prevent catastrophic forgetting, false positive self-grading and unstable automatic weight overwrites.

The existing text-LoRA training script in the research repository is only a starting component: textual corrections alone do not train pixel interpretation, image diffusion, or video synthesis. Every model family must have its own evidence, training process and validation record.

# 17 Model performance and memory architecture

The target Android device must support useful local generation rather than spending its resources in uncontrolled planning. The workload manager measures separately: model load and verification, token prefilling, token decode, diffusion steps, VAE/image decode, enhancement, video frame inference, MP4 encoding, thermals and peak memory.

- **Plan once, reuse safely:** cache exact reference analyses and canonical interpretations by content hash and model/methodology version; no stale cross-identity leakage.

- **Fast startup when ready:** reuse already valid local model registrations and avoid heavyweight work until Start.

- **Avoid redundant Qwen passes:** deterministic stages compile mechanically; semantic Qwen calls are batched or selectively invoked; detect model output incompleteness without restarting a full 25-minute decode from zero.

- **Stage resources:** release Qwen memory before large QNN diffusion where necessary; keep only required references and completed checkpoints in RAM; do not delete model files.

- **Match turbo/scheduler to model:** steps, CFG, denoise, batch, aspect ratios and control channels derive from actual checkpoint capability rather than generic constants.

- **Performance evidence:** compare time to first valid preview, time to a finished image, correctness and device thermals against a reproducible baseline. The owner’s two-hour unfinished pre-render is an unacceptable observed symptom, not an intended performance class.

- **No fictional guarantees:** do not promise a specific shortened generation time or larger encoder capacity until verified on device.

# 18 Video Creation

**Purpose:** produce a finished **10–30-second video** from either one image (automatic prompt generation) or user-selected frame references. Video is a first-class creation family, not merely an MP4 file join utility.

| **Workflow**           | **Required behavior**                                                                                                                                                                 |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Single-image video     | User selects the image, optional motion direction and parameters; pre-render intelligence generates appropriate motion specification; one Start produces 10–30 seconds.               |
| Frame-referenced video | Owner orders the reference frame(s) and any intended temporal anchors; inference must honor those actual pixels and sequence constraints.                                             |
| Temporal planning      | Represent identity, pose, camera, motion, scene evolution and frame-anchor constraints explicitly; do not replace true references with generic textual approximations.                |
| Long-form execution    | If native implementation uses internal segments, continuity, frame/identity agreement, start/end constraints and stitched A/V correctness are part of the actual generation contract. |
| Output quality         | Decodable MP4, correct duration, plausible coherent progression, defined frame rate and preservation of source identity and intended scene.                                           |
| Autonomy               | After validation and one Start, generation, intermediate persistence, video assembly and publication occur in the background without repeated dialogs.                                |

The audited branch has an in-app Wan integration and 1–5-second segment machinery, but this is a partial implementation detail and cannot redefine the owner’s 10–30-second result or pretend exact multi-keyframe conditioning has been validated.

# 19 Persistence, provenance and safe upgrades

Persistence is a production feature. Files downloaded or created on the phone cannot be lost because of architectural changes, signing mistakes, APK replacement or automated cleanup.

| **Protected object**     | **Minimum persisted fields**                                                                                                        |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Project / job            | Stable ID, workflow type, mode, owner validation, task state, timestamps and resume tokens.                                         |
| Canonical source         | Original URI/copy, content hash, Core association, interpretation evidence and source authority.                                    |
| Sheet                    | Role, source references, exact protocol/version, generated panels, full artifact, fidelity checks and provenance.                   |
| Original methodology     | Exact version/hash of full methodology, chosen arguments, output schema, frozen references and approved exceptions.                 |
| Compiled prompt          | All binding authority refs, final unified prompt, negative constraints, tokenizer proof, owner overrides and pre-render timings.    |
| Inference                | Checkpoint and adapter hashes, native runtime profile, seed, steps, CFG, scheduler, denoise, canvas and conditioning references.    |
| Media / enhancement      | Raw artifact, stage outputs, final artifact, model versions, fingerprints, scores and failure records.                              |
| Human grading / learning | Precise owner-assigned grade/corrections, reviewed region pixels, comparative relationship, training eligibility and model lineage. |

**Strict preservation:** do not run \`adb uninstall\`, \`pm clear\`, delete \`files/asterion-qnn-models\`, overwrite downloaded weights or change the existing app’s signing identity. Preserve private user plans, image assets and source references. Any APK update must be tested against the installed certificate and must not require an unauthorized destructive migration.

# 20 Implementation boundaries and repository restoration rules

This section documents how future authorized engineering should be evaluated. **No change is authorized by creating this document.**

- **Preserve historical Compiler implementations.** Compare \`asterion-art-unified-20261023\` and the legacy branch for deterministic compilation, visual specification assembly, source parsing, validation and prompt refinement. Recover behavior rather than discarding it again.

- **Retain functional Art systems.** Keep verified assets, model store, Core bridge, UI, checkpointing and workflows when they satisfy owner requirements; do not blindly revert the whole application.

- **Methodology archive is authority.** Recover owner-authored method documents and reference images from available archives. Source comments and \`DetailedMethodologies.kt\` are pointers, not a superior rewritten canon.

- **Split Artwork-only logic from Originals.** Do not let five-phase orchestration, subject layouts or selected-model fallback cross task boundaries unless explicitly part of that original methodology.

- **Restore true model capability.** Remove fake context-extension claims and measure/tokenize before inference; no unverified “fix” should merely block all generation forever.

- **Enforce integration tests.** Verify an actual on-device raw generation, followed by automatic enhancement and owner review, before declaring working software.

- **Versioned, reversible updates only.** Recovered source, database migrations, trained adapters and signed APKs need rollback points and real compatibility tests.

- **No unilateral execution.** Separate diagnosis, proposal, owner authorization, implementation and validation. The existence of this architecture document is not consent to alter GitHub or the device.

# 21 Acceptance and release criteria

| **Gate**              | **Must be demonstrated before “working” is claimed**                                                                                                          |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A / Android integrity | App opens; navigates all modes; local models remain installed; database/assets survive restart and safe signed update.                                        |
| B / Image basics      | Anime, Cosplay and Realistic each generate a detailed, valid image using native actual chosen model.                                                          |
| C / Sheets            | All four sheet roles generate reconstructed, inspectable reference masters from the intended small reference set, normally one source.                        |
| D / Artwork           | All three mandatory sheets + optional Element influence the finished illustration; owner’s actual five phases are honored only here; unified prompt verified. |
| E / Originals         | Every registered Original produces its exact methodology-defined count, framing, variation and identity behavior. Real generated samples inspected.           |
| F / Automation        | One Start from validated inputs runs through generation, enhancement, save and review-ready in background with recoverable interrupts.                        |
| G / Enhancement       | Image Toolbox + RealSR + SuperImage automatic orchestration actually processes real images without losing originals.                                          |
| H / Learning          | Repeated reviewed failures/successes produce measurable held-out improvement and no degradation of unrelated methods.                                         |
| I / Models            | Adapted/fused checkpoints exist as real trained artifacts; actual encoder limits and native compatibility measured, not inferred.                             |
| J / Video             | Real playable 10-, 20- and 30-second samples from single-image and frame-referenced paths; continuity and keyframes evaluated.                                |
| K / Regression        | The owner-provided five blurred image failures do not recur under a controlled matching test case; detect such results as failures.                           |

“Source implemented” means code exists. “CI passed” means packaging/tests passed. “Device verified” means the specified operation produced an actual artifact on the target device. “Owner accepted” means the result matches the intended aesthetic/methodology. These statuses must never be merged into a single misleading completion label.

# Appendix A Requirements traceability register

| **Origin**           | **Explicit requirement / preserved correction**                                                            |
|----------------------|------------------------------------------------------------------------------------------------------------|
| OWNER / image        | Artwork is ordinary illustration generation from Character, Theme and Art Style sheets; Element optional.  |
| OWNER / sheets       | All sheets made in-app through near-deterministic reconstruction from user references.                     |
| OWNER / render       | Three modes: Anime, Cosplay, Realistic.                                                                    |
| OWNER / Originals    | Dedicated default methodologies with their own fixed, separate outputs.                                    |
| OWNER / phases       | Exactly five critical quality phases for Artwork Creation alone; not a universal pipeline.                 |
| OWNER / intelligence | Layered pre-render work yields a coherent, detailed final model-facing prompt.                             |
| OWNER / automation   | After validation, simple Start triggers automatic background processing; optional manual prompts/settings. |
| OWNER / enhancement  | Automatically apply Image Toolbox, RealSR and SuperImage before owner review.                              |
| OWNER / learning     | Use owner reviews to compare success and failure across all relevant aspects; tailor the models.           |
| OWNER / video        | 10–30s, one-image-to-video or frame-referenced inference, automatic motion prompt.                         |
| OWNER / content      | Strictly unrestricted app-level creative purpose.                                                          |
| OWNER / preservation | No destructive app updates, unauthorized source edits, signature breakage, lost models or plans.           |

# Appendix B Original details not safe to fabricate

The following must be retrieved from the approved originals, not guessed from the damaged current code. Their absence does **not** authorize a replacement:

- **Artwork five-phase content:** exact original stage names, stage inputs/outputs, rendering vs planning boundaries, transitions and owner quality checks.

- **Every Original’s complete methodology:** full specifications, reference art, exact layout/variation specifications and version history beyond repository summary strings.

- **Master Sheet reference contract:** owner-approved source examples and canonical visual panel/content requirements for the four roles.

- **Model target specifications:** owner-approved final fusion/backbone selection, desired true image encoder context, reference capacity and performance targets.

- **Enhancement recipe parameters:** the exact approved chain order, mode-dependent activation and SuperImage/RealSR/Image Toolbox behaviors where the original specification fixes these.

| **DO NOT INFER** A missing original definition is a documentation gap to recover. It is not a request for the assistant to invent a new methodology or modify the repository. |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

# Appendix C Repository and audit reference points

**Current source snapshot:** https://github.com/Azrazieliz/AsterionArt/tree/977400c46dab56cb3b431b661867c12f1d562990

**Original unified Compiler tree:** https://github.com/Azrazieliz/AsterionArt/tree/asterion-art-unified-20261023/app/src/main/java/com/asterion/compiler

**Five-pass planner / Originals misrouting:** https://github.com/Azrazieliz/AsterionArt/blob/977400c46dab56cb3b431b661867c12f1d562990/app/src/main/java/com/asterion/art/engine/GenerationPlanner.kt

**Prompt chunking:** https://github.com/Azrazieliz/AsterionArt/blob/977400c46dab56cb3b431b661867c12f1d562990/app/src/main/java/com/asterion/art/engine/ConditioningSchedule.kt

**Generation and enhancement executor:** https://github.com/Azrazieliz/AsterionArt/blob/977400c46dab56cb3b431b661867c12f1d562990/app/src/main/java/com/asterion/art/work/GenerationExecutor.kt

**Local Qwen reasoning:** https://github.com/Azrazieliz/AsterionArt/blob/977400c46dab56cb3b431b661867c12f1d562990/app/src/main/java/com/asterion/art/brain/AsterionReasoner.kt

**Methodology registry snapshot:** https://github.com/Azrazieliz/AsterionArt/blob/977400c46dab56cb3b431b661867c12f1d562990/app/src/main/java/com/asterion/art/methodology/DetailedMethodologies.kt

**Model development status:** https://github.com/Azrazieliz/AsterionArt/blob/977400c46dab56cb3b431b661867c12f1d562990/docs/MODEL_DEVELOPMENT_NO_BUILD_20261027.md

**Fusion B research:** https://github.com/Azrazieliz/AsterionArt/blob/977400c46dab56cb3b431b661867c12f1d562990/fusion/FUSION_B.md

**Native video source:** https://github.com/Azrazieliz/AsterionArt/blob/977400c46dab56cb3b431b661867c12f1d562990/app/src/main/java/com/asterion/art/video/NeuralVideoBridge.kt

**Successful compilation workflow:** https://github.com/Azrazieliz/AsterionArt/actions/runs/40902446702

Local audit source: AsterionArt_full_repository_audit_2026-10-29.md. This architecture document is a prospective canonical product contract, not a claim that every listed subsystem currently functions.

**OWNER-APPROVED ADDENDUM \| 30 OCTOBER 2026**

# ASTERION ART — PRODUCT ARCHITECTURE ADDENDUM

Version 1.1 supplement to the untouched 29 October 2026 master (v1.0). Recovered decisions, limits and unresolved owner questions are kept distinct.

# ADD-01 Status, precedence and revision boundaries

Owner-approved addendum dated 30 October 2026 to the 29 October 2026 Version 1.0 master architecture. It extends the canonical product specification; it does not rewrite, delete or silently supersede existing sections. Where a later direct owner correction expressly changes earlier requirements, that later decision controls only the named dimension. Historical documents, source assets and previous methodology versions remain preserved.

- OWNER-APPROVED means the owner explicitly decided the behavior. HISTORICAL means an older methodology or scale exists and may require reconciliation. PROPOSED means not yet authorized; do not treat it as a product setting or default. SOURCE-IMPLEMENTED / CI-PASSED / DEVICE-VERIFIED / OWNER-ACCEPTED remain four separate evidence statuses.

- No unapproved architectural or design decisions. Where a requirement is ambiguous, ask and record the answer before modifying the affected behavior. Approval to implement already-clear requirements is not approval for unrelated model replacement, methodology redesign, altered rendering semantics, release/signing changes or destructive device actions.

- The owner is currently unable to perform target-phone testing. Continue implementing the specified systems in an isolated GitHub development branch; do not interpret the absence of device testing as permission to claim they function on-device.

# ADD-02 Universal image workflow: two alternative routes

Artwork Creation, Sheet Creation and the nine Originals must expose two alternative paths after the common initial references-and-parameters step. They share the same dedicated native generation machinery but not a generic methodology.

- STEP 1 — The owner selects the creation mode, references and their roles, mode-specific parameters, applicable B/E/T variables, concepts and other instructions. Draft inputs must be persistent before any Start action.

- ROUTE A / 2A — Direct inference from those inputs using the selected mode's own executable configuration, followed by 3A native image generation and applicable background processing. This preserves the former direct Qwen-based flow. Route A must not be replaced by an obligatory text approval stage.

- ROUTE B / 2B — The app constructs a faster-to-inspect, user-facing NON-IMAGE design specification. This is not merely a long prompt, essay or generic six-field board. It presents structured proposals for the whole output image AND fully detailed independent constituent designs, as required by that mode. The owner can edit, modify, alter, fuse, improve, select and freeze appropriate attributes within that methodology.

- ROUTE B / 3B — An explicit Bridge sends the finalized complete design state, exact images and role references, frozen decisions, parameters and methodology-specific output layout to the dedicated image-generation mode. Only then does native generation begin. Do not run a second unrelated concept-design inference that overwrites the owner-approved design.

- One Route B design-and-bridge flow corresponds to ONE requested finished output image (which may contain many independent designs); do not multiply intermediate screens or image jobs by entity merely to satisfy the design count. Keep Route A available in every applicable creation mode.

- Route selection and every unfinished edit persist independently by workflow. Generated text specifications and alterations are separate saved work products, not generated image assets. The model/inference strategy for either path must respect the authoritative mode and preserved inputs.

# ADD-03 Methodology as executable mode-specific configuration

The owner explicitly rejects treating methodologies as text documents that an AI is simply asked to read. Each mode must implement its own complete executable behavior, from interpretation through validation, design formation, arrangement, freezing, editing, native conditioning and final image construction.

- Each mode owns: exact inputs and source authority; valid parameter structure; concept selection and combination semantics; internal design-state schema; entity count and individuality; the complete output-image composition; viewpoint and dimensional contracts; transition/alteration/fusion rules; frozen identity and source constraints; prompt/native conditioning compilation; acceptance validation; and a separately saved editing and revision history.

- Shared infrastructure is permitted for storage, inference calls, background scheduling, enhancement, image assets, versioning and review. Shared infrastructure must not homogenize creative methods or inject Artwork's five quality phases into Originals.

- The existing historical methodologies and full user-provided archives are binding source materials. Distinguish ChatGPT-era execution constraints from actual creative methodology requirements before editing any rule; preserve originals and version the executable interpretation.

- The user has identified the previous coverage requirements and neutralized/forced terminology as ChatGPT accommodations rather than desired creative requirements. Remove these artifacts from the app's creative execution contract, without removing anatomical, identity, perspective, fidelity or other genuinely approved output requirements.

- Any unrelated changes to the substantive specifications of Character Creation or the other Originals still require separate explicit owner approval.

# ADD-04 Character Creation: full five-design Route B

Character Creation has already been specified in detail by the owner. Reuse its complete prior requirements; this addendum clarifies the new interaction pattern rather than replacing them.

- ONE Route B text/design flow produces the specification for ONE full output image containing FIVE independently conceived characters, not five separate per-character image requests. Each character must remain fully detailed and conceptually distinct.

- Present the sheet-level requirements once: composition, visual grammar, selected concepts, output geometry, required views, source relationships and any shared constraints. Within the same plan, preserve FIVE independent character identities and their complete anatomy, face/eyes/hair, proportions, morphology, structural/topological concept fusion, materials, distinguishing features and individual view instructions.

- Each character is individually expandable, alterable and revisable before the bridge. Keep the other four fully intact when editing one; show useful comparisons without forcing the owner to wade through a single flattened description.

- Preserve the original Character Creation requirements for same-identity front, exact side profile and rear views, phase-specific construction, original character diversity, internal identity consistency, and any source-derived requirements. The old ChatGPT-specific coverage and terminology substitutions are not normative.

- Retain Route A's existing direct character preparation. Do not change character methodology, counts, model choices, or runtime behavior under the guise of performance optimization.

# ADD-05 Artwork, Sheet Creation and other Originals

The same Route A / Route B choice applies across Artwork Creation and the other eight Originals, while the inner design object, number of independent proposals per single image, composition and operations are defined solely by the selected mode's approved methodology.

- Artwork: retain the mandatory Character, Theme and Art Style source authorities and optional Element. Preserve its owner-defined five exclusive quality phases. Route B must represent the entire artwork: subject identity, exact theme and style interactions, composition, camera, materials, effects and owner changes, not only separate independent component cards.

- Sheets: retain four distinct roles (Character, Theme, Art Style, Element), usually beginning from one owner image, with actual new missing-view/details reconstruction and exact role-specific presentation; do not substitute cropping and pasteboards. Character Sheet V5 retains the original one-complete-9:16-image requirement.

- Outfit, Weapon, Transformation, Mutation, Creature, Alternate Universe, Feminisation and Art Style Correction: preserve the original individual methodology and correct single-image counts/arrangements. Route B must edit every complete design inside ONE image-specification plan, with its own freeze/alter/fusion constraints and complete visual fields.

- A generated Original (including Feminisation) can subsequently become a reusable analytical Character, Theme, Art Style or Element Sheet as appropriate. Select the ACTUAL completed design image as source authority, retain its identity and provenance, then perform a new independent sheet-generation task; do not substitute a parent prompt or cropped multi-design catalogue.

- Reference reuse must not force automatic generation of additional sheets the owner did not request. Make the conversion available explicitly through the design library and retain versions and source assets.

# ADD-06 Theme Creation: text-only mode and text-to-sheet bridge

The owner chooses the older THEME CREATION method as the basis; older overlapping methods are not to be reintroduced as parallel duplicate image workflows. Theme Creation itself produces TEXT designs, not images, previews or mandatory multiple image renders.

- Inputs: original B/E/T structure, binding keywords, optional character configuration, optional liquid systems, approved additional parameters and other owner direction. Keyword constraints apply to development/synthesis; the atomic roulette is independently selected rather than biased to produce the keywords.

- Design process: independent atomic roulette, frozen source atoms, heavy-atom construction and molecular/visual-system synthesis yielding FIVE genuinely different TEXT themes, not five renderings, color swaps or camera angles. The generated themes must be reusable coherent physical/visual concepts, not short names or unsupported narratives.

- Text-phase actions: select, revise, alter, improve and fuse theme specifications while retaining original themes, frozen atoms and the provenance of every version. The owner can compare and approve the selected theme. Do not require image previews or multiple rendering calls inside the Theme Creator.

- Character configuration retains the historical theme-method probabilities/meaning until the owner approves their revision. The historical E-dependent configuration change must not be replaced by a made-up numerical formula. A liquid system is OPTIONALLY selectable; substantial liquid content is no longer mandatory.

- Only the explicitly APPROVED text theme is passed through a dedicated Bridge to Theme Sheet creation. This starts a NEW background image-generation job, maintains its text and version identity, and yields a reusable visual Theme Sheet under the existing Theme Sheet methodology. No Theme Creator step itself renders an image.

- Text drafts, themes, alteration/fusion histories and the approved target persist. Other mode-specific Route B flows and the Theme Creator must not be confused with one another.

# ADD-07 B/E/T redesign: owner-controlled general parameter system

Keep the original B (Badassness), E (Eroticity) and T (Contextual Tension) conceptual structure and its role as a comprehensive overall-creation parameter system; EXPAND and MODIFY it using additional owner-approved parameters and variables. The three historic values and 0–200 scoring convention are a RECOVERED starting point, not a newly approved fixed limit or final scale.

- B/E/T and extensions must direct the relevant entirety of the output image and its visual systems as appropriate to the selected mode; avoid a disconnected arbitrary slider per visual element or per character that replaces the primary overall-creation definition.

- The owner explicitly intends E at its highest requested setting to preserve the intended extreme explicit adult visual intensity rather than being silently softened to a mild or unrelated depiction. Do not introduce invented creative caps, substitution wording, censorship-like prompt workarounds or automatic dilution into the application specification.

- The precise new parameter names, values/ranges, internal variables, interactions, mode relevance and inheritance rules HAVE NOT yet been defined by the owner. They are not authorized defaults. Existing B/E/T historic definitions must be preserved as history while a versioned, configurable new schema is specified.

- Owner-defined settings and keywords must be persisted with every draft, text specification, bridge, generation job and revision. Do not allow an inferencer to override or reinterpret the values when producing the final output.

- Potential parameter proposals may be presented to the owner for selection, but no proposal becomes binding merely because a developer suggested it.

# ADD-08 Owner image reference and sheet presentation authority

Seven visual references were supplied on 30 October 2026. Preserve the original files; they describe expected hierarchy, detail and quality, not evidence that Asterion Art itself generated them.

| **Reference image(s)**               | **Binding reference purpose**                                                                                                                                                                                                          |
|--------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Francesca Prelati; Artoria Pendragon | Character Sheet: clear 9:16 analytical hierarchy; identity continuity, orthographic anatomy, face/eyes/hair, correct physical construction, equipment/material/palette details. Distinct canonical content stays source-authoritative. |
| 799225.png; 799222.png               | Theme Sheets: canonical scene and compositional system, physical/material interactions, cameras, spatial relations, environmental hierarchy, constraints and source-directed presentation (not mandatory Character Sheet aesthetics).  |
| 855688.jpg                           | Character Creation: FIVE distinct character concepts in one 16:9 output composition with method-required complementary front/profile/rear consistency.                                                                                 |
| 713603.png; 714220.png               | Feminisation: either visual presentation is acceptable; independently generated female design remains eligible for a subsequent reusable Character Sheet.                                                                              |

Canonical uploaded filenames, dimensions and SHA-256 digests are recorded in repository document docs/owner_recovered/REFERENCE_IMAGES_AND_SHEET_REUSE_20261030.md and in the owner-reference archive. Do not replace actual source pixels with a verbal description.

# ADD-09 Durable pre-Start drafts and autonomous background jobs

Unlaunched designs and already-started generations are both first-class persisted objects. The owner must not lose selections, specifications or progress by leaving the app, navigating away, closing the screen or resuming later.

- DRAFT persistence: save parameters, B/E/T, keywords, selected references and permissions, role assignments, mode, concepts, selected options, instructions, text proposals, edits, versions and video ordered keyframes. Exiting before pressing Start must NOT require reentering the entire request.

- Separate authoring from execution. Merely editing or auto-saving a draft never starts inference or image generation. Pressing Start (Route A) or the explicit approved specification Bridge (Route B) creates an actual generation job.

- Once generation starts, execute off the Activity in an Android-appropriate foreground/background mechanism with notifications, durable FIFO or resource-safe scheduling, intermediate checkpoints, resumability and correct error/cancellation semantics. Leaving the app UI must not terminate a supported in-progress task.

- Queued jobs preserve original input snapshot, source URI access, model choices, staged outputs, provenance, actual progress and failures. On interruption, restore unfinished work and avoid discarding valid completed checkpoints. Do not perform duplicate concurrent QNN work on the same model/plan.

- Draft and job histories remain accessible for further edits, reuse and regeneration. Do not clear user data automatically upon submission or completion; explicit deletion and versioning are distinct operations.

- Do not silently promise that Android will ignore battery/process termination policies. Handle supported lifecycle behavior transparently; true service endurance remains to be verified on-device.

# ADD-10 Generation, automatic enhancement, video and model boundaries

The master architecture's native image, enhancement and video obligations continue to apply; the October 30 owner direction changes sequencing of engineering work, NOT the final intended capabilities.

- Finish the currently specified app-level functions first. DO NOT optimize Qwen reasoning/inference BEFORE on-device testing; the user has observed extremely slow and inefficient pre-render reasoning but requests behavior validation before attempting performance changes.

- DO NOT tailor, train, fine-tune, merge/fuse, replace or otherwise develop the AI/model ecosystem yet. That work is deliberately postponed until actual on-phone rendering and behavior have been observed. Preserve existing installed local models and original Qwen model decisions; do not choose a different model by developer preference.

- Native images must still honor exact reference conditioning, native model capabilities, full mode-specific configuration, genuine required output quality and real pixels. Do not claim good quality simply because the APK builds or a file is produced.

- Complete the owner-required autonomous enhancement flow (Image Toolbox, RealSR and SuperImage where applicable) without inventing the order, automatic triggers, failure semantics or undocumented model-specific substitutions. Any unfixed selection of enhancement order or mechanism must be approved before changing it.

- Video Creation remains an actual 10–30-second phone-local workflow with one initial image or genuinely ordered multiple reference frames and continuity; mere stitching of separate segments is not acceptance. Preserve native background operation and explicit source/keyframe order.

- Model behavior correction, comparative learning and tailored checkpoint development are later tasks, after device evidence. NoWorld export remains deferred unless explicitly resumed by the owner.

# ADD-11 Repository, deployment and evidence discipline

All additions must preserve the existing approved assets, functioning code and user data. Source implementation, successful CI packaging, actual phone results and owner approval are different milestones.

- Repository: Azrazieliz/AsterionArt (previously Azrazieliz/AsterionCompiler). Use a separate development branch and record new documentation and code changes without merging to main or installing on-device unless explicitly approved.

- Do not run adb uninstall, pm clear, delete files/asterion-qnn-models, change application identity or signing identity, overwrite model weights, or discard user character plans, downloaded models, sources, images or grading data.

- Preserve the working original Compiler and recovery content and exact owner reference assets. Use concrete documentation, code and CI evidence; a declared status is not an acceptance test.

- As of 30 October 2026: linked development PRs \#5 (Original to reusable sheet), \#6 (durable drafts/background queue), \#7 (Character Creation Route B) and \#8 (Theme Text and other Route B modes) exist. PR \#8 CI at commit fc6cf4add passed. These are SOURCE / CI evidence only, not proof of complete or correct on-device operation. Existing original-key signed installable release remains unresolved.

- Any further new features the owner plans to specify after completing the present requirements are not yet part of this addendum; do not invent them.

# ADD-12 Questions still awaiting owner decisions (NOT approved rules)

The following are explicit specification gaps. They are questions for the owner, not permission to make implementation decisions.

| **Question**                          | **Unsettled decision**                                                                                                                                              |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| B/E/T redesign                        | Exact additional variables and scopes; any revised scale, thresholds, mode inheritance, presets and numeric interactions. Historic 0–200 is not presumed immutable. |
| Theme atomic roulette                 | Actual atom universe/registry, exclusions, source, seed/independence and how broad independent selection is verified, not merely described in an LLM prompt.        |
| Theme character configuration         | Whether to retain the historical 85% / 12% / 3% one/two/three-character odds and how E changes them. No invented formula.                                           |
| Theme text presentation               | How concise the five initial proposals should be on phone; which fields must be immediately visible vs expandable; editing and comparison behavior.                 |
| Cross-mode Route B operations         | Exact mode-specific alteration/fusion/improvement operations and protected fields wherever the original methodology does not fully define them.                     |
| Enhancement coordination              | Actual per-mode Image Toolbox/RealSR/SuperImage order, independent automatic activation rules, failure/retry policy if not fixed in authoritative originals.        |
| Device evidence and model development | Owner device tests and observed output quality required before performance optimization, training, architectural model changes or signed release decisions.         |

This addendum adds no automatic E attenuation, no unapproved design grammar, no extra per-character B/E/T overrides and no fabricated settings for unconfirmed decisions.

# ADD-13 Owner corrections: parameters, taxonomy and Artwork selection

Authoritative correction, 30 October 2026. This section supersedes any contrary implementation notes, parameter labels or pipeline counts in preceding addenda and development branches. The original master remains otherwise unchanged.

## Parameters: no invented definitions

Keep the previously defined B/E/T conceptual structure while allowing additional top-level parameters and internal subparameters, but do not fabricate their names, meanings, dimensions, or relationships. The owner has not yet defined the new parameter catalogue. An implementation may persist owner-authored parameter names without claiming that proposed axes were approved.

## Intensity: continuous and uncapped

No fixed 1–10 scale and no named absolute maximum at 10. Zero means disabled; one already denotes a prominent, meaningful presence. Numerical intensity can increase continuously to any positive decimal value, including above 10, without an arbitrary software ceiling. Previously saved B/E/T plans are preserved. Model ability to realize every setting remains subject to device testing.

## Taxonomy: definitions pending

The owner has not yet supplied the structure, categories or content of the proposed new taxonomies. Do not create categories, populate example atom lists, or modify the existing Character Creation concept taxonomy without owner approval. Remove premature taxonomy code and interface components; the original taxonomy remains untouched. Theme text roulette exists, but its future taxonomy source is explicitly unresolved.

## Artwork: one best image continues, not three

Exactly five Artwork phases. At each phase generate only the configured number of candidate images (N); select exactly ONE best image and pass ONLY that image to the next phase. After the fifth phase, automatically enhance the final single winner. Default N=3 means 3 images per phase and 15 image generations in total, NOT 39. For user-selected N, total is 5×N. Retain stage images and fingerprints for recovery; do not multiply N by earlier surviving paths.

## Development boundaries

Do not optimize or tailor models before on-device testing. Preserve original signing identity, installed models, source assets, user data and recovery plans. These implementation corrections are source-level until CI and device checks separately pass. Owner review remains required before merging or installing.

*Change control: this correction records owner instructions; it does not authorize additional taxonomy construction, parameter definitions, model retraining, merging or installation.*

---

# ADD-14 — Taxonomy baseline and clarified mode-specific parameter scope (owner, 30 October 2026)

**Status:** Binding owner corrections; the taxonomy *proposals below* are not approved. This append-only annex supersedes any earlier inconsistent wording.

## A. Canonical taxonomy baseline

The owner-provided `Concepts.md` is the structural/rigor baseline for ASTERION's future taxonomies. It already exists in this repository as [`app/src/main/assets/methodology/concepts_full_v3.md`](../../app/src/main/assets/methodology/concepts_full_v3.md), matching the owner upload byte-for-byte **apart from one final newline**. No taxonomy is to be populated or reclassified merely because it was proposed by the assistant.

Existing canonical structure: **18 BC** roots, **74 SF** subfamilies, **455 SC** selectable concept leaves, **59 alias records**, **12 domain views**, intersectable query facets, **60 MK** non-selectable materialization entries, and **23 T** non-selectable fusion/topology structures. §4 specifies an actual recursive graph; §3.10 defines canonical obligations for each selectable leaf; §§1–2 prescribe the four dispositions and stringent admission tests; §§7–7A distinguish materials/topologies from directly selectable concepts.

The owner expects future taxonomies to share the *architecture and rigor*, not blindly copy the specific Character Creation identities or admission metric. Every new taxonomy proposal must specify what it classifies, its rooted/family/leaf hierarchy, unique path and ID rules, leaf obligations, admissions/rejections, alias/query semantics, cross-taxonomy links, and precise effect on a particular methodology. The owner must approve the taxonomy itself *before* it is built. Do not invent a dictionary of categories, add sample atoms, allocate IDs or modify the existing SC hierarchy.

## B. Correct creative-parameter scope

**B/E/T applies to text-only Theme Creation**, with potential additional, owner-approved theme-specific parameters. The earlier assistant-designed 16-axis universal model and default axes for unrelated modes were rejected. Each other creation mode may get *its own* real controls based on its unique governing methodology, but no general sliders, global B/E/T, or proposed axis naming becomes canonical without owner approval. No fixed artificial 1–10 intensity ceiling is accepted: an intensity of 1 is already prominent and the scale must allow greater values without clipping. Existing historic 0–200 values and saved drafts must remain readable without arbitrary conversion. Any old B/E/T-in-non-Theme runtime wiring is a defect requiring removal, not a new requirement.

## C. Artwork's corrected image count

All five pre-existing Artwork phases stay authoritative. Each phase generates the selected batch count `N`, selects exactly **one** best output, and transfers **only that one** to the next phase. Exactly `5*N` native image generations, e.g. 15 for `N=3`, not 39. After phase five, the single winner is automatically enhanced. Native ranking and enhancement quality remain unverified until device testing. Keep checkpoints and parentage. No model optimization/training/tailoring before phone evidence.

## D. Unaltered product boundaries

Keep Character Creation's full five-independent-designs-per-image specification, separate A direct and B editable text-first routes; maintain independent methodological source authority for other Originals, Theme Creation text-first and the approved Theme Sheet bridge; preserve reference pixels, user data, existing models, signing identity, durable drafts and background job integrity. This development branch is unmerged, and CI packaging does not prove device quality.

## E. Pending owner choices — not implemented canon

The formal classification purpose and admission gate for *each* additional taxonomy; whether the existing SC taxonomy is expanded before creating a new Theme-source taxonomy; which new Theme-only parameters actually improve generation; and whether particular Originals need any additional controls. All require owner review. Candidate designs live in separate **PROPOSAL** documents and are not requirements.


## ADD-15 — Final owner correction: Art Style Sheets are sufficient (2026-10-30)

- **Reject the assistant-proposed Art Style taxonomy.** ASTERION already has user-approved **Art Style Sheets** and their mode-specific conditioning. No parallel style-leaf taxonomy, style index or implicit style ontology is authorized.
- B/E/T is **Theme Creation-specific**. Do not display or apply B/E/T or default intensity values in Artwork Creation, Character Creation or other Originals. Existing historical intensity values may be deserialized for compatibility, but must not steer those modes.
- The **existing** `Concepts.md` §4 BC/SF/SC recursive selector is not a new proposed taxonomy. Implement its canonical branch/view pools, strict constrained-X precedence and SC-only uniform draws as specified by `Character Creation v3` §12, without altering the classification itself or its IDs.
- Theme taxonomy schema remains **undefined** by the owner. Do not invent category names or candidate leaves. Preserve the previously approved text-only Theme workflow, pending a separately authorized taxonomy specification.
- Continue implementing established application systems; do not spend implementation time on new unrequested taxonomies, generic parameter sliders, performance optimization or model tailoring before actual device testing.

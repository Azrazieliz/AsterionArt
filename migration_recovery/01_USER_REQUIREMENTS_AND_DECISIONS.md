# Canonical user requirements, corrections, and rejected assumptions

## Product and UX
1. **ASTERION ART** is the primary Android studio, not a separate one-off Compiler or a generic text-to-image chat. It uses Asterion Core as the canonical sheet/asset authority and exports approved work to NoWorld.
2. Phone-first on Samsung Galaxy S26 Ultra, background-first automation. Jobs, model imports and intermediate results must survive navigation/restarts where OS permits; visible progress, cancel/retry and finalized results on return. Never pretend Android can provide unconstrained always-on inference; thermal/RAM limits matter.
3. **Create -> Image or Animation** only at first selection. **Image -> Artwork, Defaults (methodology modes), Sheets**. Animation opens real video workspace. Useful Home dashboard: recent images, active queue, creation shortcuts, model status, sheet browser. Premium visual design, not a generic dense form.
4. All user interactions should be clickable galleries, source selection chips, checklists, mask brush, crop gestures, sliders and settings. **No manual writing except search terms in the Asterion Core sheet bridge**. The Default mode should already know its own required inputs/geometry.
5. Absolutely exclude translation/CBZ/retypesetting/publishing from Art: user will build a separate translation app. Pose workflow and prompt writing are internal generation policies, not launcher tiles. Do not make character/default creation depend on irrelevant sheet types.

## Four normalized sheet types
- **Character Sheet, Art Style Sheet, Theme Sheet, Element Sheet**, each source-authoritative, versioned, deterministic/professional layout with machine-readable record and actual reference crops. Avoid generated microtext masquerading as verified text.
- Character Master Sheet must contain largest full-body canonical identity, face neutral/front/three-quarter/profile, eye close-ups, hair views & alternate canonical states, body/proportions/anatomy, outfit front/side/back, color palette, identity invariants and selective distinctive details.
- The character-specific slot is **optional typed module**: Saber Artoria uses Excalibur, Lancer Artoria uses Rhongomyniad, Francesca does not need a weapon; use wings, horns, tail, tattoo/markings or another real distinguishing feature instead. Do not fabricate content to fill templates. Artoria Saber and Lancer are distinct form IDs; canonical details must never blend.
- Preserve proportion locks and identity hierarchy. Character Sheet v5 methodology generally prefers <=4 diagnostic callouts, up to 6 if justified. Include the original PNG/WebP boards in private recovery archive.

## Methodology modes — exact contracts, not generic art
- Include Character Creation, Art Style Correction, Outfit Creation, Weapon Creation, Transformation Creation, Mutation Creation, Creature Creation, Alternate Universe Creation. The Methodology.zip is the source authority. Preserve all versions for historical traceability, with **Character Creation v3 overriding v2/v1.5** when conflicting.
- **Character Creation v3:** creates new original identities, **no Character Sheet or Theme Sheet prerequisite**. Phase 1 = 15 distinct authored characters, exactly THREE separate 16:9 white analytical sheets with FIVE characters each; each character must show same body's front, exact 90-degree profile and rear; no outfit/weapon/environment, no 3/4 substituted for profile; require v3 Concepts and Design_Direction (455 selectable concepts) and phase-specific freezes/approval for outfit, weapon and assembly. The precise primary text is in `Methodology/Character Creation v3/characters_creation.md`.
- **Art Style Correction:** target Art Style Sheet plus one original illustration ONLY. Original content/identity/pose/framing is immutable; redraw entire drawing/render style without importing subjects from style-reference image. Retain framing/aspect; do not require Character or Theme Sheet.
- Outfit Creation: source identity or source image, 10 distinct outfits in white 16:9 2x5 catalogue, freeze body/face/anatomy; single selected 9:16 preview.
- Weapon Creation: source identity + optionally frozen outfit, 10 distinct isolated weapons in white 16:9 2x5; no hands, character, effects or scene in catalogue.
- Transformation: five coherent states of same identity, uncropped white analytical catalogue.
- Mutation: five distinct structural deviations, default directional 70% retained identity / 30% mutation pressure, NOT merely a power transformation or clothes swap.
- Creature Creation: five original coherent anatomy/locomotion/structure designs; no preexisting character/theme requirement, concepts source optional.
- Alternate Universe: five distinct worlds with same character's key identity protected; explicit world direction/concept.
- Separate output geometry, authority, freezes and selection for every mode; do not converge into one generic artwork prompt.

## Generation, models, and automation
- **Five-pass image creation:** pass 1 establishes ALL major structure, characters, clothing, geometry, environmental theme, interactions, pose/camera and major lighting. Pass 2 stabilizes identity and geometry. Pass 3 resolves surfaces/materials/lighting. Pass 4 resolves finer details while preserving earlier geometry. Pass 5 integrates coherent final rendering without major redesign. Candidate grading, selection and enhancement happen AFTER pass five. Cannot claim five actual inference passes if backend only does one; preserve pass outputs/provenance.
- Default is automated generation + grading + best-candidate finalization and enhancement; user will not direct tiny LLM-like instructions about hands, eyes, horn shape for each image. Manual masked editing exists but is secondary and must not alter pixels outside user's mask.
- Rank: **Rin Flanime > MiaoMiao > Illustrious Hassaku XL > NoobAI XL v11**. Animayume mostly rejected. One useful automatically selected anime route, not five visible choices. Optional fused Anima backbone is only deployable if benchmarked superior to unchanged Rin.
- **Fusion B:** candidate 70% Rin/30% MiaoMiao (Rin FP16 original, MiaoMiao BF16 original); 685 matching tensor keys/shapes; FP32 accumulator, Rin FP16 output. The source Rin original v1.4 was NOT proven identical to the preferred user's Rin Flanime v1 Turbo QNN package. **No real merged model** or QNN conversion confirmed. Hassaku is Illustrious/SDXL and incompatible with direct Anima weight averaging.
- Users also want to test multiple other Illustrious checkpoints discovered on Hugging Face. Original safetensors checkpoints must be tagged as **not directly runnable QNN** until converted; import must handle source and compiled formats distinctly.
- Expose model output tags **Anime, Realistic, Cosplay, Other, Animation** as supported with true capability detection, and support model-specific param routing.
- Screenshot controls demonstrate available params, **NOT desired numeric defaults**: aspect ratios 1:1,3:4,4:3,9:16; schedulers DPM++ 2M, 2M SDE; Karras, steps, CFG, batch count, img2img denoise, optional seed. Automation may tune many more model-valid parameters, without silently dropping prompt/spec authority.
- Realistic cosplay does NOT normalize horns/wings/tails/species anatomy to human; preserve distinctive body semantics.

## Video/enhancement
- Nightmare-like video must be **inside** Asterion Art UI. Support still->video, multiple ordered image/keyframe sources, world manifestation/ultimate, background output and NoWorld export. Native multi-keyframe conditioning not verified; current separate Wan-compatible local API/segment stitching must be labeled truthfully.
- Automatic best-output enhancement requested using user's **SuperImage Pro**; do not claim proprietary binary integrated without actual supported rights/capabilities. Also inspect Image Toolbox and RealSR; currently implemented licensed/fallback toolchains should not be confused with exact SuperImage Pro.
- Imported huge models 6–7 GB, ~300K Core images; avoid duplicate copies and escalating heap usage. Preserve installed app data, model library and signing. No forced reinstall after importing models. Stable signed release/update critical.

## Explicit user feedback / corrections
- Earlier assistant wrongly called screenshots' incidental settings “defaults”: **corrected**.
- Earlier assistant described interactive LLM-directed micro-edit loop: **rejected**; methodology is designed specifically to avoid such micro-instructions.
- Earlier assistant suggested CBZ utility inside Art: **rejected**.
- Earlier assistant mixed all create modes under one generic tab: **rejected**.
- Earlier assistant required character/theme sheet for Character Creation and Art Style Correction: **rejected**.
- Earlier assistant considered four checkpoints weighted equally: **rejected**; choose tested strong backbone, Fusion B preferred over equal merge.
- Earlier assistant offered more architecture/roadmap despite implementation authorization: **rejected**; user wants real work and accurate reports only when requested.

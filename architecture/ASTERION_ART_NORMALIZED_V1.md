# Asterion Art normalized architecture v1.0 — canonical decisions

This contract supersedes ASTERION_ART_INTEGRATION.md wherever they conflict. This is architecture, not a built APK.

## Ownership
- Asterion Core owns canonical characters, distinct character forms, reference assets, versioned sheets, output records and feedback.
- Asterion Art combines compiler, Android NPU image engine, deterministic sheet builder, image/video editor, grading, background queue and enhancement in one app.
- NoWorld receives approved still and 2D video outputs, keyframes and provenance. Existing AsterionCompiler continues only as a reusable legacy source; do not modify upstream Local Dream.

## Sheet roles
CHARACTER, ART_STYLE, THEME, ELEMENT. All four use structured schemas, external metadata, original source references and deterministic professional layouts, not freehand AI-generated tiny typography.

### Character Master Sheet layout (semi-fixed)
1. Header: canonical name + franchise/form
2. Dominant full-body identity
3. Face: front / 3-4 / profile
4. Eyes: essential closeup
5. Hair: front / side / back, canonical states
6. Body: proportions and anatomical views
7. Outfit: front / side / back, construction
8. Character-specific typed module only when justified: weapon, equipment, wings, horns, tail, markings, species anatomy, mechanical limbs, transformations, etc.
9. Physical and outfit/equipment color palette
10. Identity locks and essential diagnostic details

Layout adapts to identity. Default diagnostic callout count <= 4, exceptional <= 6 when justified. Do not fabricate a weapon to fill an empty panel. Artoria (Saber) and Artoria (Lancer) require distinct form IDs; their outfits and Excalibur/Rhongomyniad must not be merged. Francesca's sheet does not need a weapon module if no source requires it.

## Visible modes
- CREATE IMAGE, CREATE VIDEO, CREATE SHEET, HISTORY/GRADING, ADVANCED EDIT, MODEL LAB, SETTINGS
- CREATE SHEET: Character Sheet, Art Style Sheet, Theme Sheet, Element Sheet
- CREATION: Character Creation, Art Style Correction, Outfit, Weapon, Transformation, Mutation, Creature, Alternate Universe

Pose workflow, Prompt compilation, validation, authority, freeze/revision and five-pass sequencing are internal default rules, not separate launcher modes.
Do NOT ship CBZ translation, retypesetting, publishing or archiving in Asterion Art.

## Mandatory touch UI
Normal work is click/tap driven: sheets retrieved from Core via search, visual card choices, trait chips, galleries, image import, crop gestures, segmented controls, sliders and rating stars. No ordinary text authoring. The only routine text input is Core-sheet search. Unknown custom concepts require import + tap approval of extracted proposals, not invented completion.

## Five-pass image creation
Source methodology: Pass 1 establishes every major visual structure; passes 2 onward refine without redesign. Art-specific interpretation:
1. Foundation: identities, outfit, pose, interaction, macro environment/elements, camera and main lighting.
2. Identity/geometry stabilization: preserve pose/layout, correct actual identity and physical contacts from references.
3. Materials/lighting: surfaces, shading, reflections, scene material interactions.
4. Micro-construction: details only of already established anatomy, hair, costume, accessories and elements.
5. Final coherence: tighten consistency and local rendering without major redesign.
Candidate grading, selection, enhancement and Core export happen AFTER pass 5. Native backend must support image-to-image/latent continuation/inpainting before claiming all five physical stages ran.

## Anime fusion research gate
User ranking Rin Flanime > MiaoMiao > Hassaku XL (Illustrious) > NoobAI XL v11. Animayume excluded as a primary route.
First explore compatible Rin+MiaoMiao Anima-family blend with ORIGINAL compatible weights. Hassaku is Illustrious/SDXL, cannot be merged by direct averaging into Anima/QNN model. Cross-family teacher distillation is experimental. Deploy one anime automation route ONLY after the merged candidate surpasses Rin baseline; otherwise one-click automation can select a model behind the scenes.

## Animation, grading and background
Nightmare-compatible video engine integrated within Art: true image-to-video + ordered keyframe video generation when supported; otherwise labeled segmented interpolation, not falsely claimed native multi-frame conditioning. Saved candidate/user preference scoring adapts routing and controls; model weights change only through deliberate fine-tuning. Durable foreground execution while generating with thermal/battery safety and persisted queue; completed results shown on return, with Android OS limitations respected.

## Binding constraints
Use permissioned Binder/AIDL for Core-Arts actions, content-URI grants for media, and provenance hashes for source references and outputs. No 6–7 GB file copying into every app. Honor source versions, preserve unique character/form IDs, and never normalize fantasy anatomy in cosplay mode. Photograph-like realism changes rendering, not established wings/horns/tails.

## Validation status
This file encodes product architecture only. Until runtime implementation/benchmark: no APK, fused checkpoint, dynamic LoRA engine, or integrated video backend may be claimed complete.

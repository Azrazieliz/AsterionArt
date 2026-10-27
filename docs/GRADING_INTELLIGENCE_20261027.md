# Asterion Art — intelligence and grading contract (2026-10-27)

**Owner-approved scope**: remove Keep/Reject; use Advanced Grading with detailed manual assessment, visual evidence and exact correction targets. Do not reinterpret Originals methodologies or restructure existing canonical taxonomy.

## Exact behavior implemented

1. Design Library selection, Alter, Regenerate and Fuse are refinement/selection actions, never implicit training labels.
2. Advanced Grading has optional category ratings (0 means not assessed), optional overall 1–5, feature name, an explicit Preserve field and explicit Correct field.
3. User may grade whole image, select a design from a generated sheet (capturing that design's sheet cell), or specify a normalized rectangular image region.
4. On explicit Save, the actual region is decoded from the rendered image, stored as private PNG, hashed SHA-256 and converted into bounded 4x4 spatial RGB/edge features.
5. Per-mode and per-category locally trained regression heads learn ONLY from explicit submitted scores + pixel samples. Updating/deleting grading recomputes parameters so stale labels are not retained.
6. Only user-authored Preserve/Correct instructions can influence neural text planning. Context must match same creation mode, relevant concept(s), and optionally exact design ID. Numeric grades alone do not create instructions.
7. Dedicated Phase-1 Character Qwen author uses contextual grading notes as evidence after canonical concepts; notes cannot override fixed methodology or taxonomy.
8. General Qwen reasoning incorporates only context-scoped grading notes, never Keep/Reject. Existing native Qwen LoRA loader remains available.
9. App-private JSONL retains individual dimensions, image URI, exact local patch path and checksum. User can explicitly export ZIP with the actual cropped PNGs and JSONL; no automatic uploads.
10. An optional Qwen3 PEFT supervised LoRA training recipe learns the *textual scope of user-written corrections*. A text-only Qwen adapter cannot understand image pixels and is not a vision/diffusion/video LoRA.

## What this is NOT

- Not an automated anatomy assessor, a vision-language model, or an improved diffusion checkpoint.
- Not a promise that a 1–5 grade captures the cause of a visual failure. Only user-supplied notes specify preservation/correction.
- Not a reason to change any Original's methodology or to fragment the taxonomy.
- Not a claim that new Qwen or image LoRA weights have been trained or visually benchmarked.
- Not an app build or a release. This branch is source-only until the owner authorizes builds.

## Data guarantees

- No Keep/Reject actions remain in the active Design Library. Historical statuses are interpreted neutrally; existing artwork is not deleted.
- Explicit Grading is amendable and deletable. Retained image evidence is private and remains intact unless separate storage cleanup is implemented.
- Region coordinates refer to source image dimensions; image-derived features are numerical, not invented anatomical interpretations.
- User-authored notes are scoped by mode and concept, or exact design. A grade of an unrelated design is never generalized across methods.
- Automatic parameter suggestions are based on explicit new overall grading records for full images only, not historical stars or design-specific sheet-cell scores.
- All work stays on `research/model-capabilities-no-build-20261027`, with EUR 0 GPU spend, GitHub only, no desktop controller.

## Validation status

Implementation and unit-test sources committed. No GitHub Actions build, unit-test execution, GPU training, APK packaging or phone inference performed.

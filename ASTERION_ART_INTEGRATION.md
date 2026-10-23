# Asterion Art integration contract (v0.1)

Status: architecture and data contract only; no generation backend wired.

## Project layout
- Asterion Core remains the authoritative character, sheet and artwork database.
- Asterion Art consolidates compiler, on-device generation, editing, scoring, enhancement and animation.
- Existing AsterionCompiler main branch remains untouched. Development occurs on this branch.
- Third-party engines are adapters with separate license and hardware compatibility reviews, not copied APKs.

## Worksheet format
Four typed sheets: CHARACTER, ART_STYLE, THEME, ELEMENT. Each has an original reference image, extracted/cropped images, versioned structured fields, provenance, confidence and user corrections. Sheets can be assembled using deterministic layouts and reference crops; generative rendering is not required.

## User workflow
Choose 1+ characters, art style, theme, 0+ elements, and anime/realistic/cosplay/animation mode. Fantasy anatomy remains intact in realistic/cosplay mode. Automatic candidates are scored against reference requirements; user ratings and corrections override automated suggestions.

## Scoring and learning
Separate mandatory constraints from soft preferences. Preserve accepted/rejected candidates, per-criterion user scores, model/seed/parameter metadata and versioned scoring profiles. Adapt ranking, model routing and parameter selection from feedback; do not claim that model weights are updated without an explicit training backend.

## Video generation
Provide an adapter for supported local video engines (Nightmare-compatible if integration is technically and legally feasible). Support one-image image-to-video and ordered multi-keyframe conditioning with explicit start/end or intermediate frames when the selected backend supports it. For backends lacking multi-keyframe conditioning, use per-segment image-to-video, temporal consistency checks and interpolation/compositing; label these as approximations rather than native multi-frame inference. Export videos, keyframes, provenance and timing metadata to NoWorld. Do not claim a playable animation until rendered and validated.

## Execution gates
1. Audit Local Dream, Nightmare and image-editor/upscaler code licenses and native interfaces.
2. Add typed ELEMENT worksheet and deterministic sheet builder.
3. Connect Asterion Core reference and grading contracts.
4. Wire and device-test NPU image generation.
5. Add image editing, upscaling and video adapters.
6. Build, install and test a signed Android release.

The app should not impose additional content-category filtering on lawful, consensual adult creative work; upstream model constraints and applicable law still apply.

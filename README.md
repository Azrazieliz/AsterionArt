# Asterion Art — unified reference-led creation (in development)

This branch implements an in-progress Android app with application ID com.asterion.art.
Legacy AsterionCompiler is preserved on legacy-asterion-compiler-20261023.

## Implemented source

- Four-sheet project: Character, Art Style, Theme, Element.
- Editable reference-sheet creator with source images, role-specific fields, deterministic PNG board and embedded JSON facts.
- Original external sheets have an OCR fallback; authored sheets bypass OCR guessing; prompt authoring is not silently clipped at 850 characters.
- Anime / Realistic / Cosplay / Animation modes.
- Nightmare Mobile authenticated localhost API client for installed-model selection, still generation, image-to-video, media retrieval and real upscale workflow.
- Candidate batch, approximate technical scoring, explicit human grading, append-only feedback and per-mode preference learning.
- Multiple ordered first-frame video segments with optional approximate clip joining. Exact endpoint conditioning is not available.
- NoWorld asset manifest export with NOT_APPROVED provenance.
- JVM unit tests and GitHub Actions Android builds.

## Not yet done / not verified

- Native embedding of Nightmare/Local Dream engines; Nightmare currently runs as a separate installed app.
- Core direct content-provider integration and Core library saves.
- True multi-reference visual conditioning of every sheet image.
- Start-and-end-keyframe video inference.
- Automated aesthetic, identity, style, interaction and theme visual verification.
- Real diffusion checkpoint training or LoRA fine-tuning.
- Full Image Toolbox / RealSR / SuperImage source-level integration.
- Galaxy S26 Ultra device validation, release signing and installed APK.

## Phone use during development

Install Nightmare Mobile and enable its authenticated API from Settings -> Advanced -> API. Asterion Art contacts 127.0.0.1:8820; enter the API token in the Studio screen. Select or create sheets, choose a mode, and request generation. Installed Nightmare models and recipes are required. No powerful PC is required to run cloud Android CI; model conversions have separate hardware needs.

For animation, several user-selected stills can each initiate real video generation and produce stitched approximate segments. The next selected frame is not guaranteed as the preceding segment endpoint. Do not claim exact keyframe interpolation.

The sheet builder does not impose a thematic-category filter. Model limitations, legal constraints and third-party licenses still apply.

## Third-party source rights

Nightmare Mobile's inference backend derives from Local Dream and carries CC BY-NC 4.0 terms; the Qualcomm runtime and each model have independent licensing. This branch does not vendor those binaries.

Production completion gates: CI success; generation/visual reference conditioning on device; secure Core bridge; fully native app engine; accurate grading; animation endpoint fidelity; enhancement evaluation; model provenance; Android release signing and update validation.

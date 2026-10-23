# Asterion Art — Integration Branch (2026-10-23)

**This is an in-progress, separately installed Android application** on branch `asterion-art-unified-20261023`, NOT a release. Source intentionally reuses the existing Compiler Android project on a side branch, preserving the existing `main` branch and the prior LoRA PR. `applicationId=com.asterion.art` prevents overwriting existing Compiler.

## Implemented in source

- Four-sheet reference project data (character(s), art style, theme, optional elements), output modes anime/realistic/cosplay/animation.
- Android OpenDocument reference and video keyframe selection with persistent URI grants.
- Reference text extraction using local ML Kit OCR (not visual conditioning).
- Authenticated, loopback-only Nightmare API adapter: probe /info, /models, /flows, /flows/{id}, execute /run and fetch /results MP4/PNG. Requires another instance of Nightmare Mobile running its API on 127.0.0.1:8820 with a manually supplied token.
- Choose an installed image model with mode-appropriate labels, otherwise report missing model. No automatic download or guess of checkpoint.
- Wan image-to-video per anchor frame, with explicit segment duration; multi-frame approximate mode creates individual model-inferred segments and remuxes compatible MP4 tracks.
- **No claim of exact first-and-last-frame inference**. Strict multi-anchor mode blocks until a suitable two-endpoint model backend exists; approximate sequence never passes the target image as an end-frame condition.
- Manual rating history, rudimentary actual pixel-derived exposure/clarity scoring, scoring uncertainty, rejection gates and an unconnected online preference-weights learner.
- NoWorld staging export to Downloads/AsterionArt/NoWorld (MP4 + `.nwasset.json` with art_acceptance=NOT_APPROVED, device_validation=NOT_TESTED).
- Android CI regression tests and build workflow.

## Not implemented / not verified

- No direct native engine fusion from Local Dream or Nightmare; initial backend is the authenticated external-app API.
- No Asterion Core cross-app IPC, automatic reference retrieval, secure Core saving, or authoritative library sync.
- No direct vision conditioning with the character/style/theme/element images, only initial OCR descriptions. The app explicitly warns about this; fine-grained identity fidelity is not yet reliable.
- No proven automated character/style/anatomy grading, image correction, multi-candidate selection, user training feedback wired into the model selector, auto-upscaling or actual model fine-tuning.
- No genuine two-endpoint video interpolation. NoWorld staging is not an actual NoWorld project import.
- S26 Ultra runtime tests, release signing and in-place update signature are not yet verified.

## Device setup for current integration

1. Build the Art Android debug APK via GitHub Actions; install it separately from Compiler.
2. Install Nightmare Mobile, download a compatible NPU image/video model, and enable its Settings → API. Copy its token into Asterion Art's backend field.
3. Choose the sheets; for stills, current OCR mode will compile their **text** into a prompt. For animation, choose one source frame to generate a Wan clip, or several frames with the explicitly warned approximate option.
4. Check results and submit a 0–10 human rating. For animation, export an unapproved MP4 + metadata into NoWorld staging.

Do **not** use the app as an unattended trusted pipeline until the Core bridge, model capability checks, reference conditioning, and image QA are validated.

## Upstream dependencies

- Local Dream `xororz/local-dream`: CC BY-NC 4.0.
- Nightmare Mobile `AbrahamPaulJ/nightmare-mobile`: CC BY-NC 4.0.
- Existing Asterion compiler source retained on isolated branch. Credits and all license requirements must be respected if code is incorporated into a redistributed binary.

The branch currently calls Nightmare's public local API and does not copy its native source or model assets into this repository.

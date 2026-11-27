# Asterion Art ↔ Asterion Control diagnostics integration

Version: 0.2.10  
Protocol: Asterion partner bridge v1  
Authority: `com.asterion.art.asterion.bridge`

## Purpose

Asterion Control can inspect the actual evidence produced by Asterion Art when a generation is poor or fails. The integration is deliberately read-only. It does not replace either application's engine and does not allow Control to start, stop, edit, or retry an Art job.

The most important evidence is the exact native prompt passed to the image-generation model, the separate prompting-model plan, and the native per-stage inputs. These are already persisted in Art's app-private generation checkpoints. The provider exposes them without copying or exporting generated image bytes.

## Owner approvals

1. In Asterion Art, open Testing → Asterion Control · Diagnostics and enable sharing.
2. Enabling pins the currently installed Asterion Control APK signing certificate in Art. A later signer change is rejected until the owner disables and re-enables sharing locally.
3. In Asterion Control, locally approve the installed Asterion Art package/signing identity. Control's approval is stored on-device and cannot be granted through the GitHub queue.
4. Disable the Art switch at any time to revoke access immediately.

Both applications must be installed on the same Android device. The Control package must be `com.asterion.control`.

## Available read-only actions

- `diagnostics.snapshot`: Art build/device metadata, recent task outcomes and recent generation-job IDs.
- `diagnostics.job`: one job's mode, prompt lengths, settings, reference counts, image-model identity, prompting-model identity/path, Character Creation state, visual-audit status, stage count and available evidence files.
- `diagnostics.read`: paged text from one allowlisted artifact for a known job ID.
- `diagnostics.bridge-events`: paged, metadata-only history of provider calls, including method/action, request ID, caller package, outcome, elapsed time, response size and exception class. Request payloads, prompt text, image data and credentials are never recorded.

Allowed artifacts:

- `native-prompt-trace`: exact model ID and prompt/negative prompt for every compiled native stage.
- `brain-plan`: prompting model, domain, complete source authority, synthesis, final positive/negative prompt and verification metadata.
- `native-stage-inputs`: actual per-candidate/per-stage inputs sent to the native image engine.
- `last-failure`: saved generation failure details and native runtime tail.
- `model-load-ms`: native model load timing.
- `character-capsules`: frozen character-design evidence where applicable.
- `character-plan`: the separate Character Creation prompting-model output, frozen identities, model ID and failure/progress state.
- `character-telemetry`: Character Creation model-load, first-token, streaming and stage timings.
- `character-visual-audit`: local multimodal findings and concerns; source image URIs are redacted and the report is explicitly not an automated certification.

Long artifacts and bridge-event history are paged in bounded 3,000-character chunks. Job IDs and artifact names are validated against fixed allowlists; arbitrary paths are never accepted. The event journal retains at most 128 recent records and is stored in Art app-private storage.

## Security and data boundary

- Only the installed `com.asterion.control` UID and the locally pinned Control signer may call the provider.
- Control must independently approve Art's signing identity on-device.
- No network listener, GitHub token, Control bearer token, image bytes, model weights, arbitrary filesystem access, mutation, generation or editing operation is exposed.
- All diagnostic files remain inside Art's private app data directory.
- Prompt text and saved failure reports are owner-authored/private content; sharing is disabled until the owner enables it.
- Bridge-event records contain call metadata only; request and response bodies are excluded.

## Runtime compatibility note

Live read-only verification on 2026-11-27 isolated the blocker before Android provider invocation:\n\n- Existing Windows worker health returned `complete / ok:true` and reported Control worker version `0.8.0`.\n- Existing phone status returned `complete / ok:true`: Samsung SM-S948B, Android 17, Control app `0.9.38`, paired and reachable, authenticated native WSS connected over Wi-Fi.\n- `phone_app_bridges` returned `ok:false`, `status:error`, `elapsed_ms:9`, `error: "Unsupported operation"`. The failure is the existing Windows relay/command executor rejecting the operation before it reaches Art on Android; it is not an Art provider authorization failure.\n- The current Control source branch `work/control-native-ai-reconstruction-20261126` already contains the `phone_app_bridges`, `phone_app_capabilities`, `phone_app_invoke` and `phone_app_operation` mappings using the existing WSS-first transport. The installed/runtime worker is therefore behind the source branch. Control\u2019s own 2026-11-26 progress report records that in-place Windows deployment was blocked by execution safety checks and no successful deployment receipt exists.\n- At the time of the check, the phone reported 5% battery and severe thermal status. No screen interaction, local owner approval, app installation or provider call was attempted.\n\n**Next required operation:** deploy the already-reviewed Control relay source update into the existing Windows worker using its approved deployment path, then let the existing supervisor reload it at a safe idle checkpoint. Do not replace the worker, re-pair the phone, revive a retired gateway, or bypass deployment safety checks. After that, install the original-key-signed Art 0.2.10 update, enable sharing in Art, locally approve Art in Control, and confirm that discovery and the first `diagnostics.snapshot`/`diagnostics.job`/`diagnostics.read` calls succeed. The new Art-side `diagnostics.bridge-events` journal will then expose provider-side request outcomes and latency; it cannot record a command rejected before Android receives it. No image-generation model or generation pipeline is changed by this diagnostics integration.

# Asterion Art ↔ Asterion Control diagnostics integration

Version: 0.2.10  
Protocol: Asterion partner bridge v1  
Authority: \`com.asterion.art.asterion.bridge\`

## Purpose

Asterion Control can inspect the actual evidence produced by Asterion Art when a generation is poor or fails. The integration is deliberately read-only. It does not replace either application's engine and does not allow Control to start, stop, edit, or retry an Art job.

The most important evidence is the exact native prompt passed to the image-generation model, the separate prompting-model plan, and the native per-stage inputs. These are already persisted in Art's app-private generation checkpoints. The provider exposes them without copying or exporting generated image bytes.

## Owner approvals

1. In Asterion Art, open Testing → Asterion Control · Diagnostics and enable sharing.
2. Enabling pins the currently installed Asterion Control APK signing certificate in Art. A later signer change is rejected until the owner disables and re-enables sharing locally.
3. In Asterion Control, locally approve the installed Asterion Art package/signing identity. Control's approval is stored on-device and cannot be granted through the GitHub queue.
4. Disable the Art switch at any time to revoke access immediately.

Both applications must be installed on the same Android device. The Control package must be \`com.asterion.control\`.

## Available read-only actions

- \`diagnostics.snapshot\`: Art build/device metadata, recent task outcomes and recent generation-job IDs.
- \`diagnostics.job\`: one job's mode, source prompt text, settings, reference counts, image-model identity, prompting-model identity, prompt lengths, visual-semantic verification flag, stage count and available evidence files.
- \`diagnostics.read\`: paged text from one allowlisted artifact for a known job ID.

Allowed artifacts:

- \`native-prompt-trace\`: exact model ID and prompt/negative prompt for every compiled native stage.
- \`brain-plan\`: prompting model, domain, complete source authority, synthesis, final positive/negative prompt and verification metadata.
- \`native-stage-inputs\`: actual per-candidate/per-stage inputs sent to the native image engine.
- \`last-failure\`: saved generation failure details and native runtime tail.
- \`model-load-ms\`: native model load timing.
- \`character-capsules\`: frozen character-design evidence where applicable.

Long artifacts are paged in bounded 3,000-character chunks. Job IDs and artifact names are validated against fixed allowlists; arbitrary paths are never accepted.

## Security and data boundary

- Only the installed \`com.asterion.control\` UID and the locally pinned Control signer may call the provider.
- Control must independently approve Art's signing identity on-device.
- No network listener, GitHub token, Control bearer token, image bytes, model weights, arbitrary filesystem access, mutation, generation or editing operation is exposed.
- All diagnostic files remain inside Art's private app data directory.
- Prompt text and saved failure reports are owner-authored/private content; sharing is disabled until the owner enables it.

## Runtime compatibility note

The current connected Control Windows relay returned \`Unsupported operation\` for the \`phone_app_bridges\` discovery request. The Control source branch contains the Asterion v1 partner routes, but the currently running relay has not yet exposed them. Therefore this Art-side provider can be built and source-tested now, but end-to-end discovery/invocation requires the existing Control relay update to be deployed and a local owner approval on the phone. No replacement bridge is required.

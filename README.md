# Asterion Art — clean-slate rebuild

This branch is the **new source-of-truth implementation branch**, not a working APK. All legacy application/engine/Gradle implementations have intentionally been removed from this branch. This is a genuine rebuild rather than continued patching of the old prompt-centric architecture.

## Preservation / provenance

- Legacy engineering history is preserved without rewriting commits at `archive/asterion-art-legacy-20261127` (legacy head: `4691df4f24a01d0115973f1fbbcf011e1f9621de`).
- The old `main` and former integration branch were not modified by the cleanup.
- The full authoritative owner architecture and approved corrections are preserved under `docs/architecture`, `docs/production`, `docs/owner_recovered`.
- All 14 tracked original methodology/taxonomy source files were moved **byte-for-byte as existing Git blobs** from `app/src/main/assets/methodology` to `specifications/original-methodologies`. These are **development/training specifications, NEVER runtime instructions to a general-purpose chatbot**.
- Previous technical proposals, defective app code, CI scripts and dead-end experiments remain retrievable from the preserved legacy branch, not from this rebuild.
- No existing local models, device installations, private source images, signing secrets or other user data were deleted.

## Binding product definition

Asterion Art is a **genuine Android-local creative image and video application**. Implement full independent Artwork, Sheet, Originals and video workflows. Artwork alone has its owner-defined five phases, with multiple candidates and one continuation at each stage. All Originals use their independent original governing methodologies. The system needs structurally encoded mode-native method execution and trained visual/creative skills—NOT runtime methodology interpretation, generic chatbot prompts or deterministic creative text fallback.

- Three genuinely tailored image generators: **Anime, Cosplay, Realistic**, subject to verified phone-native support.
- Original and reference-aware visual conditioning, genuinely integrated Character/Theme/Art Style/Element sheets, coherent character identities, source and field provenance, selection and controlled edits.
- Both individual video clips and multi-shot projects, with audio, dialogue, music and lip sync.
- Native on-device generation once permitted model weights are acquired; automatic quality optimization, enhancement, robust unattended background operation, checkpoints/restart, user grading and validated learning.
- Reuse existing authenticated Asterion Control bridge, Core and Downloader ecosystem; no mandatory novel third-party server, paid compute or unverified free GPU.
- Quality accepted on actual original modes and representative image/video outputs—not build success, prompt quality, synthetic previews or sphere tests.

## State

**Repository cleaned; implementation not started in this branch.** No production model, signed APK, on-device generation or quality acceptance is claimed. Development should proceed only after consolidating the owner's one-time outstanding decisions, without reasking settled specifications. Re-establish CI on the newly implemented project, test actual native inference, verify original signing identity and conduct on-device quality acceptance.

See `docs/rebuild/CLEANUP_RECORD_20261127.md` and `specifications/README.md`.

# ASTERION ART — Owner-approved durability and methodology authority (2026-10-30)

## Decisions explicitly approved

1. Complete requirements already specified by the owner's 2026-10-29 master document and uploaded original methodologies. Preserve existing working code, models, assets, saved generations and signing identity. Work only on a separate development branch; **do not merge or install without further owner approval**.
2. All unfinished authoring requests must persist across app navigation, Activity termination and process relaunch. Save selected sheets, local image/document URI grants, concepts, directions, image-mode choices, panel assignments and video keyframe order. Draft persistence is **not** an approval to start execution.
3. After the owner selects **Start**, actual still-image generation continues in an Android foreground execution component independent of the UI. Native jobs must be serialized due to shared memory/NPU resources. Persist durable queue entries and the existing checkpoints; recover incomplete jobs after service/process interruption where Android permits.
4. The owner cannot presently perform device testing. Do not claim device acceptance or release completion from CI alone.

## Decisions not made or authorized

1. The owner has said certain Original methodology restrictions were accommodations to ChatGPT limitations. **Which individual rules and sections may be removed has NOT been specified.** Keep original methodology source untouched and do not remove, rewrite or weaken rules pending explicit section-by-section authorization.
2. Enhancement chaining, model replacement/fusion, new model routes, video interpolation or conditioning strategies beyond the approved canonical design, and output/ratio deviations require owner approval when an original authority does not fix the choice.
3. Do not automatically clear drafts when a job starts or finishes. Deletion must be explicit, never a side effect of processing.
4. Preserve the existing original-key-only Android signing policy and do not uninstall, clear app data, migrate model locations destructively, or overwrite user models.

## Implementation in this branch

- `RequestDraftStore`: synchronous private state persistence, per-workflow keys, no implicit launch/deletion.
- Artwork, Sheet Builder, per-Original methodology editors, Character Creation Phase 1 and Video Workspace restore unfinished selections on the next visit. Android OpenDocument URI read grants remain preserved by the existing picker paths.
- `ForegroundRenderQueueStore`: durable FIFO for native image work.
- `GenerationForegroundService`: executes submitted still-image work serially in foreground, survives UI exit, drains pending entries, retains interruption recoverability and refreshes its notification.
- `GenerationWorker`: legacy WorkManager fallback remains for foreground startup denial; this must be verified on target Android.
- On app reopen, orphaned foreground queued work is revived without asking the owner to reenter the request.

## Validation status

Automated Android CI and JVM tests required. Real model execution, device restart behavior, Android service time limits, long-running generation, image quality and release signing are not established by this source change.

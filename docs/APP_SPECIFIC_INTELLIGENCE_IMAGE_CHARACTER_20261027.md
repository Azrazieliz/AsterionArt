# Asterion Art — approved priorities 1–3 (source implementation, 2026-10-27)

Owner scope: (1) app-specific visual intelligence/self-learning, (2) improved image inference, (3) approved Character Creation Phase 1 end-to-end. No changes to other Originals or rich canonical taxonomy. EUR 0; GitHub only; NO builds until separately authorized.

## (1) Visual intelligence / corrected self-learning

- **Owner-controlled model setup:** AI weights are NOT bundled in the APK. The owner installs/loads selected Qwen3 reasoning, Qwen3-VL vision + projector, and compatible image-generation model archives from the application's Models area on first use. Existing installers verify model signatures/checksums where the catalogue defines them. Model loading is the sole permitted network exception; image/video generation must run locally. No model checkpoint is silently selected or required to be embedded.
- The pinned `llama.cpp` v0.7.0 `mtmd` C API is compiled alongside the existing JNI runtime. `asterion-vision` is an independent Android-native CPU inference engine and consumes actual RGB pixels. It never sends images to a remote provider.
- User-approved Advanced Grading remains the only labeled training source. Per-image-region numeric grading trains a small local score model by mode/dimension, with no false claims of semantic understanding. Keep/Reject are not used.
- A new multilayer Qwen3-VL visual audit inspects actual generated pixels and each frozen character's anatomy, physical concept fusion, view identity and illustrated 2D grammar separately, preserving all frozen authority across pages. It distinguishes visible evidence, concerns, unknowns and inconclusive responses. It never issues an invented grade or overwrites the frozen design.
- Generated output SHA-256 is checked against the saved generation provenance before analysis.
- Machine observations appear beside Grading. User can explicitly review them as an UNSAVED grading draft. Only actual Save adds a human-approved training example.
- Explicit Preserve and Correct text from the grade can launch masked local inpainting for the graded region; original pixels outside the mask remain preserved. The revision job carries exact correction, preservation and original grade ID.
- A free/optional Qwen3 text LoRA training script exists separately; no trained adapters or newly trained image VLM are claimed.

## (2) Native image inference improvements

- Character Creation renders each independently frozen character as its own three-view clinical reference instead of asking one short-context image encoder to attend to five different character payloads at once.
- Native image tokenizer checks must verify ONE complete owner-approved character specification per generation. The incompatible prompt-sliced sequential repaint mechanism was removed on the recovery branch; true image-encoder extension remains unimplemented. This is NOT an expanded simultaneous diffusion text encoder.
- Completed identity triptychs are saved in per-character checkpoints. Five are assembled into a single 16:9 continuous white analytical sheet without artificial bars or surrounding captions. Nonwhite tile backgrounds fail rather than silently becoming decorated panels.
- Rendered sheets and per-identity records remain traceable to frozen specifications, and the vision model can be scheduled only AFTER the NPU image engine releases its memory.
- The 32K Qwen reasoning decoder is fixed to expose native token counting and explicit conversation reset. Both prompt overflow and generation-time context exhaustion stop cleanly instead of silently shifting away earlier methodology authority.

## (3) Character Creation Phase 1

- 1–5 sheets selected by user; five original anatomical identities per sheet, all independently authored and frozen before the user authorizes any image generation.
- Full physical pairwise concept fusion, higher-order fusion for three-plus concepts, independent female character authorship, clinical unclothed anatomy, distinct design, aesthetic quality, specific hand-illustrated 2D anime/manhwa language and true-front/exact-profile/direct-rear references remain mandatory.
- Character-specific frozen prompts remain available for optional inspection; Validate & Generate directly authorizes rendering without requiring an extra approval dialogue.
- Design Direction, sub-concepts and former character outfit/weapon/assembly phases remain absent. Outfit and Weapon are separate modes.

## Integrity and limitations

- All work on this research branch is source-only: no APK assembled, Kotlin/NDK compilation verified, Android installation demonstrated, or real generation/device inference tested. Assertions of functional/visual quality are premature.
- Reasoning weights, Qwen3-VL language/projector weights and converted QNN image-generation checkpoints are owner-selected and loaded **from within the app**, not automatically extracted from APK assets. The app's model selectors and import paths exist; completion of the first-run setup workflow has not been device-tested.
- Qwen3-VL native `mtmd` remains experimental, and inference reliability, latency, memory requirements and device compatibility are unverified.
- The local trained image-feature scoring heads are not semantic vision learning or LoRA fine-tuning. Only explicitly approved Advanced Grading is an authoritative label; the training/evaluation loop for improved generative quality is incomplete.
- The unapproved sequential prompt-slicing path was removed: it never expanded simultaneous diffusion text context. The compatibility of native model resolutions, image input semantics, true exact-profile views, and cross-view consistency is unverified.
- Artwork generation, Sheet Creation, other Originals, video, editing, library, and Core integration all have separate implementation/verification gaps; see `docs/IMPLEMENTATION_STATUS.md` for the cross-application status. Video currently needs a separate phone-local Nightmare-compatible process; no self-contained integrated video backend is proven.
- The old automatically embedded model packaging and source-only model-packaging workflows have been removed because they contradicted the owner's first-run installation decision. No assumption of public distribution is made.

## Next engineering and verification gates (owner authorization required before APK builds)

1. Confirm that manual setup in the app loads reasoning, visual and QNN image checkpoints, with correct routing and clear failure states; do not require user-side shell commands or external app installations.
2. Verify Kotlin/CMake/JNI compilation and installation only when the owner permits builds.
3. Generate and inspect representative reference sheets, artworks, each separate Original, Character Creation five-character/15-view sheets, targeted image edits, and video jobs against their specifications.
4. Verify actual semantic visual observation, independent user-confirmed grading, masked corrections and measurable improvement to later generations.
5. Verify all creative features are device-local after the owner finishes loading the models. Remove unintended external-service dependencies rather than conflating local loopback with an integrated native engine.
6. Reconcile source code with `docs/IMPLEMENTATION_STATUS.md` using tested evidence rather than updating a green status based on source presence.

## Preserved source-only corrections

- Character Creation detects the reasoning model after the owner installs it without requiring a screen restart.
- Validate & Generate directly authorizes frozen Character Creation images. Full prompt inspection is optional.
- The character-specific upright triptych-resolution preference remains intact on fallback image routes; the final output remains five identities per 16:9 sheet.
- Other Originals' methodology definitions and the existing full taxonomy were not modified.

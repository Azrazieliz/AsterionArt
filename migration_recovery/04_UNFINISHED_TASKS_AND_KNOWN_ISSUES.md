# Accurate implementation status, failures and remaining work

## Delivery/verification matrix
| Area | Source/CI status | Actual user-device validation |
| --- | --- | --- |
| Kotlin Compose UI / navigation | Present; Android CI green | Premium usability/flows need on-phone review |
| Four normalized sheet layouts & editor | Present, source-driven and tested partly | Real boards, layout fidelity and machine authority incomplete/unverified |
| Character Creation v3 455 concepts + staged forms | Present with 23 source-level unit tests in most recent APK | Need actual 15-individual x 3-sheet render/QA and freeze transition tests |
| QNN native executable packaged | Pinned upstream library packaging verified by CI | Qualcomm NPU inference on S26 Ultra NOT independently verified |
| QNN import | format classification, markerless ZIP, progress and original-format separation committed | User's exact failed ZIP and 6–7GB restart/cancel behavior NOT re-tested |
| 5 real image passes & multi-reference | Worker/source contract present | No verified full image with user model |
| Grading/preference learning | CPU technical quality and user ratings code | Subjective identity/style fidelity unverified |
| Inpaint + outside-mask pixel lock | Code present | Device test and very small mask precision unverified |
| Automatic enhancement | RealSR CLI / toolbox code added at later main commits | On-device model/executable availability and result test outstanding |
| SuperImage Pro | Android SEND handoff to installed Pro app; public package observed | Exact proprietary engine NOT embedded/copied |
| Video | Nightmare-compatible Wan local API, segments/provenance | Native video backend/NoWorld export end to end NOT verified |
| Core bridge | Art code present, Core provider on `asterion-art-sheet-bridge-20261023` | Matching-signature cross-app read/write unresolved |
| Fusion B | Source SHA and 685 tensor shapes verified; FP16-BF16 synthetic test passed | NO 4 GB fused file, benchmarks, or QNN conversion |
| Stable signed update | Debug APK builds successful | Signed user-compatible release NOT established; risk of data loss |

## Exact known errors, bugs and past failed approaches
1. **24 Oct screenshots:** `MODEL IMPORT FAILED — Missing NPU model type marker (ANIMA/SDXL/etc.)`; second `Model installation cancelled`. Cause likely hardcoded marker/format assumptions, but precise file and cancellation trigger NOT established. Later commits changed import logic, not confirmed device-fixed.
2. **CI in native integration (23/24 Oct):** Android compiled failures `40043969749`, `40044005696` on bridge/engine integration followed by green `40044388000` and `40044831118`; test sequence and source SHA list in `ART_CI_RUNS_20261026.json`.
3. **UI/methodology changes (24 Oct):** transient Android failures in `40080759978`, `40080848581`, `40080972718`, `40081082336`, `40081189710`, `40081231580`, `40097946566`, succeeded on later commits. Do not treat failed intermediate commits as current HEAD, but retain their links for diagnosis.
4. **Incorrect design:** one huge generic Create tab with unrelated controls; initial flat Home, incorrect required Character and Theme Sheets in Character Creation or Style Correction; default button layout not methodology-aware. User explicitly rejected this, and later commits addressed it partially; verify on device.
5. **Overbroad prompt/AI claims:** using text generation to command fine-grained hand/eye/horn micro fixes; user rejects as default. Image model must infer coherent detail via source authority and five-pass pipeline. No invented source facts.
6. **Fusion B:** equal five-model fusion rejected; a four-way direct merge across SDXL and Anima impossible. 685 tensor shape matching alone is not evidence of quality, distribution rights, valid NPU conversion or user preference.
7. **APK artifact failures/links:** previously missing or inaccessible artifacts in conversations; links are valid only if verified. Preserve GitHub run/artifact IDs and SHA instead of inventing sandbox links.
8. **Unreliable model/data upgrade:** debug APK signer changes. NEVER advise uninstalling and reimporting multi-GB model collections absent robust stable signing/migration.
9. **ChatGPT Android UI crashes / node-not-found corruption:** user reported repeated conversation regressions and inaccessible Library files. These are platform failures, separate from Art code, but motivate export preservation. Do not assume chat history will remain accessible.
10. **Safety/legal integration boundary:** reading proprietary app package metadata or launching ACTION_SEND does not permit blindly embedding SuperImage Pro internals or redistributing unsigned commercial app binaries. Keep MIT RealSR and user-installed Pro distinct.

## Priority continuation backlog
P0. **Protect app data and signing identity.** Compare currently installed `com.asterion.art` cert with planned release; establish reproducible signing and safe upgrade path without model copy/clear.
P0. **Reproduce importer failure through ADB on a small compatible QNN ZIP** and inspect source model package structure; check 7GB SAF streaming, cancel/resume, durable progress, no duplicate/partial directory, uncompressed-size guards.
P0. **Verify S26 Ultra actual NPU model loading and first image**, then five-pass conditioned generation, RAM/thermal/thread/background survival. Provide logs/visual result proof, not CI-only claims.
P0. **Validate Character Creation v3 on device**: 455 selectable concept taxonomy, exactly three 16:9 sheets each with five bodies and three physically correct orthographic views, user phases/body/outfit/weapon freezes.
P1. **Validate Art Style Correction** two exact inputs and lossless content preservation; improve selection from Core without typing.
P1. **Improve top-level UI:** Home, Image/Animation gateway, Artwork/Defaults/Sheets, interactive previews and clickable templates; no generic combined form.
P1. **Test progress/cancel/resume and final results** for stills, model import and video; report indeterminate progress truthfully.
P1. **Enhancement:** test MIT RealSR in app and Image Toolbox, provide optional public-share to installed SuperImage Pro; benchmark anime linework preservation and tile memory.
P1. **Core/NoWorld:** inspect Core bridge branch merge, signing permission, zero-copy SAF URI reference and result streaming; test data provenance & gallery sync.
P2. **Fusion B full private merge** after writable private Hugging Face destination/authorization and checks. Benchmark against original Rin on user's sheets; only deploy if improved; perform verified QNN conversion.
P2. **Other Illustrious models**: user wants to test multiple variants including Hassaku v3.4; original SDXL checkpoint must be converted separately to compatible phone runtime; maintain rankings based on measured output.
P2. **Standalone video backend**: replace external Nightmare API when feasible; preserve explicit I2V and segment vs genuine jointly-conditioned keyframes.

## Definition of done
A working update-compatible signed Asterion Art APK installs on the current phone **without losing imported models**, loads a proven QNN model, processes one real methodology-correct task with all five valid image passes, presents finished output after switching away, provides actual progress/cancellation, correctly handles native enhancement, then confirms Core round-trip. Source tests alone are insufficient.

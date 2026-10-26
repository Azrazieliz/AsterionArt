# Detailed recoverable conversation and implementation chronology

This record is reconstructed from accessible user turns, previous assistant handoffs, Git history and CI logs. It is NOT a verbatim transcript of every system/tool/assistant message; inaccessible historical content must never be invented. Exact commit and CI lists live in adjacent machine inventories.

## Before 23 October 2026
- Project began as AsterionCompiler, a prompt/graph compiler implementing methodology files, pass-specific prompt logic, sheet authority and future Asterion Core integrations. Older code in `archive-pre-asterion-art-20261023` and `legacy-asterion-compiler-20261023`, HEAD `db68e11c411ae120b7d08e5e8aa5914608a7a404`.
- User preferred model use on a phone and asked to integrate the most useful Local Dream, Nightmare, Image Toolbox, RealSR and SuperImage capabilities into a single Asterion Art app.
- Original ZIP of methodologies contains nested ODT archive, Character Creation v1.5/v2/v3, Prompt Writing, Pose, four Sheet families, Art Style Correction, Outfit/Weapon/Creature/Mutation/Transformation/AU, Illustration Analysis and excluded CBZ translation. Versioned primary documents must not be flattened into vague modes.

## 23 October — product corrections, architecture and rebuild
- User asked for Nightmare-like video inside Art; preferred Rin Flanime, MiaoMiao, Hassaku XL, NoobAI XL v11 and dropped Animayume; sought persistent background generation and exact multi-image editing.
- User explicitly set anime quality ranking **Rin > MiaoMiao > Hassaku > NoobAI**. Advanced controls screenshot demonstrated scheduler/aspect/steps/CFG/batch/denoise/seed *types*, not approved numeric defaults. Keep five-pass methodology and SuperImage Pro preference; automatic hands-off enhancement by default.
- User clarified no LLM-style tiny instructions as primary edit workflow; structured visual generative passes first.
- User asked sheet-specific normalized production; methodology ZIP becomes default creation modes. Keep Character/Style/Theme/Element Sheets and Character Creation, Art Style Correction, Outfit, Weapon, Transformation, Mutation, Creature, AU; make Pose/Prompt internal. Exclude translation/retypesetting app entirely.
- User supplied character boards for Saber Artoria and Lancer Artoria, plus Francesca reference. Lancer/Saber weapon module is wildcard optional; must adapt to identity; all UI click-driven except Core search.
- User authorized discarding then-current main repo content, BEGIN implementation. An archive branch preserves old code. Commit `6b2e7ebec0d490d0ee2f01d6286f46c1c14e26de` builds new Art shell with Jetpack Compose and Gradle, subsequently fixed compilation, WorkManager sheet rendering, scoring, 5-pass generation adapters, model library, masked edit, Nightmare-compatible video and Core bridge.
- User requested a downloadable app, original CI debug APK released as an artifact; no release-signed installation proof.
- Fusion B investigation established 685 identical tensor keys/shapes from Rin Anima v1.4 and MiaoMiao v1.6, **different dtypes FP16 vs BF16**. Streaming numerical conversion/merge and synthetic tests are in `fusion/`. Source preflight GitHub workflows succeeded, but private multi-GB checkpoint was not merged or QNN converted. Hassaku XL Illustrious v3.4 is a separate SDXL source.

## 24 October — real user device feedback and iterative fixes
- ADB accessed S26 Ultra via authorized Windows laptop/Remote Desktop Commander. Observed installed packages: `com.zhenxiang.superimage.pro` v3.22.0, `ru.tech.imageresizershrinker` v4.2.0-rc07, `com.tumuyan.ncnn.realsr` v1.13.2; SEND image/png intent handlers responded. Do not equate this to lawful/proven embedding of proprietary SuperImage Pro.
- Screenshot showed import errors `Missing NPU model type marker (ANIMA/SDXL/etc.)` and `Model installation cancelled`. Diagnosis: importer assumed particular ZIP package layout/marker; raw safetensors not QNN; exact failing file unconfirmed.
- User required progress bars for import/image/animation, useful Home, click-through Create→Image/Animation→Artwork/Defaults/Sheets, mode-specific methodology input contracts and model-type tags. Reject generic dependencies (Character Creation is new identity, needs no Character/Theme Sheet; Style Correction only source illustration+target style sheet).
- Commits `919db6b4`, `5373c992`, `7d5725cc`, `c106af5a`, `4772d8b2`, `b81faf9c`, `71c1f6e5`, `76203c27`, `da450f2d`, `f538bb54`, `3127b813`, `9661e446`, `07f81344` reflect progress reporting, model classification, v3 concepts (455), new navigation, import classification and contracts; many intermediate CI failures were resolved in succeeding commits. Current runtime still requires device verification.
- An earlier 24 October Library handoff recorded three different ephemeral debug-signing certificate SHA-256 fingerprints, breaking update-in-place. New signed release should reuse user's signing key; NEVER silently uninstall old APK, reset app data or reimport 6–7 GB models.
- Subsequent 24 October commits after older handoff: `8d636132` enforce methodology-specific staged flows; `fa54886c` fix style redraw and AU direction; `3995f068` integrate MIT RealSR; **latest `88206baa`** adds in-app image toolbox and automatic neural SR after one-shot methodology. These later commits supersede older handoff's `92285d2`/ `07f8134` but do not erase old decisions.
- Latest CI `40099170762` success at source HEAD `88206baa`: 23 unit tests, no failures in attached latest artifact. Not a device acceptance test, and signing mismatch remains possible.

## 26 October — migration and preservation
- User reports conversation corruption and requests complete recovery. GitHub Art head `88206baa...`, Core head `7d7ab331...`; pre-existing migration source snapshots of both repositories available from successful 26 October workflows `40335416298` (Art) and `40335426421` (Core).
- This branch records Art and Core exact commit histories, current source file-tree SHAs, CI and a next-chat prompt. Local full recovery ZIP preserves supplied methodology/design files, source archives, available build artifacts, reference screenshots/video and previous handoffs. Public GitHub intentionally does not host potentially personal reference media or hundreds of MB of APKs.

## Reliability/failure lessons
1. Do not equate green CI with runtime NPU inference, SuperImage integration, video export or Core syncing.
2. Do not supply fabricated sandbox download links. Before linking an APK or ZIP, verify the exact container path and package contents.
3. Avoid claiming read/write connector permissions without checking: private Hugging Face merge output required writable private repository; none was verified.
4. An app install may reject updates because debug APKs differ in signing identity; first inspect installed signing before touching phone data.
5. Do not repeat canceled work or revert a later working branch to older chat/handoff state. Latest `main` and exact tree are baseline.
6. ChatGPT conversation corruption itself is unrelated to code; user explicitly requests external detailed handoffs to prevent losing project details and work.

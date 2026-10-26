# Private assets and external artifact inventory

The primary downloadable RECOVERY ZIP, generated in this conversation, includes the original methodology, source archives and private reference media below. Two large APK artifact ZIPs are preserved as separate, directly linked companion downloads (their hashes/IDs are in the recovery manifest). They are **not copied into this public GitHub repository**, to avoid unintended publication and GitHub's 100 MiB per-file limit.

### Original methodology / project documents
- `Methodology.zip` — authoritative supplied archive, 40 entries including nested `Archive odt.zip` with historical .odt versions. Includes Character Creation v3 `Concepts.md`, `Design_Direction.md`, `characters_creation.md`; v2 and v1.5; four sheet methodologies; Prompt Writing/Pose; other creation modes; historical Translation module that must stay excluded from Art UI.
- `asterion_art_architecture_v1.zip` + associated unpacked contracts: early schema, three form variant fixtures, Kotlin normalized sheet model, tests and workflow registry.
- Library handoff `ASTERION_ART_CONTINUATION_2026-10-24.md` and `ASTERION_ART_NEXT_CHAT_PROMPT.txt`, plus ecosystem links `Automation_Ecosystem_Initialization_2026-10-23.md`.
- `Automation_Architecture_Pack(1).zip`, 11 docs, cross-project architecture link. **Do not convert those concepts into new mandatory Art features.**

### User imagery and recordings
- `Artoria Pendragon Lancer.png`; `Artoria%20-%20Fate.webp` (Saber); `Francesca Prelati Character Reference Sheet(1).png`, submitted comparison examples.
- Device screenshot of model import failure and assorted mounted .png/.jpg files; original filename may be opaque. The private archive inventories each via SHA256, filename, bytes and category. Avoid identifying individuals from user photos.
- A mounted MP4 recorded reference is included if present. Its video content may not have been fully inspected; do not claim detailed scene-specific findings without actually reviewing.

### Binaries
- GitHub source archive Art: run `40335416298`, source artifact `12656907263`, commit `1bdff7823a6afbb0e37c364af15092bf4e5c25f3`. User-facing backup additionally includes a newer separate migration-branch source archive.
- Core source archive: run `40335426421`, artifact `12657226213`, archived Core source at immutable migration commit; active Core `main` `7d7ab3318cfdd829b28faa301568cedc8b100961`.
- Last Android source main build `40099170762`: latest debug APK artifact `12562172283`, Android CI success, 23 JVM unit tests; artifact ZIP is a **separate companion download**, not nested inside the primary ZIP. Debug signer may not match installed app.
- Previous portable debug artifact `40044831118`, artifact `12539924835`, likewise a **separate companion download**. Available older standalone `Asterion-Art-debug.apk` is identical to the nested APK bytes and is not duplicated.
- Fusion tensor preflight `40044619909` and source pin `40044548814` archives remain available via GitHub links; source manifest is in `fusion/fusion_b_sources.json`.

### Original model weights not included
**Not recovered as owned files:** 4+ GB Rin/MiaoMiao originals, actual Fusion B fused checkpoint (never created), converted QNN Hassaku, SuperImage Pro binary/closed-source assets, device private app data/models, NoWorld local assets, or unseen chat attachments. Pointers/SHAs are preserved. Do not claim the ZIP contains those files. Never store access tokens, Android signing keys or private Core library dumps in a public repo.

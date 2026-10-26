# ASTERION ART — DETAILED CONVERSATION RECOVERY (2026-10-26)

**AUTHORITY:** This directory is a read-only migration record, not a request to restart or overwrite the application. The branch was forked from `main` at exact commit `88206baa9e2a33d41195834783023a9fb0c7e0dc`. Its source tree preserves the actual 24 October 2026 implementation, including newer work not yet present in older chat handoffs. Maintain that newer baseline unless a user explicitly requests another version.

**Repository:** https://github.com/Azrazieliz/AsterionCompiler
**Recovery branch:** `migration/asterion-art-conversation-backup-20261026`
**Source branch:** `main` at `88206baa9e2a33d41195834783023a9fb0c7e0dc`
**Latest verified source-head Android CI:** https://github.com/Azrazieliz/AsterionCompiler/actions/runs/40099170762 (success). CI/packaging success is not device functional proof.
**Core dependency:** https://github.com/Azrazieliz/Asterion-Core at `7d7ab3318cfdd829b28faa301568cedc8b100961`; cross-app Core bridge branch `asterion-art-sheet-bridge-20261023` at `a36ef4c46cbcc62849db8ec4bd87356eeb74bb9c`.

## Files here
- `ART_GITHUB_SNAPSHOT_20261026.json`: **80 exact commit SHAs/messages**, branch SHAs, all source path/blob IDs, PR references.
- `ART_CI_RUNS_20261026.json`: **100 GitHub Actions runs** with commit hashes/statuses/links.
- `CORE_GITHUB_SNAPSHOT_20261026.json`: **100 Core commits**, 32 Core branches, builds and SHA pointers.
- `01_USER_REQUIREMENTS_AND_DECISIONS.md`: binding product contract including all corrections.
- `02_CONVERSATION_AND_IMPLEMENTATION_HISTORY.md`: request/action/verification chronology and failures.
- `03_TECHNICAL_ARCHITECTURE.md`: source map, protocols, environment, risks.
- `04_UNFINISHED_TASKS_AND_KNOWN_ISSUES.md`: prioritized handoff; no false completion claims.
- `05_RECOVERABLE_ASSET_POINTERS.md`: project assets, methodology ZIP, design boards, binaries and what is not in public Git.
- `06_PASTE_TO_RESUME.md`: standalone next-chat prompt.

**Separate local full recovery ZIP:** The user's attached Methodology.zip, character sheets, prior APKs, source snapshots, recorded reference video and earlier Library handoffs should be retained *outside public Git history*. They are in the user-facing download archive generated in the current conversation, not silently published to this public repository. GitHub branch source remains accessible independently even if that archive link expires.

## Honesty labels
- VERIFIED: commit, file hash, build/test record, directly examined source or zip bytes.
- IMPLEMENTED, NOT DEVICE VERIFIED: code compiled/exists but the phone's model/NPU/audio/video output hasn't been tested.
- PROPOSED: architectural design/fusion candidate not executed or benchmarked.
- INACCESSIBLE: items inaccessible to tools or not available as original chat transcript. Do not invent the missing material.

Preserve package name `com.asterion.art`; never uninstall/clear app data or rotate signing identity in a way that makes updates impossible without prior backup. The installed debug APK signer may differ from the GitHub CI signer: verify actual certificate first.

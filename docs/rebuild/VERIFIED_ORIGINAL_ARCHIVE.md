# Original methodology archive — verified source manifest

The full original user-supplied source archive is in the owner's ChatGPT Library under:
`/Asterion Art/ASTERION_ART_20261030_OWNER_REFERENCE_AND_METHODOLOGY_ARCHIVE.zip`

Byte-verification on 2026-11-27:
- Outer ZIP SHA256: `4ad83b25bd70894488c63d5ac73cdf0ead8389947c01185e043fe78a5d54639e`; size 7,796,874 bytes.
- Nested `Methodology(2).zip` SHA256: `14888597e24d3e39cb9ec6b665ebed0f2a753321b8b12f7a4bb311f51134d9f2`; size 1,452,241 bytes.
- Nested archive: 40 entries, including the original detailed sheet and prompt-writing authorities.
- Seven original visual reference images remain in the Library ZIP. They were **not** indiscriminately committed to GitHub or designated as training data.

Critical authoritative sources **not in the 14 legacy-app methodology blobs copied into `spec/methodologies/`** (retrieve by archive path and verify before implementation):
- `Methodology/Character Sheet/character%20sheet.md` SHA256 `42b115a7fb013056e095a8ccdedef354fdd7d77d70dad65328a39d06caded8b3`
- `Methodology/Theme Sheet/Theme-Sheet-Workflow-v1.md` SHA256 `f4351f8660a672838e12c5d9a6f006b0751fa4c5c09a60e8aeb5fc17f00505b3`
- `Methodology/Art Style Sheet/Art_Style_Master_Sheet_Absolute_Authoritative_Rules.md` SHA256 `3268851df749859f53f4325bcd6ac0eaa06697523a8fd97cc53727812c3e33d1`
- `Methodology/Element Sheet/Element_Sheets_Sole_Authoritative_Ruleset.md` SHA256 `53bd1dfd445b2f48a6f1a6e86b42d0480e54093fc2b7f150211ba4e234e425c3`
- `Methodology/Prompt Writing/prompt.md` SHA256 `2af1810d4d87414b2384a93c6d72ee7959faeb599b0bf84d0cc16e694cf75a2a`
- `Methodology/Character Creation/ILLUSTRATIONS_PROJECT_MASTER_EXECUTION_LOCK_v1.5.md` SHA256 `02ebaa1766fb52d976a91ae2af3dfe9b89c7331cf88509876ef5b2a84b5963ae`

The original full ZIP is **not** checked into the clean branch. Never treat missing files from the legacy app assets as permission to invent sheet rules. These artifacts are development/training/reference authorities, not material to supply to a generic model during production inference. Legacy code is accessible from the archive branch if reusing verified tooling is warranted.

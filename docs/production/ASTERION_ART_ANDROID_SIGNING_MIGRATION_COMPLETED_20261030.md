# ASTERION ART — Completed Android signing migration and full data restoration

**Status: COMPLETED / VERIFIED on device · 2026-10-30.** This record supersedes the pre-migration warnings in [ASTERION_ART_PERSISTENT_SIGNING_20261030.md](./ASTERION_ART_PERSISTENT_SIGNING_20261030.md). No new certificate may be generated for ordinary future releases: reuse the key saved at the precise paths below.

## Installed application

| Property | Verified value |
|---|---|
| Android package | `com.asterion.art` |
| Version | `0.2.3` (`versionCode=5`) |
| Permanent release certificate SHA-256 | `51e506a7ecfd7c7bf5962e442d55a10ceaf12f796583330a9a86f0bd749d3773` |
| Verified installed APK SHA-256 | `043966d21eed63c803240be15f7ce0027d52d9e4e4772bbf55680b24f7447d7f` |
| APK size | `302209798` bytes |
| Source CI | [successful run 41112425042](https://github.com/Azrazieliz/AsterionArt/actions/runs/41112425042), build source `60da233ef4eebd49023aea065940855322d3feb3` |
| Signed APK on laptop | `D:\AsterionArtSigning\migration-20261030\AsterionArt-v0.2.3-PERSISTENT-20261030.apk` |
| Signed APK on phone | `/sdcard/Download/AsterionArt-v0.2.3-PERSISTENT-20261030.apk` |

Android accepted an `adb install -r` upgrade of the temporary new-signer debug build to the non-debuggable release. After installation the actual installed `base.apk` was pulled back from Android and both its full SHA-256 and certificate were matched independently to the signed release. The process `com.asterion.art` ran after launch and no fatal `AndroidRuntime` entries were found in the checked log window. **This verifies installation, not quality or real QNN/Wan inference.**

## Data preservation — full verification

- Original debug-signed `com.asterion.art` APK was preserved at `D:\AsterionArtSigning\migration-20261030\installed-original-signed.apk`; original APK hash `0fbba05dbc4f51e9cfec1b57ff92a3c62859f0e47955234fff5eb6dd0bb7f37f`; original certificate `388e8551e78392be4a5211e2a44b3e2958a8bb00c05eed5c8d984fccf1175ae6`.
- The entire pre-migration app-private `files/`, `databases/`, `shared_prefs/` and `no_backup/` tree was archived **before** any uninstall; complete archive is `D:\AsterionArtSigning\migration-20261030\private-app-data.tar`.
- Archive exact SHA-256: `dbd2187509d6e9cdbef9508a011508a371d3a3c4b1ddb96c50bf3d46172167c1`.
- Archive size `5734474752` bytes; **17 regular files / 5734455667 content bytes**, including model files and metadata, catalog preferences and WorkManager records. Archive entries and every byte were read and validated before migration.
- Initial streaming restore was interrupted by ADB USB disconnection after the temporary new-certificate app was installed; the automatic rollback also could not reach the device. **This interruption did not alter or corrupt the independently verified laptop archive.**
- ADB returned; a local independently tested 32 MiB-per-chunk, offset-addressed, fsynced `adb exec-in run-as com.asterion.art dd` resume procedure restored missing files without uninstalling the temporary app again.
- Each of **all 17 restored files** was checked on-device for full byte size and SHA-256 against the corresponding archive entry **before final non-debuggable release upgrade**. Both giant UNet model files passed full SHA-256:
  - `unet_part1.bin`: `3648753cadc5140d7480221ca4d9566a102c5c26b2b373479bc4df67301adb48`
  - `unet_part2.bin`: `3d05df779bd30ad030cd92d8ac77503a3f30db138cd5ccdc67445a8b3de475e6`.
- Proofs saved locally: `backup-evidence.json`, `restoration-verified.json`, `file-restoration-checksums.json`, `resumable-restore-status.json`, `finalization-status.json` in `D:\AsterionArtSigning\migration-20261030`.

**Key point:** Android cannot update one certificate to another without a valid signing lineage. The debug-key → permanent-key transition was therefore accomplished only after full backup, with temporary debuggable new-signer installation, verified full restoration, then in-place same-certificate release upgrade. **Do not repeat this migration**; future APKs must use the permanent certificate and upgrade normally.

## Future release key: persistent, no rotation

- Keystore: `D:\AsterionArtSigning\identity-20261030\asterion-art-persistent-20261030.p12`.
- Alias: `asterion-art-persistent-20261030`.
- Key password: Windows DPAPI-protected `D:\AsterionArtSigning\identity-20261030\release-password.dpapi` (never printed or committed).
- Public certificate and `identity.json` manifest beside the private keystore.
- Local private backup (same Windows user profile): `%USERPROFILE%\Documents\ASTERION-PRIVATE-SIGNING-BACKUP\identity-20261030`.
- Reusable sign-and-upgrade guard: [`native/sign_release_persistent_20261030.ps1`](../../native/sign_release_persistent_20261030.ps1). New packages must carry **this exact cert**. Never generate a replacement automatically.
- Protected identity is bound to the current Windows DPAPI user profile. The separate local key copy is not independently recoverable after full profile loss; an additional secure recovery escrow would require a distinct secret-availability decision. Do not upload the private key into the public repository.

## Remaining acceptance boundary

Installation and preserved local data have been verified. **Native QNN image synthesis, Qwen3 reasoning, Wan video generation, actual five-phase Artwork quality, model compatibility and mode-by-mode methodology fidelity are not certified by this migration.** The installed app includes a Testing Center to exercise real native inference and surface independent PASS/FAIL evidence. Do not claim those tests passed without their device outputs.

This document records the actual terminal state and supersedes any earlier claim that Android migration was still blocked.

> **UPDATE — MIGRATION COMPLETED (30 October 2026):** The phone now runs `com.asterion.art` version 0.2.3 using the permanent certificate `51e506a7…749d3773`. All 17 private-data files (5,734,455,667 bytes) were restored and individually SHA-256 verified. The original debug-signed APK and complete pre-migration archive remain backed up. **Use [the completed-migration authority](./ASTERION_ART_ANDROID_SIGNING_MIGRATION_COMPLETED_20261030.md) for current status.** The warnings below describe the earlier pre-migration state, not the present installation.\n\n# ASTERION ART — Persistent signing identities and preservation record

Date: 2026-10-30. **Owner-requested action executed**: generate a new key and save it so future releases can reuse it rather than rotate keys every time.

## New permanent signing identity — created, verified and backed up

- Release alias: `asterion-art-persistent-20261030`
- Key algorithm: RSA-4096; keystore: encrypted PKCS#12; password: cryptographically random 48-byte source, saved as Windows DPAPI-protected SecureString; **never committed or printed**.
- Certificate SHA-256: `51e506a7ecfd7c7bf5962e442d55a10ceaf12f796583330a9a86f0bd749d3773`
- Keystore SHA-256: `8126c65bb771981ca4636269d2e2a3bed900c66b3b9716ddf5b18b02af777ae8`
- Private keystore: `D:\AsterionArtSigning\identity-20261030\asterion-art-persistent-20261030.p12`
- Encrypted password: `D:\AsterionArtSigning\identity-20261030\release-password.dpapi`
- Public certificate: `D:\AsterionArtSigning\identity-20261030\public-signing-certificate.pem`
- Local manifest and tested verifier: `identity.json`, `verify-saved-key.ps1`.
- Restricted-permission, checksum-verified **second local copy**: `%USERPROFILE%\Documents\ASTERION-PRIVATE-SIGNING-BACKUP\identity-20261030` (5 preserved files).
- Verification performed after creation: decrypt saved Windows-protected password, open persisted keystore through keytool, verify exact alias, certificate fingerprint and keystore hash. PASS.
- The previous `D:\AsterionArtSigning\asterion-art-release.p12` and `release-pass.dpapi` were not modified or replaced.

**No private signing credential belongs in Git.** This file contains public fingerprints and on-device retrieval locations only. The password backup is recoverable only under the same Windows DPAPI user profile, and is not a portable, disaster-proof key recovery system; provision an independently secured recovery procedure before relying on it across Windows reinstall or laptop loss.

## Actual installed-phone identity — 30 October 2026 verified from APK

Read-only ADB pull and `apksigner verify --print-certs` verified installed Android package `com.asterion.art` version `0.2.3`. Phone-side installation was **not changed**.

- Installed certificate SHA-256: `388e8551e78392be4a5211e2a44b3e2958a8bb00c05eed5c8d984fccf1175ae6`.
- Installed signer subject: `C=US, O=Android, CN=Android Debug`.
- Existing laptop debug keystore SHA-256 certificate: `9bc439712a0a460cc4703ebf254aaaf9bf3cc6e67b0ced6fc12162ed3ca3b9ad`. Not a match.
- Previously stored local signing keystore certificate: `397a14cd2683a76f18cdd501e767553caa9abd2ad4a3a2c5b6fe3f56f8fd86fa`. Not a match.
- Existing historical repository original owner-certificate pin: `b0263c37c9f4ae40423e30d275b6c8af0b777ed223d01e3fff1a114a43707ede`. Not a match.
- Newly generated signing identity: `51e506a7ecfd7c7bf5962e442d55a10ceaf12f796583330a9a86f0bd749d3773`. Not a match.

### Mandatory Android compatibility caveat

**A newly generated key cannot update the currently installed application, which is debug-signed under a different key.** Android requires the same signing certificate or a valid authenticated key-rotation lineage. Neither may be assumed or fabricated. Do not attempt to use `adb install -r` with a mismatched certificate, nor `adb uninstall`, `pm clear`, deleting `/files/asterion-qnn-models`, or destroying local designs. All data remains on the device.

Read-only `adb shell run-as com.asterion.art id` confirmed that the existing app is debuggable and its private app data is readable with explicit app access, including ~5.6 million 1K blocks of models. Before any *authorized* signing migration, take and verify a full non-destructive backup of its app-private data; restoration support must be proved before uninstalling anything. **No migration, data clearing, installation, or backup was performed here.**

## Future build usage

For future APKs signed with the NEW permanent key, use `native/sign_release_persistent_20261030.ps1`. It verifies the pinned keystore fingerprint, saved DPAPI password, signed artifact certificate and existing installed-app signer before allowing installation. The earlier historical-signature verification script remains untouched.

The new key is valid for a **fresh installation or a device already migrated onto that exact certificate**. A compatibility-preserving upgrade of today's debug-signed installation remains blocked until its original private signing key is recovered or a separately approved, independently verified migration plan preserves data.

Work status: local key creation **DONE**, key persistence **VERIFIED**, backup **DONE**, installation **UNCHANGED**, release APK **NOT YET DELIVERED**.

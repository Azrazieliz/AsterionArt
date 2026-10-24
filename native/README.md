# Asterion Art: native QNN engine integration

Asterion Art integrates **only the native inference runtime** from the official, non-filtered
[Local Dream](https://github.com/xororz/local-dream) release v3.0.0-alpha.6, not its screens or
installed Android application. The original source repository remains unchanged.

- Source APK: https://github.com/xororz/local-dream/releases/download/v3.0.0-alpha.6/LocalDream_armv8a_3.0.0-alpha.6.apk
- Verified upstream SHA-256: \`bec75b588200eb83aedd82b0a26ed8c5de4cfd923d0f51e1e65d241fccc11f82\`
- Upstream license: CC BY-NC 4.0 (noncommercial; attribution retained)
- Qualcomm QNN native runtime: separate Qualcomm notices/terms apply.
- No source checkpoint or QNN model files are bundled with the runtime. The model
  library is imported separately into the app.

## Building with native runtime

The CI workflow downloads and hashes the official upstream APK, extracts the ARM64 native
runtime, QNN/DiT dependencies and provenance into temporary Gradle source directories,
then compiles one unified Asterion Art APK. This does not require a 64 GB PC.

\`\`\`sh
python3 native/prepare_native_runtime.py --project .
gradle :app:assembleDebug
\`\`\`

Runtime packaging is ***not*** by itself a proof the Qualcomm Hexagon backend
runs on an S26 Ultra. Its SoC/HTP version, converted model format, OS security
restrictions, memory pressure and Qualcomm runtime compatibility require
device tests. Any error must appear as failure, not a completed generation.

The content policy and output restrictions of bundled third-party models are
not modified; this integration uses the official release variant *without*
its optional NSFW classification package.

## Release policy

This is a noncommercial personal-use project. Preserve upstream credits,
QNN license notices and original license files. Commercial redistribution
needs separate permission.

# Asterion Art

**Status: initial from-scratch Android implementation.** The old AsterionCompiler source tree was replaced; its former main head is retained in `archive-pre-asterion-art-20261023`. Nothing has been removed from that historical branch.

Asterion Art is a touch-first Android creative system. The first functional slice supports importing real reference images, assembling normalized deterministic character/art-style/theme/element sheets, persistent offline catalogs, background sheet rendering, saved results, rating and advanced parameter controls. Source code is deliberately new; the upstream Local Dream and licensed third-party apps have not been copied.

## Architecture
- `model`: immutable typed schema, distinct franchise character forms and provenance.
- `data`: persistent local JSON store (v1); migration path to SQLite once Core bridge is implemented.
- `render`: real Android Canvas sheet renderer using user-selected images; no AI-generated fake annotation.
- `work`: WorkManager background sheet jobs and output persistence.
- `ui`: clickable Compose workspace, gallery, selection and manual scoring.
- `engine` (planned): embedded QNN native model inference, five-pass continuation, video backend, enhancement adapters.
- Asterion Core remains separate and authoritative. Share reference images via persisted content URIs before the dedicated verified cross-app service is ready.

## Run
Android Studio with JDK 17 / Android SDK 35; `gradle :app:assembleDebug`.
Cloud CI builds and tests the APK. No installed APK is claimed until the build succeeds.

## Non-claims
This first iteration **does not generate AI images/video**, merge checkpoints, train models, embed SuperImage Pro or connect directly to Asterion Core. Those are remaining integration tasks, not working functionality.

## Model order
Rin Flanime > MiaoMiao > Illustrious Hassaku XL > NoobAI XL v11. Animayume excluded. Anima and Illustrious/SDXL cannot be weight-averaged directly; verify a compatible fused candidate before deploying it as the single anime route.

## UI
No text authoring for normal tasks. Sheet references are selected using the Android file picker; Core search is reserved for the Core integration. Advanced generation parameters are touch-adjustable.

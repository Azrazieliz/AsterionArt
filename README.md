# Asterion Art — clean reconstruction

The reconstruction branch contains the Android studio, recursive Character selector, persisted creation queue and workflow executor. It also contains the reference-projection experiment and the preferred-model comparison tools. Production creation requires a trained artistic author, renderer and evaluation bundle compatible with each workflow.

## What is retained

- Binding owner product architecture: `docs/architecture/ASTERION_ART_MASTER_ARCHITECTURE_CANONICAL_WITH_ADDENDUMS_20261030.md`.
- Latest controlling owner decisions: [November 28 reconstruction decisions](docs/production/OWNER_RECONSTRUCTION_DECISIONS_20261128.md); [authority order](docs/rebuild/AUTHORITY_AND_RESET.md). Other owner corrections: `docs/production/OWNER_*.md`. Later decisions supersede contradictory historical implementation assumptions.
- Original methodology material, byte-for-byte Git blobs copied from the former app assets into `spec/methodologies/`. **Development and validation inputs only; they must never be shipped as runtime instruction prompts.**
- Recovered Artwork five phases and reference authority: `docs/owner_recovered/`.
- Original signing identity evidence and public identity material, not private keys: `docs/production/ASTERION_ART_PERSISTENT_SIGNING_20261030.md`, `spec/signing/`.
- Historical context handoff `docs/production/ASTERION_ART_EXECUTION_HANDOFF_20261127.md`, which points at the now-archived implementation and is not evidence that its tasks were completed.

## Recovery of legacy experiments

- Previous main: `archive/art-main-pre-clean-20261127` (`627015d1ea9788f8e44fff587ac76b64ceeb988b`).
- Previous development/integration: `archive/art-integration-pre-clean-20261127` (`4691df4f24a01d0115973f1fbbcf011e1f9621de`).

These branches retain the old Android project, native libraries and historical tooling for **selective, independently verified reuse**; they are not the new architecture.

## Non-negotiable direction

- Native, mode-specific executable creative intelligence; learned design and visual conditioning, not generic reasoning over methodology documents or deterministic creative templates.
- Three distinct trained visual model families (Anime first, Cosplay and Realistic secondary), local Android inference, real reference control, predominantly automatic creation, one coherent Character Sheet instead of crude panel collage.
- Artwork-only five phases, independent Originals workflows, Sheet Creation, video with Sound-owned dialogue/music/SFX and Art-owned visual timing/lip-sync, durable background execution, automatic best-chain enhancement, Nexus shared rating and learning. Models and assets streamed from TeraBox through Nexus and cached locally. Integrate with Nexus, Sound, Writer, 3D, NoWorld and Control; Core, Downloader and Rounder are Nexus donors, not separate targets.
- Original references and creative methodology semantics are authoritative. Owner questions are to be consolidated once before new implementation. No invented user approvals.
- Do not claim completion until signed device deployment and real visual quality/function acceptance. Verify any free GPU allocation before using it. No paid compute.

## Build status

```sh
./gradlew :creation:test :app:assembleDebug
```

The debug build uses `com.asterion.art.reconstruction`. The release configuration retains the existing package and owner signing input names. See [reconstruction evidence](docs/production/CLEAN_RECONSTRUCTION_EVIDENCE_20261128.md) for the tested scope and model results.

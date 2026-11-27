# Asterion Art — Owner reconstruction decisions (2026-11-27)

Status: owner-approved reconstruction decision record; append-only supplement to the canonical architecture and existing methodology authorities.

## Selected decisions

| ID | Topic | Owner selection |
| --- | --- | --- |
| NR-01 | Neural architecture | Benchmark all architectures and select the best. |
| NR-02 | Acceptable pre-generation time | Best achievable balance of quality and speed. |
| NR-03 | Model development | All approaches, selected by measured results. |
| NR-04 | Image-generation engine | Reconstruct the entire image pipeline if justified. |
| NR-05 | Originals methodologies | Review each methodology with the owner before modifying it. |
| NR-06 | Additional mode-specific parameters | Propose specialized controls for each mode. |

## Additional owner requirement

The app is supposed to work in the background, so even five minutes of pre-generation is acceptable, although that would be rather long. Five minutes is not a mandatory target or a fixed upper limit. Optimize the measured balance of quality and speed rather than removing creative intelligence solely to achieve an arbitrary short latency.

## Scope and preservation

- Architecture and model-development choices must be selected through comparative evidence, not assumed in advance.
- Entire image-pipeline reconstruction is conditional on justification; this decision does not select a replacement engine or checkpoint.
- Each Original's methodology must be reviewed with the owner before any change to that methodology. Existing authoritative files and contracts remain unchanged pending that review.
- New specialized controls are proposals, not approved axis names, values, scales, defaults or implementation requirements. Existing owner-approved Theme-only B/E/T scope is not overridden.
- Existing durable background execution, checkpoint recovery, local generation, model/data preservation, signing identity and other established requirements remain in force unless separately amended.
- Preserve all unanswered decisions as open. This record does not approve any new taxonomy, specific model, training dataset, methodology revision, merge, or deployment.

## Motivation and evidence boundary

The owner reports that enabling Qwen prevented image generation altogether. Bypassing neural authoring yielded unacceptable creative text and images. These observations motivate comparative reconstruction; they are not proof that any specific alternative architecture will succeed. Measure actual end-to-end mode output, quality and latency, including background operation, before selecting a production solution.

Canonical authority: [master architecture and addendums](../architecture/ASTERION_ART_MASTER_ARCHITECTURE_CANONICAL_WITH_ADDENDUMS_20261030.md).

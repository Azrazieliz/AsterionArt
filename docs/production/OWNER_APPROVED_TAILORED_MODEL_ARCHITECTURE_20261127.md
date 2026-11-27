# Asterion Art — Owner tailored model architecture decisions (2026-11-27)

Status: approved selections only; supplements OWNER_APPROVED_NEURAL_RECONSTRUCTION_20261127.md.

| ID | Topic | Owner decision |
| --- | --- | --- |
| TM-01 | Training infrastructure | Existing PC and free resources only. |
| TM-02 | Phone model storage | Prioritize quality without a predetermined storage limit. |
| TM-03 | Image foundations | Three independent Anime, Cosplay and Realistic models. |
| TM-04 | Quality modes | Adjustable quality and automatic optimization. |
| TM-05 | Video processing time | Optimize quality against processing time without a fixed limit. |
| TM-06 | Long-term improvement | Hybrid approach: automatic small updates and reviewed major updates. |

## Additional owner direction

Verbatim: "But be careful, not the single restriction. And aside from that, loras and sheets, how would they work ? You can try to redefine everything based on the tests we did with the previous architecture."

The first sentence is ambiguous; preserve it verbatim rather than assigning a new policy. The owner requests proposals for LoRA and sheet architecture and permits reconsidering the technical architecture using prior test evidence. This does not approve any particular proposed redesign, new restriction, new taxonomy, or changed Original methodology. Previously established review-before-modifying-Originals remains binding. All unanswered decisions remain open.

## Boundaries

- No paid compute commitment. Existing PC availability, GPU specifications and free training feasibility must be measured.
- Three distinct production image foundations; specific base weights, architecture, fusion ratios and training recipes remain undecided.
- Quality/storage decisions do not remove physical RAM, storage, thermal, compatibility or reliability constraints.
- Automatic small model changes require validated scope, provenance, regression evaluation and rollback. Major changes require review. Exact thresholds and approval workflow remain open.
- No training-image use authorization is inferred from quality references. No specific model, merge, methodology change or deployment is approved by this record.

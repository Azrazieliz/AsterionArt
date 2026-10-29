# Artwork Creation — recovered five-phase quality protocol

**Original source:** `asterion_art_architecture_v1.zip`, archived 2026-10-23,
`asterion_art_architecture_v1/ARCHITECTURE.md` §5 and
`contracts/workflow_registry.json` (`five_pass_names`).
Additional original source: owner-supplied `Methodology(2).zip`,
`Methodology/Prompt Writing/prompt.md` §§4–8.

The architecture archive labels itself **Approved design target / architecture
deliverable**, while explicitly distinguishing the original Prompt Writing
rules from implementation subdivisions for Passes 2–5. Preserve that
distinction. This is recovered earlier authority, not a newly devised method.

## Recovered pass names (original workflow_registry.json)

1. `FOUNDATION`
2. `IDENTITY_GEOMETRY`
3. `MATERIAL_LIGHT`
4. `FINE_CONSTRUCTION`
5. `FINAL_COHERENCE`

## Recovered stage responsibilities (original ARCHITECTURE.md §5)

1. **Pass 1 — Foundation:** all characters, canonical appearance, outfit, pose,
interaction geometry, theme, environment, element macro-form, frame, camera
and primary lighting exist and are in the correct positions. Failure
requires re-branching before refinements.
2. **Pass 2 — Identity and geometry stabilization:** preserve composition
while reinforcing source traits, distinct character identities, pose
connectivity, interfaces/contact, required equipment and element scale.
No wholesale re-layout.
3. **Pass 3 — Materials and illumination:** skin/fabric/metal and other defined
materials, surface behavior, shadows/highlights, lighting-to-material
consistency, atmosphere and environment interaction.
4. **Pass 4 — Fine construction:** hair/eyes, seams, ornamentation, small
markings, surface geometry and high-frequency details that already
belong to established structures. No newly invented major objects.
5. **Pass 5 — Final visual coherence:** consistency and artifact-limited
finishing within the current image; verify all frozen identifiers/required
structures before accepting the candidate. Selection and external upscaling
occur afterward, not in place of Pass 5.

**Original prompt-writing authority:** Pass 1 establishes every major
structure, every required major interaction and the entire composition.
Pass 2 and subsequent passes preserve that structure and refine only
source-supported textures, surfaces, materials, lighting and fine detail.

## Structural constraints

- Applies **only** to Artwork Creation, not to Sheet Creation, Originals,
  image editing or video.
- Each actual native call retains the **complete** binding source-derived
  prompt; no token-sliced diffusion repainting.
- Image-to-image / latent continuation must be actually supported by the
  selected backend and validated before claiming five real passes.
- Every pass must retain full provenance, settings and source-sheet identity.
- `character -> outfit and pose -> theme -> environment and element ->
  art style and rendering and camera and lighting` is the original
  metadata priority order.
- Theme Sheet explicitly prevails on overridden visual states; never
  restore an overridden Character Sheet outfit/expression.
- Original Prompt Writing document prohibits narrative inference.
- A stage failing identity, composition or required structure must not
  silently be recorded as successful or be repaired by inventing subject matter.

**Source scope:** This reconstruction preserves the recovered approved
October 23 version; it is not proof of completed image generation or
owner acceptance of any subsequent code changes.

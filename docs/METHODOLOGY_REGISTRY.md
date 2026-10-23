# Methodology import map

The authoritative source archive is the supplied Methodology.zip. This registry maps it to intended internal workflows.

Visible modes:
- Character Sheet
- Art Style Sheet
- Theme Sheet
- Element Sheet
- Art Style Correction
- Character Creation
- Outfit / Weapon / Transformation / Mutation / Creature / Alternate Universe

Internal, not separate mode tiles:
- pose geometry workflow
- source-image analysis
- prompt compilation and token priority
- validation, freezes, revision/versioning
- five-pass image process

Excluded:
- translation/retypesetting and CBZ publishing.

Five generation stages (actual inference pending):
1. Structural foundation: characters, pose, contacts, clothes, theme, elements, composition and major light.
2. Identity/geometry consolidation: protect macro layout; stabilize facial/body, outfit and object invariants.
3. Surface/material/lighting: fabrics, metals, shadows, highlights and environmental interaction.
4. Microstructure: hair, ornament, edges, fine detail without replacing established large structures.
5. Render coherence: final restrained image refinement. Score, choose and upscale afterwards.

The prompt methodology explicitly says pass 1 must establish all major structures; subsequent passes must not invent them.

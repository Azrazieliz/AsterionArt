# DESIGN DIRECTION — Character Creation Design-Language Database

**Status:** ACTIVE / CANONICAL DESIGN-DIRECTION REGISTRY  
**Document role:** Shared design-language authority used together with `Character_Creation.md`  
**Coverage:** Phase 1 character/body/identity + Phase 2 outfit + Phase 3 weapon  
**Storage model:** One document contains every complete Design Direction entry.  
**Execution model:** Exactly the selected Design Direction entry becomes active unless the user explicitly requests another operation such as comparison or deliberate mixing.

---

# 0. PURPOSE

`Design_Direction.md` is the central database of reusable character-design languages for the Character Creation workflow.

It exists to make a named design direction executable **without requiring the original reference images or franchise screenshots at generation time**.

The system separates responsibilities as follows:

> **`Character_Creation.md` = execution authority and phase methodology.**  
> **Concept data = what must physically/materially become the character.**  
> **Active Design Direction = how the valid Phase-1 body, Phase-2 outfit, and Phase-3 weapon are aesthetically authored.**

A Design Direction is therefore neither an art/rendering style nor a copied-character template. It is a structured design grammar.

---

# 1. GLOBAL AUTHORITY / PRECEDENCE

## 1.1 Character Creation remains authoritative

No Design Direction may weaken, replace, bypass, or reinterpret mandatory rules in `Character_Creation.md`.

This includes, among other things:

- concept decomposition and complete concept fusion;
- body-first Phase-1 ownership;
- independent character authorship and population differentiation;
- transformed / altered anatomy requirements;
- frozen-state dependencies;
- Phase-2 ownership of clothing only;
- Phase-3 ownership of weapons only;
- unusual-anatomy accommodation;
- renderer handoff and phase presentation;
- validation, freeze, and revision rules.

If a concept-valid body must become extreme, transformed, muscular, petite, asymmetrical, multi-limbed, hollow, nonstandard, or materially unusual, the active Design Direction must **adapt its design language to that body**, not normalize the body back toward familiar reference examples.

**Concept-first aesthetic authorship.** Character creation always starts from supplied/resolved concepts. The causal order is **SUPPLIED CONCEPTS → CANONICAL IDENTITIES / ANCHORS → SYMBIOTIC PHYSICAL CONSTRAINTS → VALID BODY-SOLUTION SPACE → DESIGN-DIRECTION AESTHETIC AUTHORSHIP → PREMIUM FEMALE BODY + IDENTITY**. Direction authorship works inside that valid space: body-regime refinement, proportion/mass, face/eyes/hair, transformed anatomy, silhouette, hard/soft relationships, shape grouping, rhythm, asymmetry, material presentation, colour, focal hierarchy and desirability. It is neither a replacement concept nor an independent generator, theme source or post-hoc cosmetic filter. It cannot change frozen inputs, remove anchors, add an unsupported species, substitute franchise tokens for symbiosis or normalize unusual anatomy. A direction-only request supplies no concept set; request the missing concept specification before creation. Any random/no-theme language means no additional theme supplied, with concepts already present.

## 1.2 Design Direction authority

Within the solution space left open by Character Creation, the selected Design Direction governs the aesthetic decisions needed to make the result belong recognizably to that design family.

It may govern, as applicable:

- body/proportion preference among valid alternatives;
- face / eye / hair design language;
- silhouette refinement;
- transformed-body aesthetic treatment;
- identity finishing;
- outfit architecture and coverage logic;
- garment families, material language, layering and ornamentation;
- weapon category preferences where several categories are equally valid;
- weapon macro/secondary-detail language;
- cross-phase coherence;
- style-specific anti-drift and validation.

## 1.3 Rendering separation

Design Direction does not own the final drawing/rendering style unless a future workflow explicitly assigns that responsibility.

It controls **what the character, clothing, and weapon are designed like**, not the brushwork, shader, painterly finish, line treatment, or other rendering language.

---

# 2. ENTRY REGISTRY

| ID | Canonical Design Direction | Status | Complete Phase Coverage | Entry Version |
|---|---|---|---|---|
| DD0001 | Shoujo Kaisen | ACTIVE / COMPLETE | Phase 1 + Phase 2 + Phase 3 | 1.1 |
| DD0002 | Fate | ACTIVE / COMPLETE | Phase 1 + Phase 2 + Phase 3 | 1.0 |
| DD0003 | Arknights | ACTIVE / COMPLETE | Phase 1 + Phase 2 + Phase 3 | 1.0 |

The registry is expanded in this same document as new directions are compiled.

IDs are stable internal identifiers for this Design Direction database. Renaming a display label does not silently change an existing ID.

---

# 3. ACTIVATION AND ROUTING

## 3.1 Exact activation

A user instruction such as:

`Design Direction: Shoujo Kaisen`

activates **DD0001 only**.

A user instruction such as:

`Design Direction: Fate`

activates **DD0002 only**.

A user instruction such as:

`Design Direction: Arknights`

activates **DD0003 only**.

Equivalent unambiguous phrasing such as `Shoujo Kaisen design direction` or `use the Fate design direction` resolves to the corresponding canonical entry. Direction activation is a routing choice, not a character-creation command and not a concept input. Creation requires supplied/resolved concepts first; request them if missing.

## 3.2 Entry isolation

When one direction is active:

- load that entry in full;
- do not import rules from another Design Direction entry;
- do not average multiple entries together;
- do not silently borrow a convenient rule from another franchise;
- do not treat registry descriptions as substitutes for the full entry.

Only explicit user authorization may activate a deliberate mixed-direction operation. A future mixed-direction method must preserve the source entries rather than rewriting them. Automatic candidate-specific selection and per-sheet distribution are owned by `Character_Creation.md` §1.12; different candidates assigned different complete entries are not a mixed-direction operation. Each selected character retains her own assigned entry through the downstream freezes.

## 3.3 Complete-entry rule

The active entry is consumed as a **whole design grammar**, not as a bag of optional keywords.

Execution must use the phase-specific section corresponding to the active Character Creation phase while retaining the entry's cross-phase invariants and coherence rules. Read the complete final executable entry; historical evidence and provisional hypotheses are preserved separately for audits and revisions and do not activate through entry loading.

## 3.4 Phase routing

**Phase 1:** use the entry's body / identity / transformed-anatomy grammar.  
**Phase 2:** use the same entry's outfit grammar while preserving the frozen Phase-1 body.  
**Phase 3:** use the same entry's weapon grammar while preserving the frozen body + frozen outfit.  
**Phase 4:** no new design-direction authorship occurs; assembly preserves the already frozen Phase-1/2/3 decisions unless Character Creation says otherwise.

**Phase scope is strict.** Cross-phase baselines and vocabulary lists apply only to their owning active phase. Phase 1 authors the intrinsic character under Character Creation §5.1.5A; outfit, weapon, equipment, prop, scene and presentation-device systems retain their separate phase boundaries. Its spatially detached intrinsic anatomy must pass `Character_Creation.md` §5.4.8; attachment or a device source never converts an emitted effect into intrinsic matter. Phase-2 and Phase-3 catalogues retain their own authorized analytical supports and labels under §§6.18 and 7.22. Emitted/environmental effects and later-phase systems cannot enter these phases through a Design Direction example. Bounded intrinsic luminous material is distinct from emitted spectacle.

## 3.5 No source-reference dependency

Once an entry is marked COMPLETE, the executor should not need the original screenshots to apply it. References may later be used to audit, validate, or deliberately revise the entry, but are not required for ordinary execution.

---

# 4. MANDATORY ENTRY CONTRACT

Every Design Direction category added to this document must be complete enough to function as a standalone aesthetic authority under `Character_Creation.md`.

Each complete entry must define at minimum:

1. purpose and authority relationship;
2. core design DNA;
3. cross-phase invariants;
4. Phase-1 body / identity / transformed-body grammar;
5. Phase-2 clothing grammar;
6. Phase-3 weapon grammar;
7. cross-phase coherence compiler;
8. style-specific decision priorities;
9. anti-drift / anti-copy rules;
10. Character Creation execution interface;
11. standalone sufficiency test;
12. phase-specific validation;
13. corpus / confidence / non-restriction notes where relevant;
14. explicit version history.

A direction remains **PENDING** until all required phases are defined strongly enough that the source references are no longer required for execution.

---

# 5. DESIGN-DIRECTION SELECTION PRINCIPLE

The selected Design Direction is a **design bias and aesthetic grammar**, not an anatomy prison.

The correct order is:

> **resolve mandatory concept consequences → independently author the character → apply the active Design Direction to the valid solution → refine within that direction → freeze → continue downstream with the same direction.**

Never reverse this into:

> **choose a franchise mannequin → force concepts onto it.**

Likewise, Phase 2 and Phase 3 must remain character-specific:

> **frozen body → direction-compatible dedicated outfit → frozen outfit → direction-compatible signature weapon.**

---

# 6. DESIGN DIRECTION ENTRIES

## DD0001 — SHOUJO KAISEN

**Registry status:** ACTIVE / COMPLETE  
**Phase coverage:** Phase 1 + Phase 2 + Phase 3  
**Canonical source entry:** embedded below in full.  
**Preservation rule:** The embedded specification is the complete canonical DD0001 authority. Do not substitute a shorter registry summary for it.

<!-- BEGIN EMBEDDED DD0001 SPECIFICATION — CANONICAL IN-DOCUMENT SOURCE -->

# Shoujo Kaisen — Complete Character Design Style Specification

**Status:** ACTIVE / EXECUTABLE DESIGN-STYLE PROFILE  
**Version:** 1.1  
**Scope:** Character Creation Phases 1–3  
**Source basis:** 39 user-supplied Shoujo Kaisen references accumulated across the initial corpus and Addenda 1–3  
**Execution authority:** `Character_Creation.md` remains the sole workflow/execution authority.  
**Style authority:** This document is the sole Shoujo Kaisen design-style authority when `Design Style: Shoujo Kaisen` is active.

---

## 0. PURPOSE

This document defines the **character-design grammar** of Shoujo Kaisen strongly enough that Character Creation can use it without needing the original reference images.

It does not reproduce individual characters, illustrations, poses, compositions, backgrounds, costumes, weapons, or scenes. It extracts the recurring design logic that makes a newly authored character read as belonging to the same broad design family.

The document governs three distinct design layers:

1. **Phase 1 — Character / body / identity design**
2. **Phase 2 — Outfit design**
3. **Phase 3 — Weapon design**

The same style identity must survive through all three phases while each phase retains its own ownership boundaries under `Character_Creation.md`.

The operative relationship is:

> **CONCEPTS determine what the character becomes.**  
> **SHOUJO KAISEN DESIGN STYLE determines how that resolved character is aesthetically authored.**  
> **PHASE 2 translates that same style into dedicated clothing.**  
> **PHASE 3 translates that same style into a signature weapon.**

The design style is a **grammar, not a restriction list**. It does not reject concept-driven muscularity, petite scale, extreme height, unusual topology, extra limbs, cavities, nonhuman appendages, strange substrates, or other transformations merely because the reference corpus contains fewer examples. When `Character_Creation.md` requires such a body, the Shoujo Kaisen grammar adapts to it rather than normalizing it away.

---

# 1. AUTHORITY AND PRECEDENCE

## 1.1 Workflow authority

`Character_Creation.md` controls:

- phase boundaries;
- concept decomposition and fusion;
- mandatory concept materialization;
- number of candidates;
- frozen-state dependencies;
- body/outfit/weapon ownership;
- renderer handoff;
- validation and correction;
- presentation geometry;
- explicit render authorization.

This document never overrides those rules.

## 1.2 Style authority

When Shoujo Kaisen style is active, this document controls the **aesthetic design decisions left open by the workflow**, including:

- female-character appeal direction;
- proportion biases;
- body-shape resolution where several valid solutions exist;
- face and eye language;
- hair architecture;
- treatment of transformed anatomy;
- silhouette refinement;
- outfit construction language;
- coverage/open-region character;
- garment layering and ornamentation;
- weapon visual language;
- cross-phase cohesion;
- style-specific failure detection.

## 1.3 No reference-image dependency

After this document is active, the original Shoujo Kaisen screenshots are **evidence used to build the profile**, not operative references required for execution.

The profile must therefore be sufficiently explicit to execute without looking back at those images.

---

# 2. SHOUJO KAISEN CORE DESIGN DNA

## 2.1 Core identity

The style prioritizes **premium female gacha-character appeal** through a combination of:

- strong feminine silhouette readability;
- deliberate glamour;
- polished anime facial identity;
- soft or elegantly controlled body mass;
- high hair presence;
- body-aware costume construction;
- dramatic but readable ornamental systems;
- strong character-specific iconography;
- willingness to mix fantasy, East-Asian, gothic, celestial, demonic, mechanical, modern, sporty, aquatic, ceremonial, and other vocabularies without losing the same female appeal grammar.

The defining point is not one costume genre. The defining point is that wildly different themes are translated through a consistent **desirability-first premium heroine design language**.

## 2.1A HARD STYLE HIERARCHY — CORE BEFORE VOCABULARY

This hierarchy is mandatory whenever Shoujo Kaisen is active. It exists to prevent optional motifs from masquerading as the style itself.

### Tier 0 — defining structural identity

These properties carry the style and receive the highest design weight:

1. **stylized mobile-gacha female character readability at first glance;**
2. **strong, intentionally authored female body regime rather than mannequin anatomy;**
3. **clear anime face/eye identity and graphic head silhouette;**
4. **a small number of bold, readable whole-character masses;**
5. **body-aware glamour or attractiveness appropriate to the chosen body regime;**
6. **simple macro silhouette with strong shape separation;**
7. **character-specific iconography concentrated into a few memorable structures;**
8. **concept-driven transformation integrated without losing clean graphic readability.**

If these do not read clearly at thumbnail scale, the design is not Shoujo Kaisen-compatible merely because it contains franchise-associated motifs.

### Tier 1 — strong construction tendencies

Use after Tier 0 is already solved:

- body-framing outfit architecture;
- one or two large garment/armor/appendage masses;
- strong central-body versus peripheral-mass contrast;
- deliberate exposed/covered region rhythm where appropriate;
- clear hair massing;
- controlled asymmetry;
- compact clusters of secondary detail around focal points;
- one dominant theme/concept hook translated into shape, not token decoration.

### Tier 2 — conditional vocabulary only

The following may appear **only when justified by the active concepts, sub-concepts, or independently selected character direction**:

- gothic vocabulary;
- demonic vocabulary;
- celestial/divine vocabulary;
- East-Asian ceremonial vocabulary;
- samurai/warlord vocabulary;
- mechanical/technological vocabulary;
- aquatic vocabulary;
- seasonal/event vocabulary;
- gold trim;
- filigree;
- horns;
- wings;
- elaborate engraved metal;
- cathedral-like, occult, royal, sacred, or dark-fantasy motifs.

These are **never default style seeds**. Their presence cannot compensate for weak Tier-0 or Tier-1 construction.

### Tier 3 — default-generation prohibition

When applying Shoujo Kaisen to an already supplied/resolved concept set with no additional theme, vocabulary families require independent support from those concepts and authorized subordinate influences. Corpus frequency cannot add gothic, demonic, celestial, cathedral, ornate-gold, winged or horned identity.

Begin from the canonical concepts, their anchors and symbiotic physical constraints; apply structural Shoujo Kaisen aesthetic authorship inside that valid body-solution space. Any authorized subordinate choice remains governed by Character Creation and cannot replace the primary set.

## 2.2 Theme breadth / body-language consistency

The corpus supports broad thematic variation. A character may read as:

- xianxia / wuxia / East-Asian fantasy;
- divine / celestial;
- demonic / gothic;
- samurai / warlord-like;
- aquatic;
- technological / mechanical;
- modern / casual;
- sporty;
- seasonal / event-costume;
- ecclesiastical / ceremonial;
- dark fantasy;
- high fantasy;
- hybridized between several of these.

These themes are **not the style itself**. They are interchangeable subject vocabularies passing through the same design grammar.

## 2.3 Appeal modes

Shoujo Kaisen supports several recurring appeal modes that may be selected according to the concept fusion and the character's independent identity:

### A. Glamorous -glamorous

- elegant, controlled, imposing;
- elongated silhouette;
- facial poise;
- confident body architecture;
- sophisticated ornament and garment flow.

### B. Soft cute-glamorous

- petite or compact scale permitted;
- softer facial geometry;
- sweeter expression/eye framing;
- lighter apparent body mass;
- glamour remains present without requiring extreme proportions.

### C. Voluptuous / hyper-glamorous

- large or extreme chest-forward mass distribution;
- strong hip/thigh emphasis;
- pronounced torso-to-waist-to-hip contrast;
- body silhouette used as a primary visual hook.

### D. Concept-transformed premium heroine

- unusual anatomy or material transformation can be strong or extreme;
- the concept may radically change silhouette, topology, appendages, substrate, proportions, or joints;
- the design still receives Shoujo Kaisen face/hair polish, controlled glamour, elegant body reading, and premium heroine refinement rather than being normalized to a conventional mannequin.

No appeal mode is mandatory for every character. The designer selects the strongest mode for the resolved concept/body identity.

---

# 3. CROSS-PHASE STYLE INVARIANTS

These principles must remain visible from Phase 1 through Phase 3.

## 3.1 Character remains the primary focal system

Outfit and weapon may be spectacular, but the female character remains the dominant identity.

## 3.2 glamour is designed, not appended

glamour should emerge from proportion, silhouette, garment architecture, material contrast, open-region framing, posture-independent body readability, and character-specific visual rhythm. It must not depend on copying an display-oriented pose from a source illustration.

## 3.3 BOLD SIMPLE MACRO MASSES FIRST — HARD

The style must read through a few large graphic masses **before** any ornamental vocabulary is considered.

Preferred first read:

- body silhouette;
- head/hair silhouette;
- one or two dominant outfit/armor masses;
- one strong appendage or concept-transformation mass where applicable;
- one readable weapon silhouette in Phase 3.

Only after those masses are strong may secondary detail be added. Secondary detail is normally **clustered**, not distributed uniformly. Fine trim, functional straps/cords, engraved edges, restrained hardware, ribbons, or filigree are optional finishing vocabulary and must remain visually subordinate.

### Macro-detail ratio rule

At normal/mobile viewing scale, the character should be understandable from roughly **3–6 major shape groups**. Fine ornament may enrich those groups but must not create a new forest of equally weighted shapes.

If removing most micro-detail would destroy the character's identity, the design is under-authored at macro scale and fails.

## 3.4 Strong contrast between exposed/simple regions and authored detail

A recurring design rhythm is:

> **quiet body/skin or simple material region ↔ dense authored garment/ornament region**

This contrast is more characteristic than uniformly decorating every square centimeter.

## 3.5 Hair is an identity and silhouette system

Hair is commonly long or very long, with flowing masses that extend the silhouette and provide visual grandeur. This is a strong tendency, not a hard requirement.

Shorter hair is permitted when it is the strongest independent character solution, but it must compensate through equally strong head/face/silhouette authorship rather than falling back to generic anime hair.

## 3.6 Ornament is organized around hierarchy

Common visual tools include:

- gold or metallic edging;
- filigree;
- carved or engraved forms;
- ribbons and cords;
- tassels;
- beads;
- structured straps;
- decorative armor boundaries;
- feathers;
- horns;
- wings;
- East-Asian ornamental vocabulary;
- gothic or celestial motifs;
- mechanical framing.

These are vocabulary options, not mandatory tokens.

## 3.7 Theme does not overwrite body identity

The style may place the same female appeal grammar inside very different thematic systems. Therefore a theme must not turn the character into a costume mannequin or erase concept-driven anatomy.

## 3.8 NO THEME INVENTION FROM STYLE VOCABULARY — HARD

A Design Direction entry may contain examples of themes found in its source corpus, but those examples do not authorize the executor to invent one of those themes when the active Character Creation inputs do not call for it.

Shoujo Kaisen contributes **design treatment**, not an automatic narrative/theme generator. Gothic, demonic, celestial, samurai, aquatic, mechanical, ceremonial, or similar vocabularies enter only from the active character direction, concepts, sub-concepts, or another explicitly authorized source.

## 3.9 MOBILE-GACHA READABILITY TEST — HARD

Before accepting any phase design, inspect it conceptually at small mobile-card scale. The first read must remain:

1. character body and overall silhouette;
2. face/head/hair identity;
3. one or two dominant design structures;
4. outfit or weapon category;
5. only then secondary ornament.

A design whose first read is `filigree`, `gothic cathedral`, `gold decoration`, dense suspended ornament, or another detail/theme token has inverted the hierarchy and must be simplified.

---

# 4. PHASE 1 — CHARACTER / BODY / IDENTITY DESIGN GRAMMAR

## 4.1 Phase-1 purpose under this style

Phase 1 must produce a body and identity that would already read as Shoujo Kaisen-compatible **before a dedicated outfit or weapon exists**.

The style must therefore be visible through:

- proportion choices;
- mass distribution;
- face geometry;
- eye construction;
- hair architecture;
- silhouette;
- transformed-anatomy refinement;
- material treatment;
- identity finishing.

It must not depend on Phase-2 clothing to create the style retroactively.

## 4.2 Body-range principle

The corpus shows frequent voluptuous bodies, but this is a **strong frequency bias, not a universal anatomy lock**.

Supported Phase-1 regimes include, among others:

- petite + petite chest;
- petite + moderate curves;
- slim / elegant;
- slim-curvy;
- balanced;
- voluptuous;
- hyper-voluptuous;
- tall / elongated;
- concept-driven muscular or structurally powerful;
- concept-driven transformed / altered / unusual bodies.

The active concept fusion and independent character identity determine which body regime is appropriate.

### Critical rule

Do not forcibly enlarge the chest, narrow the waist, soften musculature, lengthen the legs, or restore conventional anatomy when doing so would weaken concept fusion or erase the independently resolved body regime.

Instead, translate the Shoujo Kaisen style through **polish, proportion coherence, glamorous framing, face/hair quality, elegant mass transitions, and controlled silhouette hierarchy** appropriate to that body.

## 4.3 Common proportion biases

When the concept set leaves several equally valid options, Shoujo Kaisen tends to favor:

- feminine torso/waist/hip relationships;
- visually intentional chest architecture rather than neutral mannequin anatomy;
- long or visually prominent legs;
- deliberate thigh/hip mass;
- narrower waist where compatible;
- graceful limb taper;
- smooth transition between major body masses;
- low-to-moderate visible musculature in ordinary bodies;
- strong overall curvature or elegant line flow.

These are selection biases, not obligations.

## 4.4 Petite-body grammar

A petite Shoujo Kaisen character remains:

- fully resolved;
- fully authored;
- premium-gacha in finish;
- capable of petite chest geometry;
- capable of compact limbs and reduced mass;
- distinct in head/body relationship and facial architecture;
- glamorous through proportion harmony, surface treatment, hair, eyes, silhouette, and later clothing rather than forced voluptuousness.

Petite must not automatically mean oversized head, mascot-like body, mascot compression, or generic moe proportions.

## 4.5 Voluptuous-body grammar

When a voluptuous direction is selected, common style treatment includes:

- strong chest projection;
- pronounced chest/waist contrast;
- broad or projected hip/lower-torso mass;
- substantial thighs;
- soft surfaces and smooth mass transitions;
- visually controlled rather than randomly exaggerated anatomy;
- sufficient limb length and torso coherence to preserve premium heroine proportions.

## 4.6 Muscular / powerful / broad concept-driven bodies

If concepts produce a muscular, broad, powerful, dense, armored, or otherwise non-corpus-typical body:

- preserve that body regime;
- do not feminize it by merely shrinking shoulders or removing muscle;
- establish Shoujo Kaisen compatibility through attractive female head/face design, strong eye language, deliberate hair, coherent waist/hip architecture where physically compatible, polished surfaces, and glamour-aware silhouette rhythm;
- keep the transformation aesthetically authored rather than realistic-bodybuilding generic.

The style adapts to the body rather than rejecting it.

## 4.7 Heavily transformed / altered body grammar

For characters with extreme concept-driven morphology—extra limbs, wings, tails, horns, cavities, branching systems, unusual joints, mineral/organic replacement, segmented bodies, nonstandard substrates, asymmetric masses, internal openings, tentacular structures, or other transformations—apply the following:

1. **Preserve the transformation at full Character-Creation strength.**
2. **Keep female character identity deliberate rather than accidental.**
3. **Organize exotic anatomy into elegant macro masses.**
4. **Use controlled curvature and directional flow rather than random protrusion.**
5. **Keep face/head/hair among the strongest identity anchors unless the concept genuinely transforms them.**
6. **Use high-quality transitions between normal and transformed matter.**
7. **Prefer a few strong transformed systems over decorative mutation noise.**
8. **When appendages are large, use them as silhouette architecture rather than background spectacle.**
9. **Do not copy monster companions from source illustrations as substitute morphology.**
10. **Do not normalize unusual topology merely to imitate the mostly humanoid reference roster.**

## 4.8 Face grammar

Common tendencies:

- polished anime facial construction;
- smooth cheek planes;
- tapered or refined jaw;
- relatively compact lower face;
- clear feminine eye/brow framing;
- attractive or heroine appearance;
- strong expression potential even when Phase-1 presentation remains neutral.

Faces must not converge into one stock template.

## 4.9 Eye grammar

Typical Shoujo Kaisen eye design uses:

- large but not chibi-scale eyes;
- high iris visibility;
- polished lashes;
- strong eye-color identity;
- controlled highlights;
- visually clean anime structure;
- occasional unusual iris/sclera treatment when conceptually appropriate.

The eyes should support glamour, elegance, cuteness, danger, or supernatural presence according to the individual character.

## 4.10 Hair grammar

Strong tendencies:

- long to extremely long hair;
- large flowing grouped masses;
- layered ribbons/locks rather than hair-as-one-flat-shape;
- hair used to enlarge silhouette and reinforce motion potential;
- strong value/color separation from body and major appendages;
- occasional braids, tied sections, ornaments, twin sections, or asymmetric framing.

Shorter hair is allowed but must retain equally deliberate architecture.

## 4.11 Skin and body-material treatment

Conventional skin tends toward:

- clean, smooth anime surfaces;
- soft value transitions;
- high material cleanliness;
- controlled highlights;
- glamorous but illustrated rather than photorealistic treatment.

Concept-transformed skin/material may become:

- crystal;
- metal;
- feather;
- scale;
- bone-like;
- translucent;
- liquid-like;
- luminous;
- mechanical;
- void-like;
- other Character-Creation-compatible matter.

When transformed, retain clean material boundaries and premium illustrative clarity.

## 4.12 Nonhuman feature treatment

Commonly compatible additions include:

- horns;
- elf-like ears;
- wings;
- tails;
- antlers;
- feathers;
- scales;
- claws;
- demonic/celestial cranial structures;
- mechanical appendages;
- tentacular or serpentine structures when concept-driven.

These should be integrated into the character's identity, not pasted on as accessories.

## 4.13 Phase-1 Shoujo Kaisen style test

A Phase-1 candidate passes the style test when, without outfit or weapon:

- she reads as a premium anime/gacha heroine;
- body regime is deliberately authored rather than default;
- proportions have coherent glamorous/elegant design logic appropriate to her regime;
- face and eyes are polished and character-specific;
- hair has deliberate silhouette function;
- transformed anatomy, if present, is elegant and organized rather than noisy;
- the body is visually attractive/compelling without requiring source-pose imitation;
- no source character has been copied;
- the design still reads when almost all decorative micro-detail is mentally removed;
- no optional theme vocabulary is doing the work that body/head/silhouette authorship should do.

Failure examples:

- generic anime mannequin with Shoujo Kaisen colors;
- forced exaggerated chest scale on a concept/petite body that does not support it;
- normalized humanoid anatomy that erases concept transformations;
- monster body with a generic anime face pasted on;
- stock long-hair voluptuous template repeated across candidates;
- reference illustration pose/composition imported into Phase 1.

---

# 5. PHASE 2 — OUTFIT DESIGN GRAMMAR

## 5.0 Domain breadth

Phase 2 has no fixed historical or technological ceiling. Historical, fantasy, contemporary, futuristic, ceremonial, military, street/fashion, technical, formal, armored and hybrid garments are all valid. Armor, partial armor, protective shells and rigid wearable systems are clothing-domain options when they remain clearly separable from the frozen body. Fixed integrated ornament is also valid; garment quality is judged by architecture, identity and material hierarchy rather than by ornament count.

## 5.1 Phase-2 purpose under this style

A Shoujo Kaisen outfit should feel deliberately authored to **showcase the exact frozen character**, not merely dress her.

The outfit typically combines:

- strong body framing;
- intentional open regions;
- elegant or dramatic silhouette extensions;
- ornate secondary structure;
- material contrast;
- themed ornament;
- high glamour / display-forward styling tolerance;
- readable major garment masses.

The frozen body remains authoritative.

## 5.2 Core outfit thesis

A typical Shoujo Kaisen outfit is best understood as:

> **BODY-FRAMING CORE + 1–3 BOLD GARMENT/ARMOR MASSES + CHARACTER-SPECIFIC SHAPE HOOK + OPTIONAL CLUSTERED ACCENTS**

Theme is carried through the active concepts/sub-concepts and the chosen character direction; it is **not generated from the Shoujo Kaisen style vocabulary itself**.

The outfit should clearly reveal why it belongs to this exact character. Its first read should come from large garment boundaries, exposed/covered region design, shoulder/torso/hip architecture, and silhouette—not from trim density.

## 5.3 Garment-extent philosophy

The style strongly supports deliberate open-region architecture, but coverage varies widely.

Common exposed or visually framed regions include:

- upper chest;
- upper-torso opening region;
- shoulders;
- upper back;
- side torso;
- abdomen;
- waist;
- hip line;
- upper thighs;
- legs.

The open regions should be **architected**, not accidental.

Typical methods:

- cropped upper components;
- deep neckline structures;
- open side panels;
- high-cut lower-body construction;
- split skirts/panels;
- thigh openings;
- detached sleeves;
- asymmetric coverage;
- harness-like framing;
- partial armor framing;
- translucent layers;
- bodysuit cut geometry;
- narrow central panels;
- draped fabrics that expose rather than bury the body.

Not every outfit must be highly open. A more covered outfit can still fit the style when silhouette, fitted construction, body framing, material contrast, and ornament maintain the same glamour grammar.

## 5.4 Garment-family vocabulary

Commonly compatible garment systems include:

- fitted bodysuit / leotard-like foundations;
- qipao/cheongsam-adjacent or East-Asian ceremonial derivations;
- kimono/yukata/robe-derived hybrids;
- split robes;
- fantasy dresses with major openings;
- detached sleeve systems;
- corset/bodice structures;
- harness/strap systems;
- thigh-high stocking systems;
- asymmetric skirts and hanging panels;
- loincloth/panel hybrids;
- partial armor;
- shoulder armor + open torso combinations;
- gothic fitted fantasy clothing;
- celestial or priestess-like drapery;
- modern oversized-shirt/casual hybrids;
- seasonal eventwear;
- sporty event variants;
- mechanical or techwear hybrids;
- capes, mantles, and large rear panels used as framing masses.

These are a vocabulary, not a fixed menu. They are also **not defaults**: a garment family is selected because it fits the frozen body, active concepts/sub-concepts, and individual character direction. Do not choose a gothic, ceremonial, celestial, or East-Asian family merely because it is listed here.

## 5.5 Body-specific tailoring

The outfit must adapt strongly to the body regime.

### Petite body

Favor:

- smaller garment scale;
- lighter component mass;
- sharper control of proportion so clothes do not overwhelm the body;
- delicate or compact openings;
- strong leg/waist/shoulder framing appropriate to the character;
- ornament scaled to the body.

Do not compensate for a petite chest by forcing a chest-emphasis outfit.

### Voluptuous body

Favor:

- structurally credible support/framing;
- garment edges that reinforce chest/waist/hip rhythm;
- large open areas balanced by strong garment anchors;
- substantial panels/sleeves/armor masses that visually balance the body;
- controlled rather than chaotic strap density.

### Muscular / broad / powerful transformed body

Favor:

- garment architecture that acknowledges shoulder, ribcage, muscle, or structural mass;
- strong openings and attachments that make the body look intentionally powerful and glamorous;
- asymmetry or armor systems that reinforce rather than hide strength;
- avoid shrinking the outfit into generic minimal fitted clothing if the body demands stronger structure.

### Heavily transformed body

The outfit must visibly accommodate:

- appendage roots;
- unusual joints;
- extra limbs;
- cavities;
- topological openings;
- transformed surfaces;
- tails;
- wings;
- horns;
- fins;
- mechanical systems;
- other concept-derived anatomy.

Use openings, split construction, dedicated collars, articulated interfaces, floating-over-body garment frames only when physically wearable, segmented components, or asymmetrical routing.

Do not cover the defining transformation merely to make the character resemble a conventional source heroine.

## 5.6 Silhouette architecture

Common Shoujo Kaisen outfit silhouette tools:

- long trailing panels;
- detached sleeves;
- asymmetric shoulder masses;
- broad cape/mantle framing;
- ribbons/cords/tassels;
- thigh-high leg lines;
- narrow waist framing;
- split lower-body fabrics;
- large but organized armor pieces;
- long fabric tails;
- exposed central body axis surrounded by more complex outer masses.

A useful recurring rhythm is:

> **simple/visible central body ↔ larger detailed peripheral garment masses**

## 5.7 Material language

Frequently compatible materials include:

- silk-like fabric;
- satin-like illustrated fabric;
- soft matte cloth;
- translucent fabric;
- lace-like systems;
- leather;
- glossy dark bodysuit material;
- polished metal;
- engraved gold/brass-like accents;
- painted or enameled armor;
- crystal;
- glass-like structures;
- feathers;
- fur trim;
- mechanical composites;
- luminous bounded materials.

Material contrast should be deliberate. A typical outfit may combine a soft textile base with a small number of rigid, glossy, metallic, translucent, or luminous accents.

## 5.7A Flat-surface costume rendering

Phase-2 garments favour broad continuous 2D material surfaces. A major cloth, leather, knit or composite panel should normally read as one clear colour/value mass with a small number of fold/shadow/highlight groups. Dense prismatic faceting, repeated beveled rectangles and shard-like tessellation are not default costume detail. Use them only when a resolved material specifically requires faceting, and localize them so the garment plane remains readable.

When the Phase-1 body is already highly plated, scaled, crystalline or fragmented, use simpler clothing surfaces by default to strengthen body/outfit separation. The garment can share colours with the body while still differing through seams, hems, folds, panel scale, surface continuity and highlight language.

## 5.8 Ornament grammar

Ornament is **conditional finishing**, not a core style carrier. A passing outfit may be comparatively plain if its body framing, macro architecture, proportions, silhouette, and identity hook are strong.

When ornament is appropriate, prefer:

- one or two motif families at most;
- clustering around focal junctions, closures, borders, or major structural transitions;
- clean unornamented regions between clusters;
- character-specific symbolic geometry derived from the concept fusion.

Possible finishing vocabularies include seam/border rhythms, functional fasteners, restrained engraved rigid inserts, lace-like textile borders, tech construction accents, feather-shaped fabric cuts, or theme-specific panel geometry **only when independently justified**.

Phase 2 does not use standalone jewelry or suspended decorative accessory systems as costume vocabulary. Ornament remains integral to garment panels, seams, edges or functional construction. Avoid equal-detail saturation everywhere. If finishing detail becomes the first read, reduce it.

## 5.9 Color architecture

The corpus often uses strong, clean palette identities such as:

- black + red + gold;
- white + blue/cyan + silver;
- white + gold + warm accent;
- purple/black + luminous accent;
- red/white + gold;
- teal/white + metallic detail;
- dark neutral + saturated hair/accent color.

These are examples of **palette construction**, not mandatory palettes.

Preferred logic:

- 1 dominant garment family color;
- 1 secondary structural color;
- 1–2 accent colors;
- metallic or luminous micro-accents where useful;
- enough contrast to preserve body/garment separation.

## 5.10 Body-emphasizing garment grammar

The style permits strongly body-conscious clothing, but the clothing must remain structurally real.

Useful mechanisms include:

- fitted tops with clear textile panels and seams;
- cropped upper garments;
- sleeveless/high-collar combinations;
- side-cut or asymmetric garment boundaries;
- short skirts or shorts;
- high slits in actual skirt/robe panels;
- open-back constructions;
- stockings, thigh-highs or other fitted legwear;
- oversized sleeves paired with a substantial central garment;
- fitted waist/hip shaping through seams and tension;
- translucent secondary cloth layers used as real fabric, not atmosphere;
- contrast between quiet body/skin regions and large coherent garment masses.

The frozen body may remain highly readable and garment surface area may be limited. What matters is that every visible garment piece has clear clothing substance: panel mass, thickness, seam/hem logic, support, fold/tension behavior and material identity.

The renderer-facing blueprint expresses the exact garment geometry positively rather than repeatedly describing the amount of body left visible.

## 5.11 Phase-2 transformed-body adaptation rule

When Phase 1 produces a radically altered body, the outfit must preserve the same Shoujo Kaisen principles **through the altered anatomy itself**.

Examples:

- a four-armed character receives a deliberately authored four-arm sleeve/cuff/garment hierarchy rather than hiding the extra arms;
- a crystalline torso receives fitted frames and fabric anchors around the crystal structure rather than covering it with a generic dress;
- a broad muscular character receives powerful open armor/drape architecture rather than being reshaped into a slim source-like body;
- a winged character receives rear openings and front/side framing that preserve wing-root clarity;
- a hollow/void torso receives garment boundaries that frame the aperture rather than crossing or relocating it.

## 5.12 Phase-2 style test

An outfit passes when:

- it could plausibly belong to the Shoujo Kaisen design ecosystem without copying an existing outfit;
- it is unmistakably tailored to the frozen body;
- it uses deliberate garment-boundary and negative-space architecture;
- body and outfit remain visually separable;
- large garment masses are readable;
- secondary ornament is rich but subordinate;
- theme vocabulary is coherent;
- transformed anatomy is accommodated rather than hidden;
- the character remains visually dominant;
- glamour/glamour is appropriate to the individual body rather than applied as one template;
- the outfit remains recognizable after mentally stripping away minor trim, surface motifs and non-structural finishing;
- its first read is bold garment architecture rather than fantasy ornament density.

Failure examples:

- generic thin-frame outfit whose identity depends on trim instead of substantial garment construction;
- generic full armor that hides the character;
- ten outfits that are the same bodice with different ornaments;
- clothing copied from a source screenshot;
- clothes that normalize or conceal unusual anatomy;
- excessive straps/details with no macro garment architecture;
- outfit whose only Shoujo Kaisen cue is one upper-torso opening.

---

# 6. PHASE 3 — WEAPON DESIGN GRAMMAR

## 6.1 Phase-3 purpose under this style

The weapon should feel like a premium collectible signature object belonging specifically to the frozen Shoujo Kaisen-style character and outfit.

The style favors:

- strong category readability;
- bold, graphic, economical macro silhouette;
- one memorable secondary construction hook;
- character-linked material/color language;
- occasional oversized scale when it improves the character silhouette;
- optional clustered micro-detail that never competes with category shape.

A weapon does **not** become more Shoujo Kaisen-like by becoming more ornate.

## 6.2 Weapon-category range

The style can support any Character-Creation-valid recognizable category when it fits the character, including:

- swords;
- paired blades;
- greatswords;
- daggers;
- spears;
- lances;
- glaives;
- halberds;
- polearms;
- scythes;
- bows;
- staves;
- axes;
- hammers;
- whips;
- chains;
- shields as primary weapon systems;
- mechanical/futuristic recognizable weapon categories;
- coherent hybrids that retain category readability.

Do not reduce Shoujo Kaisen weapon design to “ornate sword.”

## 6.3 Macro silhouette

Preferred macro characteristics:

- instantly readable weapon type;
- elegant directional flow;
- strong long-axis geometry for blades/polearms where appropriate;
- controlled curves or pointed forms;
- a few large intentional masses rather than many competing projections;
- scale chosen in relation to the character, not universally oversized.

Large weapons are common-compatible because they create dramatic collectible-character silhouettes, but compact weapons are equally valid when they fit the individual character.

## 6.4 Secondary-detail language

Secondary detail is optional and chosen **after** the weapon category, macro silhouette, scale, and one signature construction hook are frozen.

Compatible systems may include engraved borders, layered guards, insets, negative-space apertures, nested outlines, fine channels, cords, crystal segments, mechanical joints, or theme-specific ornament. Filigree, horn/wing forms, East-Asian linework, celestial arcs, gemstones, and similar motifs are conditional vocabulary only when independently justified by the active character/theme.

Do not select any of these merely because the design direction is Shoujo Kaisen.

## 6.5 Material language

Commonly suitable:

- polished dark metal;
- silver/white metal;
- warm gold/brass-like metal;
- red enamel/lacquer-like surfaces;
- crystal;
- translucent glass-like material;
- bone-like structural matter;
- mechanical composite;
- luminous bounded material;
- concept-derived coherent materials inherited from the character.

A weapon should normally use a small hierarchy of materials rather than an undifferentiated fantasy mixture.

## 6.6 Character/outfit echo

A Shoujo Kaisen signature weapon should echo **selected** identity systems from the character, such as:

- the curvature of horns or appendages;
- a dominant body aperture/negative-space geometry;
- hair-flow direction;
- outfit panel angles;
- trim profile;
- material contrast;
- one important accent color;
- celestial/demonic/mechanical/East-Asian structural vocabulary;
- concept-derived surface segmentation.

Do not copy every motif. One or two strong physical relationships are preferable to exhaustive repetition.

## 6.7 Weapon + transformed body

The weapon must adapt to unusual bodies rather than forcing conventional anatomy.

Examples:

- extra arms may justify paired or multi-interface weapons, but only when the selected weapon category remains clear;
- large claws or unusual hands require corresponding grip geometry;
- very petite bodies may receive proportionally delicate or deliberately oversized weapons according to visual balance;
- massive/muscular bodies may support denser or heavier weapon silhouettes;
- winged/tail-heavy silhouettes require weapon placement that does not visually collide with dominant appendages;
- strange materials can migrate into the weapon as actual structure while remaining physically separate from the body.

## 6.8 Scale relationship

Weapon scale should be chosen from character-specific composition logic:

### Petite / delicate character

Possible solutions:

- compact elegant weapon;
- long slender weapon that extends verticality;
- intentionally oversized contrast weapon if the character can still visually dominate.

### Voluptuous / glamorous character

Possible solutions:

- long spear/glaive;
- dramatic sword;
- ornate bow;
- elegant large weapon that counterbalances the body's broad mass without obscuring it.

### Muscular / powerful character

Possible solutions:

- broad blade;
- heavy polearm;
- axe/hammer;
- dense mechanical weapon;
- other strong category whose mass matches the body.

### Highly transformed character

Select the category and scale that best interacts with the transformation without duplicating or visually merging into it.

## 6.9 Weapon ornament hierarchy

Use:

> **clean macro category → strong secondary construction → intricate micro ornament**

Avoid:

- giant decorative wings that erase category identity;
- random spikes;
- multiple floating rings with no structural role;
- emitted effects defining the weapon's basic shape;
- motif soup;
- ornament large enough to compete with the blade/head/limbs of the weapon.

## 6.10 Phase-3 style test

A weapon passes when:

- category is instantly readable;
- macro silhouette is clean and elegant/imposing;
- internal detail feels premium and collectible;
- materials are coherent;
- ornament is hierarchical;
- at least one substantive physical relationship ties it to the character/outfit;
- it feels specific to the character rather than generic inventory;
- it does not copy a source weapon;
- it preserves physical separation from body/outfit;
- it remains Shoujo Kaisen-compatible even if isolated on white.

Failure examples:

- generic weapon with matching palette only;
- unreadable fantasy mass;
- identical weapon template repeated across characters;
- source weapon copied directly;
- weapon that becomes an extra anatomical limb;
- weapon whose style identity depends on emitted aura rather than construction.

---

# 7. CROSS-PHASE COHERENCE COMPILER

## 7.1 Phase-1 → Phase-2

The outfit should inherit the **character's resolved design logic**, not merely the raw concept words.

Extract from the frozen character:

- body regime;
- dominant silhouette;
- face/hair identity;
- important anatomical openings/appendages;
- material hierarchy;
- asymmetry;
- color/value architecture;
- strongest glamorous/elegant/powerful design direction.

Then design clothing that frames, extends, contrasts, or supports those exact properties.

## 7.2 Phase-2 → Phase-3

The weapon should inherit:

- the character's scale and body interface;
- outfit edge language;
- major material relationships;
- selected accent colors;
- one or two structural motifs;
- the complete silhouette balance.

Do not simply match the outfit palette.

## 7.3 Full character read

At the end of Phase 3, the three systems should read as:

> **one Shoujo Kaisen-style character identity expressed through body + outfit + weapon**

not:

> generic body + glamorous costume + ornate weapon.

---

# 8. STYLE-SPECIFIC DECISION PRIORITIES

When several Character-Creation-valid solutions remain, prefer the solution that maximizes the following in order:

1. preservation of mandatory concept/body identity;
2. unmistakable stylized mobile-gacha female-character read;
3. bold whole-character silhouette and 3–6 major shape groups;
4. character-specific body regime and attractiveness/appeal appropriate to that regime;
5. strong anime face/eye/head identity;
6. coherent hair architecture without using hair length as a shortcut;
7. elegant handling of unusual morphology;
8. body-aware outfit architecture with large readable masses;
9. clear open/covered region design where appropriate;
10. one or two character-specific iconic structures;
11. signature weapon macro specificity;
12. cross-phase cohesion;
13. clustered secondary ornament;
14. micro-detail.

**Theme vocabulary, gold trim, filigree, gemstones, horns, wings, gothic/celestial/demonic cues, and similar tokens have no priority rank at all unless independently justified by the active character inputs.**

Micro-detail never rescues weak macro design.

---

# 9. ANTI-DRIFT RULES

The following indicate drift away from the style:

## 9.1 Generic anime drift

Symptoms:

- default mannequin body;
- stock face;
- generic hair;
- ordinary fantasy costume;
- weapon chosen independently of the character.

Correction:

- strengthen body regime, face/hair identity, garment framing, and signature-object relationships.

## 9.2 Single-template voluptuous drift

Symptoms:

- all characters receive same tall hourglass body;
- same exaggerated chest geometry;
- same long hair;
- same thigh-high / loincloth costume structure.

Correction:

- preserve Shoujo Kaisen grammar while radically varying body regime, concept transformation, face, hair, and outfit architecture.

## 9.3 Costume-only style drift

Symptoms:

- Phase 1 body is generic;
- style appears only after adding revealing clothing.

Correction:

- rebuild Phase-1 identity so proportions, face, hair, body transformation, and silhouette already carry the style.

## 9.4 Illustration-copy drift

Symptoms:

- source pose reproduced;
- source background/companion reused;
- source outfit or weapon reconstructed;
- character resembles one specific source rather than the design family.

Correction:

- return to abstract grammar and independently author all systems.

## 9.5 Concept-normalization drift

Symptoms:

- transformed bodies are pulled back toward ordinary glamorous humanoids because the source corpus is mostly humanoid.

Correction:

- restore full concept-driven body architecture and apply the style through refinement rather than normalization.

## 9.6 Theme-token / ornate-fantasy drift — HARD FAIL

Symptoms:

- “Shoujo Kaisen” reduced to gold trim, Chinese knots, long hair, filigree, gemstones, horns, wings, gothic arches, celestial halos, demonic motifs, or other corpus tokens;
- a random character receives a specific gothic/celestial/demonic/ceremonial theme without an independent input requiring it;
- minor ornament or suspended adornment becomes more visually important than body/outfit macro shapes;
- costume reads as high-detail fantasy couture rather than graphic mobile-gacha character design;
- weapon reads as ornate fantasy illustration prop rather than bold collectible-character equipment.

Correction:

1. remove all unjustified theme tokens;
2. reduce the design to body + head/hair + 1–3 major outfit masses + one identity hook;
3. verify the silhouette at thumbnail scale;
4. reintroduce only the minimum secondary detail that materially improves the character;
5. re-add theme-specific vocabulary only when the active concepts/sub-concepts/character direction independently justify it.

## 9.7 HIGH-DETAIL FANTASY-ART DRIFT

Symptoms:

- realistic or semi-realistic fashion anatomy replaces stylized anime/gacha proportions;
- costume complexity is achieved through many thin decorative layers instead of bold graphic components;
- the character would fit a generic ornate fantasy illustration with the franchise label removed;
- sophistication is equated with ornament count.

Correction:

- simplify component count;
- enlarge and clarify the dominant outfit shapes;
- strengthen anime face/head identity;
- preserve stronger graphic value/color blocks;
- cluster detail instead of spreading it;
- treat `premium` as **clarity + specificity + appeal**, not detail density.

---

# 10. EXECUTION INTERFACE FOR CHARACTER CREATION

When the user supplies a concept set with:

`Design Style: Shoujo Kaisen`

apply the following silently during design resolution.

## 10.1 Phase 1 interface

After concept fusion but before final body freeze:

1. resolve the independent body regime demanded by the concepts;
2. apply Shoujo Kaisen proportion/appeal biases only where they do not weaken the concepts;
3. author premium face/eye/hair identity;
4. refine transformed anatomy into elegant macro masses;
5. establish glamorous/cute/powerful/heroic appeal appropriate to that body;
6. run the Phase-1 Shoujo Kaisen style test;
7. preserve five-body independence under the Character Creation workflow.

## 10.2 Phase 2 interface

After the body is frozen:

1. identify the body's strongest anatomical and silhouette anchors;
2. choose a Shoujo Kaisen-compatible garment family that fits this exact body;
3. resolve garment boundaries and negative-space layout deliberately;
4. frame rather than hide concept-derived anatomy;
5. freeze 1–3 strong garment masses and one character-specific shape hook before adding ornament;
6. add only clustered secondary detail justified by the character/theme;
7. apply appropriate material contrast and palette hierarchy;
8. run the Phase-2 Shoujo Kaisen style test;
9. maintain full candidate differentiation required by Character Creation.

## 10.3 Phase 3 interface

After body + outfit are frozen:

1. select the best weapon category from the actual character;
2. preserve simple recognizable macro category;
3. translate one or two character/outfit structures into the weapon;
4. freeze one memorable secondary construction hook;
5. add only optional clustered micro-detail justified by that hook/theme;
6. select scale appropriate to the body and total silhouette;
7. maintain coherent material/color hierarchy;
8. run the Phase-3 Shoujo Kaisen style test;
9. maintain full candidate differentiation required by Character Creation.

---

# 11. STANDALONE SUFFICIENCY TEST

This document is considered sufficient only if an executor who has:

- `Character_Creation.md`;
- the active concept combination;
- this Shoujo Kaisen design-style document;

but **none of the original Shoujo Kaisen screenshots**, can still consistently author:

- Phase-1 bodies that read as belonging to the design family;
- Phase-2 outfits that extend that same family;
- Phase-3 weapons that complete the same family;
- transformed bodies without normalizing them;
- multiple distinct characters without collapsing to a template.

If repeated tests require the original screenshots to recover the style, this document is insufficient and must be deepened.

---

# 11A. WEIGHTED SELF-CHECK BEFORE FREEZE

Before freezing any Shoujo Kaisen body, outfit, or weapon, answer these in order:

1. **Would the design still read correctly with all gold, filigree, gems, cords, tassels, symbols, and tiny ornament removed?** If no: FAIL.
2. **Does the first read come from body/head/silhouette and a few major structures?** If no: FAIL.
3. **Was any gothic/celestial/demonic/East-Asian/mechanical/etc. vocabulary introduced solely because it appears in this document?** If yes: remove it.
4. **Does the design look like a stylized mobile-gacha character rather than generic ornate fantasy art?** If no: simplify and strengthen graphic shape language.
5. **Is premium quality being expressed through specificity and silhouette rather than ornament count?** If no: rebuild.
6. **At thumbnail scale, can the character be distinguished before micro-detail is visible?** If no: rebuild.

Only after these six checks pass may the ordinary phase validation continue.

---

# 12. VALIDATION CHECKLIST

## Phase 1

- [ ] Body regime independently authored from concepts
- [ ] Style applied as grammar, not anatomy restriction
- [ ] Face/eyes/hair premium and specific
- [ ] Silhouette has intentional feminine/heroine appeal
- [ ] Petite chest permitted where appropriate
- [ ] Voluptuous direction permitted where appropriate
- [ ] Muscular/transformed bodies preserved when concept-driven
- [ ] Unusual morphology organized elegantly
- [ ] No source character copied
- [ ] Style visible without outfit/weapon

## Phase 2

- [ ] Outfit designed for exact frozen anatomy
- [ ] Open/covered regions deliberate
- [ ] Character remains dominant
- [ ] Garment macro masses readable
- [ ] Ornament subordinate and coherent
- [ ] Material hierarchy clear
- [ ] Transformed anatomy accommodated, not hidden
- [ ] Glamour/display-forward styling appropriate to body regime
- [ ] No source outfit copied
- [ ] Outfit reads Shoujo Kaisen-compatible in isolation

## Phase 3

- [ ] Category immediately readable
- [ ] Macro simple and strong
- [ ] Secondary/micro detail premium
- [ ] Scale tailored to character
- [ ] Grip/interface fits frozen body/outfit
- [ ] Material architecture coherent
- [ ] One or two substantive character/outfit echoes
- [ ] Weapon remains physically separate
- [ ] No source weapon copied
- [ ] Weapon reads Shoujo Kaisen-compatible in isolation

## Full character

- [ ] Body + outfit + weapon read as one authored identity
- [ ] Concepts remain dominant factual source of character construction
- [ ] Style is recognizable without relying on raw franchise label
- [ ] No phase uses a generic fallback
- [ ] No phase copies source illustration content

---

# 13. CORPUS NOTES AND CONFIDENCE

## High-confidence recurring grammar

- premium female gacha identity;
- strong glamorous/glamorous bias;
- high tolerance for display-forward styling;
- polished anime face/eye treatment;
- strong hair presence, often long;
- clear body silhouette and deliberate mass distribution;
- frequent chest/waist/hip emphasis in ordinary humanoid bodies;
- frequent thigh/leg emphasis;
- broad thematic flexibility;
- body-framing garments;
- intentional open-region clothing;
- flowing panels/sleeves/ribbons/cords;
- organized ornate secondary detail;
- optional metal/crystal/engraving/filigree vocabulary when independently theme-appropriate;
- dramatic but readable signature objects/weapons;
- contrast between clean body regions and dense authored detail.

## Important non-restrictions

The following are **not prohibited** merely because less common in the corpus:

- petite chest;
- petite body;
- muscular body;
- broad body;
- short hair;
- unusual proportions;
- strong asymmetry;
- extra limbs;
- monster-derived appendages;
- cavities/openings;
- nonstandard substrates;
- radically transformed bodies.

When present because of concept fusion, they must be rendered through the Shoujo Kaisen grammar rather than erased.

---

# 14. VERSION HISTORY

## v1.1

- Corrected an empirical failure exposed by a standalone random-character test: the v1.0 document allowed optional gothic/demonic/celestial/gold/filigree vocabulary to receive too much implicit weight.
- Added a hard style hierarchy separating defining structural identity from conditional vocabulary.
- Made stylized mobile-gacha readability, bold simple macro masses, anime head identity, body-aware appeal, and concentrated iconography the primary carriers.
- Prohibited theme invention from corpus vocabulary when the active character inputs do not justify that theme.
- Reframed ornament as conditional clustered finishing rather than a core style carrier.
- Added macro-detail ratio guidance and thumbnail/mobile-card readability tests.
- Strengthened Phase-2 and Phase-3 execution order so garment/weapon macro architecture freezes before ornamental detail.
- Added hard failure modes for ornate-fantasy drift and high-detail-fantasy-art drift.
- Added a weighted self-check that explicitly removes token vocabulary to verify standalone structural fit.

## v1.0

- Expanded the former Phase-1-only profile into a complete Phases 1–3 design-style specification.
- Reframed the document from descriptive tendency notes into an executable style grammar.
- Added explicit transformed-body adaptation for Phase 1.
- Added dedicated Phase-2 outfit grammar compatible with frozen-body and unusual-anatomy requirements.
- Added dedicated Phase-3 weapon grammar compatible with simple-macro / intricate-micro and character-specific weapon authorship.
- Added cross-phase coherence compiler, anti-drift rules, standalone sufficiency test, and phase-specific validation.
- Explicitly corrected chest-scale overinterpretation: petite + petite chest and other body regimes are valid.
- Established this file structure as the intended model for one complete document per design style.



<!-- END EMBEDDED DD0001 SPECIFICATION -->

---

## DD0002 — FATE

**Registry status:** ACTIVE / COMPLETE  
**Current entry version:** 1.0  
**Source corpus:** 9 user-supplied sets × 10 references = 90 references, fully processed  
**Phase coverage:** Phase 1 + Phase 2 + Phase 3  
**Activation:** available as a standalone Design Direction under `Character_Creation.md`.

**Provenance:** the complete intake protocol, ninety individual reference records, provisional hypotheses and compilation contract are preserved in `provenance/Design_Direction_Construction_History.md`. They are audit/revision sources, not ordinary-generation rules. Future revisions must still follow their non-destructive evidence-preservation protocol.

# Fate — Complete Character Design Direction Specification

**Entry ID:** DD0002  
**Entry version:** 1.0  
**Corpus basis:** 90 user-supplied Fate references preserved in `provenance/Design_Direction_Construction_History.md`  
**Execution status:** COMPLETE / standalone under `Character_Creation.md`

---

## F0. PURPOSE

This specification converts the completed Fate corpus into an executable **character-design direction** for Character Creation.

It is designed so that an executor can create a new female character that reads as belonging to the **Fate design family** without needing the original 90 images during ordinary execution.

This entry governs:

- Phase 1 body / face / hair / transformed-anatomy authorship;
- Phase 2 clothing / armor / garment architecture;
- Phase 3 weapon authorship;
- cross-phase identity coherence;
- Fate-specific anti-drift and validation.

It does **not** govern brushwork, line rendering, painterly finish, compositing, lighting, camera, or splash-art effects.

The target is not to copy a known Servant. The target is to apply the **structural design logic** repeatedly observed across the corpus to a newly authored concept-valid character.

---

# 1. AUTHORITY AND PRECEDENCE

## 1.1 Character Creation is the execution authority

`Character_Creation.md` remains superior to this entry.

Therefore:

- mandatory concept consequences happen first;
- body topology is resolved in Phase 1 before clothing or weapons;
- a concept-valid transformed body is never normalized back toward an ordinary Fate reference merely because ordinary humanoids are common in the corpus;
- Phase 2 owns clothing only and must preserve the frozen body;
- Phase 3 owns the weapon only and must preserve the frozen body + outfit;
- population differentiation, freezing, validation, and phase dependencies remain unchanged.

## 1.2 DD0002 owns Fate-compatible aesthetic authorship

Within the valid solution space left by Character Creation, DD0002 determines how to make the result read Fate-like through:

- anime identity construction;
- body proportion refinement;
- face / eye / hair decisions;
- transformed-body organization;
- macro silhouette design;
- outfit reinterpretation logic;
- material and ornament hierarchy;
- weapon macro and signature-hook design;
- selective cross-phase echoes;
- anti-copy constraints.

## 1.3 Final specification supersedes provisional intake hypotheses

The per-set records and v0.1–v0.9 hypothesis ledgers remain preserved for provenance.

When a provisional intake conclusion conflicts with this v1.0 specification, **v1.0 is authoritative**.

In particular, the corpus evidence for “no weapon visible,” props, relics, or support systems does **not** redefine Character Creation Phase 3. Phase 3 remains weapon design unless Character Creation itself is changed.

## 1.4 No reference-image dependency

Once DD0002 v1.0 is active, the executor should not need to consult Fate screenshots to perform ordinary Character Creation.

The corpus may still be used for auditing or later revisions.

---

# 2. FATE CORE DESIGN DNA

## 2.1 Core identity

A Fate-compatible original female character should read as:

> **a clean, iconic anime identity whose concept is compressed into a few memorable body/head, outfit, symbolic, and weapon decisions rather than expressed through literal cosplay or indiscriminate fantasy detail.**

The strongest recurring Fate design signal is not one costume family, one body type, one era, or one palette.

It is **authored identity compression**.

A Fate design usually feels as though the character has been reduced to several highly legible recognition anchors that survive even when the theme changes radically.

## 2.2 HARD STYLE HIERARCHY — STRUCTURE BEFORE VOCABULARY

### Tier 0 — defining structural identity

These carry the greatest weight:

1. **clean anime female face/head readability;**
2. **one clearly authored body/proportion/topology regime;**
3. **one memorable hair/head identity hook;**
4. **a small number of large readable silhouette decisions;**
5. **concept compression into character-specific symbolic choices;**
6. **an outfit that looks designed for this character rather than borrowed from a costume template;**
7. **a signature weapon whose category and identity are readable;**
8. **controlled detail hierarchy rather than all-over decoration.**

If these are weak, Fate vocabulary tokens will not repair the design.

### Tier 1 — strong construction tendencies

Frequently useful but not mandatory:

- fitted central body/core with one or two stronger peripheral structures;
- detached sleeves, cape/coat panels, skirt panels, selective armor, stockings, straps, or body-suit regions;
- deliberate asymmetry or one-side emphasis;
- one localized symbolic motif repeated sparingly;
- clear color blocking;
- selective contrast between soft textile and hard armor/weapon material;
- cultural / historical / mythic / modern source material translated into anime design rather than copied literally;
- weapon and outfit linked through one or two selective echoes rather than complete visual duplication.

### Tier 2 — conditional vocabulary only

The corpus contains all of the following, but none defines Fate by itself:

- knight armor;
- royal capes;
- gothic dresses;
- bridal wear;
- swimsuits;
- school uniforms;
- qipao / shrine / samurai / culturally inflected clothing;
- military coats;
- futuristic bodysuits;
- mecha equipment;
- crowns;
- halos;
- horns;
- wings;
- elf ears;
- tails;
- lace;
- fur;
- gold trim;
- black/red palettes;
- white/blue/gold palettes;
- firearms;
- katanas;
- polearms;
- oversized swords;
- cyber or cosmic motifs.

Use these **only** when the concepts, body, role, or authored design independently justify them.

### Tier 3 — default-generation prohibition

When Fate is applied to supplied/resolved concepts with no additional theme, these corpus defaults remain unjustified unless independently supported by the authorized inputs:

- a Saber-like blonde knight;
- a crown;
- a blue-white-gold dress;
- a black/red gothic demon;
- a seasonal beachwear character;
- a horned female character;
- a bridal outfit;
- a giant ornate sword;
- a cape solely because “Fate characters have capes”;
- dense gold filigree;
- any known Fate character silhouette with renamed details.

Those are corpus branches, not the core grammar.

## 2.3 No-additional-theme baseline

With the supplied/resolved concept set already established and **no additional theme, season, historical source, social role or outfit direction**, apply the following aesthetic baseline only within each item's active phase. Concept anchors and unusual body plans remain controlling:

- refine the concept-valid female character through **heroic-conceptual** proportion, face/hair and silhouette authorship;
- use medium complexity rather than maximal ornament;
- in Phase 2, use one clear garment family;
- in Phase 2, resolve concept-driven garment extent and boundary architecture from the frozen body and outfit construction;
- in Phase 2, use at most one major garment silhouette mass beyond hair in this no-additional-theme baseline, while preserving every concept-required body structure;
- use two dominant colors + one accent as the normal starting palette;
- use metal only where structurally justified;
- in Phase 2 or Phase 3, use a concept-derived motif only where the governing phase permits it; Phase 1 retains intrinsic physical authorship;
- build the Phase-3 weapon from concept/character identity rather than selecting a famous Fate-like sword by habit.

This neutral baseline exists specifically to stop random generation from drifting toward whichever flashy corpus branch is easiest to recall.

## 2.4 Fate identity-anchor budget

A strong original Fate design normally needs **3–5 primary identity anchors** across the full character.

Typical anchors may include:

- hair/head geometry;
- one facial/eye marker;
- a body or transformation feature;
- one outfit macro structure;
- one symbolic motif;
- one weapon silhouette or mechanism.

Do not try to make every visible component an identity anchor.

One strong anchor should remain clearly dominant.

## 2.5 Concept translation, not literal costume illustration

Concepts should be **transmuted into design logic**.

Example principle:

- “clock” should not automatically mean gears glued everywhere;
- “dragon” should not automatically mean horns + scales + tail + wing + dragon-shaped sword simultaneously;
- “saint” should not automatically mean white robe + halo + gold cross;
- “void” should not automatically mean black-purple cosmic particles.

Instead, compress a concept into a smaller set of structural consequences such as:

- a silhouette relationship;
- a body topology change;
- a repeated geometric motif;
- a material choice;
- a garment opening/closure principle;
- a weapon mechanism.

That is closer to the Fate corpus than literal token stacking.

---

# 3. CROSS-PHASE STYLE INVARIANTS

## 3.1 Clean anime identity must survive complexity

No matter how transformed, armored, glamorous, ceremonial, modern, or mechanical the result becomes, the character should retain a **clean anime identity read**.

The face/head must not disappear under random micro-detail.

## 3.2 Thumbnail silhouette before micro-detail

At small scale, the design should already communicate:

- body regime;
- hair/head identity;
- one major outfit or transformation structure;
- weapon class or weapon mass in Phase 3.

If the design only becomes interesting after zooming into trim, it is too detail-dependent.

## 3.3 Few strong structures beat many weak structures

Prefer:

- 2–4 major body/outfit silhouette groups;
- 1 primary symbolic motif;
- 1–2 secondary motif echoes;
- 1 major weapon hook.

Avoid equal-detail treatment everywhere.

## 3.4 Selective detail clustering

Fine detail should gather around focal zones such as:

- head/face;
- collar/chest closure;
- waist/hip anchor;
- shoulder/gauntlet interface;
- weapon guard/head/core;
- one transformed anatomical junction.

Large surfaces should often remain comparatively quiet.

## 3.5 Palette discipline

Default palette architecture:

- **2 dominant color families**;
- **1 accent color**;
- optional neutral / metal.

Monochrome and low-contrast designs are valid.

High-saturation multi-color schemes are also possible, but they require deliberate organization.

No color family is Fate-mandatory.

## 3.6 Controlled asymmetry

Asymmetry is useful when it strengthens identity:

- one stocking;
- one larger sleeve;
- one armored arm;
- off-center drape;
- one transformed side;
- one dominant weapon side.

Do not introduce asymmetry merely as decoration.

## 3.7 Cross-phase genre matching is optional

The outfit and weapon may come from different surface vocabularies.

A modern/fashion character can use a historical weapon; a ceremonial character can use a firearm; a culturally inflected outfit can use a mechanical weapon.

What must match is **authored character logic**, not literal genre origin.

## 3.8 Phase ownership must be explicit

Large rear or radial structures are a recurring Fate ambiguity.

Before freezing, assign each major structure to exactly one owner:

- Phase 1 body / anatomy;
- Phase 2 clothing / equipment garment structure;
- Phase 3 weapon;
- presentation-only / renderer effect.

Do not let halos, wings, rings, floating frames, thrones, mechanical arms, energy circles, or companions drift between phases without an ownership decision.

## 3.9 Transformation must not be normalized

If Character Creation produces:

- horns;
- wings;
- tails;
- extra limbs;
- unusual skin/material;
- hollow body regions;
- altered joints;
- non-bipedal lower body;
- mechanical anatomy;
- body-integrated appendages;
- asymmetrical anatomy;

DD0002 adapts to it.

It does not force the body back toward an ordinary Fate humanoid.

## 3.10 Illustration spectacle is not design grammar

Do not convert the following into character components unless explicitly owned:

- background magic circles;
- floating debris;
- particles;
- god rays;
- environmental flames;
- decorative moons;
- architecture;
- ocean splashes;
- petals;
- background creatures;
- stage props.

---

# 4. PHASE 1 — BODY / FACE / HAIR / IDENTITY GRAMMAR

## 4.1 Phase-1 purpose under Fate

Phase 1 should produce a body that is fully concept-valid **and** already carries a Fate-compatible identity before clothing is added.

Do not rely on Phase 2 to rescue a generic body.

## 4.2 Body-range principle

The 90-reference corpus supports a wide range:

- petite and small-chested;
- slim / delicate;
- athletic;
- average-curvy;
- voluptuous;
- very strong chest emphasis / strong hip-thigh emphasis;
- tan / dark skin;
- pale skin;
- conventional humanoid;
- heavily altered / multi-limbed / winged / horned / tailed / mechanically augmented.

Therefore:

> **Fate does not have one mandatory female body template.**

When concepts do not force a body extreme, use **balanced anime proportions** rather than automatically choosing either petite or hyper-voluptuous anatomy.

## 4.3 Baseline proportion language

For a conventional humanoid when no stronger body consequence exists:

- clear feminine torso/pelvis relation;
- readable waist without impossible compression;
- long-to-balanced leg proportion;
- shoulders not excessively broad by default;
- hands and feet stylized but functional;
- anatomy simplified into clean anime masses;
- chest/hip scale selected for this character, not for Fate globally.

Avoid Shoujo-Kaisen-style automatic hyper-voluptuous inflation.

## 4.4 Petite / compact bodies

For petite or small-chested characters:

- keep limb and torso proportions internally coherent;
- use hair/head geometry, posture, costume macro, or weapon silhouette to preserve strong presence;
- do not compensate for a petite body by adding excessive armor or ornament;
- let one compact signature hook carry identity;
- weapon scale may be equal-sized, oversized, or restrained depending on concept — not automatically oversized for contrast.

## 4.5 Voluptuous bodies

For curvy characters with strong chest emphasis:

- preserve readable ribcage, waist, pelvis, and limb structure;
- allow strong curves without turning the body into the only identity system;
- hair/head, transformation, outfit, and weapon still require their own authored logic;
- avoid making one upper-torso opening or upper-torso volume the sole Fate signal.

## 4.6 Athletic / muscular / powerful bodies

The corpus contains athletic bodies more often than heavily muscular ones, but Character Creation may require stronger musculature.

When that occurs:

- preserve visible power rather than shrinking muscle toward the corpus median;
- simplify muscle groups into elegant anime planes rather than hyper-real anatomy;
- keep the face/head refined and iconic;
- let outfit articulation respect shoulder, back, thigh, and joint volume;
- use weapon mass and stance appropriate to the frozen strength regime.

This is a workflow-derived extension of Fate's observed structural logic.

## 4.7 Transformed / altered body grammar

Fate-compatible transformation is organized, not noisy.

Use these rules:

1. preserve one clear **identity anchor at the face/head** unless the concept explicitly forbids it;
2. choose one dominant transformation system before adding secondary mutations;
3. group repeated appendages into readable families;
4. keep anatomical junctions deliberate;
5. make markings/glyphs/energy channels read as a graphic system rather than random tattoos;
6. let transformed anatomy carry silhouette burden so clothing does not need to compete with it;
7. preserve functional plausibility within the chosen fantasy logic.

### Wings

- define attachment and folding logic;
- decide whether wings are anatomical, mechanical, magical-body structures, or presentation-only;
- if anatomical, Phase 2 must leave shoulder/back interfaces usable.

### Horns / antlers / cranial structures

- make them part of head silhouette rather than floating ornaments;
- integrate hair around their roots;
- Phase-2 headwear must accommodate them rather than erase them.

### Tails

- define root position and body balance;
- clothing must provide a real exit/interface if the tail is anatomical.

### Extra arms / multi-limb topology

- distribute shoulder/torso attachment points cleanly;
- avoid visual tangling;
- keep a readable primary arm pair or dominant gesture hierarchy when useful;
- Phase 2 must preserve range of motion;
- Phase 3 may exploit multiple hands without forcing every hand to carry a separate weapon.

### Non-bipedal or more radical lower bodies

The 90-image corpus does not strongly establish a non-bipedal lower-body branch, but Character Creation may require it.

Derived Fate application rule:

- preserve clean anime upper-body/head identity;
- simplify the transformed lower-body silhouette into a few legible masses;
- use one repeated geometric/material language across the altered anatomy;
- adapt clothing and weapon interfaces to the actual topology;
- do not add human legs merely to imitate the corpus.

## 4.8 Face grammar

Fate faces are generally clean and highly legible.

Use:

- compact anime facial construction;
- minimal nose and mouth geometry;
- eyes as the primary emotional and identity feature;
- jaw/chin shape adjusted to character archetype rather than one universal face;
- expressions that clearly communicate role/personality without overacting by default.

Avoid:

- realistic facial texturing;
- excessive facial accessories;
- micro-ornament around eyes merely to look ornate;
- copying a famous Fate face/hair combination.

## 4.9 Eye grammar

Eyes may be:

- soft and round;
- sharp and narrow;
- calm / regal;
- playful;
- severe;
- heterochromatic;
- nonhuman in color or pupil treatment when concept-driven.

Priority:

> **clear shape + memorable color/value + role-consistent expression.**

Do not add unusual pupils or glow unless the body/concept justifies them.

## 4.10 Hair grammar

Hair is one of Fate's strongest recurring identity systems.

Author hair through **shape architecture**, not only length/color.

Choose 1–2 dominant structural hooks such as:

- distinctive bang geometry;
- asymmetrical fringe;
- high ponytail;
- twin tails;
- braid architecture;
- side buns;
- drill/spiral masses;
- short geometric crop;
- long straight curtain;
- ribbon-tied segmentation;
- one eye intentionally obscured;
- exaggerated but organized rear flow.

Long hair is common but not mandatory.

Do not use huge hair volume just to make a design feel Fate-like.

## 4.11 Skin, markings, and body-material treatment

Body markings can be strong Fate-compatible identity anchors when they are:

- limited to a coherent system;
- repeated with consistent geometry;
- tied to concept/body function;
- placed to follow anatomy or transformed material flow.

Possible treatments include:

- tattoo-like sigils;
- circuitry-like lines;
- crack/fissure patterns;
- luminous nodes;
- symbolic scars;
- nonhuman skin colors;
- material transitions.

Avoid random decal accumulation.

## 4.12 Phase-1 Fate style test

Phase 1 passes only if:

- the body fully obeys the concepts;
- the body does not depend on clothing to feel authored;
- the face/head reads cleanly as anime;
- at least one head/hair/body identity hook is memorable;
- transformation is organized rather than noisy;
- no famous Fate character is being reconstructed by silhouette alone;
- the body is not being pushed toward one corpus-favorite body type by habit.

---

# 5. PHASE 2 — OUTFIT DESIGN GRAMMAR

## 5.1 Phase-2 purpose under Fate

Phase 2 should make the frozen body look like a specifically authored Fate-direction character rather than a generic anime figure wearing fantasy clothes.

The outfit must derive from:

1. the frozen body/topology;
2. the concept identity;
3. the selected role/mood/function;
4. Fate's structural garment logic.

## 5.2 Core outfit thesis

> **reinterpret a recognizable clothing/role archetype through clean anime macro design, selective body framing, one or two character-specific deviations, and controlled motif placement.**

Do not build the outfit by stacking franchise tokens.

## 5.3 Macro architecture

A strong Fate outfit normally resolves into **2–4 primary visual groups**.

Common structural formulas include:

### A. Fitted core + peripheral cloth

Examples of peripheral cloth logic:

- cape;
- coat;
- detached sleeves;
- long skirt panels;
- overskirt;
- sash;
- rear train.

### B. Fitted core + selective armor

Use armor at:

- shoulders;
- forearms;
- chest;
- hips;
- thighs;
- lower legs;

rather than defaulting to full plate.

### C. One dominant robe / coat / dress mass

Useful for ceremonial, regal, occult, or formal characters.

### D. Minimal clothing + identity carried elsewhere

Valid when identity is carried by:

- transformed anatomy;
- hair;
- body markings;
- weapon;
- one large support structure.

### E. Modern / functional base + one mythic or symbolic deviation

Useful for contemporary, sporty, uniform, casino, school, military, or street-derived characters.

These are construction formulas, not theme presets.

## 5.4 Garment extent and boundary architecture

Fate-compatible design supports a wide range of garment extent, from compact body-contoured assemblies to layered robes, dresses, coats and armor systems. The amount of material is not itself the identity mechanism.

Use deliberate garment boundaries and negative space:

- cropped or shortened component geometry;
- open-back or side-open construction;
- high slits and split panels;
- asymmetric boundaries;
- separated upper/lower garment masses;
- fitted garments that preserve the frozen body regime;
- full layered or armored silhouettes when they fit the character.

Every open interval is a consequence of real garment edges, support and rear continuation. Do not rely on empty space as a substitute for garment architecture, and do not use fabric quantity as a quality score.

## 5.5 Historical / cultural / mythic reinterpretation

Fate repeatedly uses recognizable historical or cultural cues, but the strongest design behavior is **reinterpretation**, not museum accuracy.

Process:

1. identify 1–3 source-form cues;
2. keep the most recognizable silhouette or closure language;
3. simplify or exaggerate it for anime readability;
4. hybridize with body-specific needs;
5. add one concept-specific deviation;
6. discard decorative information that does not improve identity.

Avoid literal cosplay reconstruction.

## 5.6 Modern / seasonal / novelty branches

The corpus strongly supports:

- schoolwear;
- formalwear;
- casino/fashion;
- swimwear;
- athletic/streetwear;
- service-role outfits;
- contemporary jackets;
- modern firearms alongside nonmodern clothing.

However:

> **do not default to a seasonal/novelty variant unless the user/theme calls for it.**

Fate aesthetic authorship with no additional theme uses §2.3 after concept resolution and within the active phase.

## 5.7 Body-specific tailoring

### Petite body

- keep macro structures proportionate unless deliberate contrast is desired;
- use one strong shape hook rather than drowning the body in fabric/armor;
- preserve legibility of the small frame.

### Voluptuous body

- design support/closure around the real frozen torso;
- use body framing without making one upper-torso opening the sole outfit concept;
- balance large curves with simple outer shapes.

### Athletic / muscular body

- allow shoulder/back/thigh structure to remain visible;
- articulate armor and fabric around movement zones;
- avoid shrinking the body to fit delicate garment templates.

### Horned / antlered head

- headwear must route around or integrate with horn roots;
- no clipping or decorative floating hats that ignore anatomy.

### Wings

- leave actual wing roots free;
- avoid capes that collide with anatomical wings unless layering is deliberately engineered.

### Tails

- create a proper opening, raised waist, split panel, or harness route.

### Extra limbs

- use split sleeves, modular shoulder pieces, harness bands, open torso zones, or distributed drapery;
- preserve motion for every functional limb.

### Non-bipedal / unusual lower body

- replace human skirt/pants assumptions with topology-specific drape, armor plates, belts, collars, harnesses, or body-surface treatment;
- preserve the Fate macro hierarchy instead of forcing standard garments.

## 5.8 Material language

Choose a small material stack appropriate to the character.

Useful Fate-compatible materials include:

- matte or satin cloth;
- structured uniform fabric;
- leather;
- polished armor metal;
- darkened metal;
- lacquer-like surfaces;
- bodysuit / synthetic material;
- lace;
- fur;
- translucent cloth;
- crystal / magical solid;
- clean mechanical plating.

Default to **2–3 dominant material families**, not all of them.

## 5.9 Ornament grammar

Ornament is valid and should remain meaningful, attached and subordinate to garment architecture.

Useful accent systems include:

- border trim;
- embroidery or woven motifs;
- fixed emblems and insignia;
- clasps and buckles;
- repeated knot geometry;
- fixed inset stones or cores;
- floral/heraldic shape families;
- line/glyph systems;
- appliqué, lace, relief or sculpted edging.

Avoid suspended-drop adornment as a default finishing device. Phase-2 ornament should normally be integrated into the garment surface, edge, closure or structural junction rather than hanging freely from it. Do not cover every panel in filigree. Gold is an accent option, not a Fate requirement.

## 5.10 Color architecture

Normal starting rule:

- 2 dominant colors;
- 1 accent;
- optional metal/neutral.

Valid extremes include:

- near monochrome;
- black/white with one bright accent;
- soft pastel ceremonial;
- saturated modern;
- dark gothic;
- white/gold heroic;
- tech neon.

No palette is canonical across Fate as a whole.

## 5.11 Asymmetry rule

Use asymmetry when it creates a clear identity function.

Good asymmetry:

- one armored limb;
- one stocking;
- one dominant sleeve;
- one side cape;
- offset sash;
- asymmetrical transformed anatomy;
- one large hip panel.

Bad asymmetry:

- random unequal details with no visual hierarchy.

## 5.12 display-forward styling / glamour grammar

glamour ranges from absent to extreme.

When used:

- integrate it into garment construction;
- keep openings intentional;
- preserve character archetype and role;
- do not let display-forward styling replace the concept identity;
- do not assume Fate means minimal garment extent.

## 5.13 Phase-2 Fate style test

Phase 2 passes only if:

- clothing fits the frozen topology;
- the outfit has a clear macro read;
- source influences are reinterpreted rather than copied literally;
- ornament is subordinate to identity;
- coverage level looks intentional;
- unusual anatomy is accommodated, not hidden;
- seasonal or novelty vocabulary was not introduced without justification;
- removing fine trim would still leave a recognizable authored outfit.

---

# 6. PHASE 3 — WEAPON DESIGN GRAMMAR

## 6.1 Phase-3 authority ruling — HARD

For Character Creation execution:

> **Phase 3 remains weapon design.**

The Fate corpus includes references where no weapon is visible, where props replace combat emphasis in the illustration, or where support systems dominate.

Those examples prove presentation breadth; they do **not** authorize skipping the required Character Creation weapon.

If Character Creation requires a Phase-3 weapon, DD0002 must provide one.

## 6.2 Weapon thesis

A Fate-compatible weapon should read as:

> **a recognizable weapon category transformed by one strong character-specific idea into a signature object.**

Do not start from ornament.

Start from:

1. category;
2. use/function;
3. macro silhouette;
4. signature deformation/mechanism;
5. material/color relationship;
6. secondary detail.

## 6.3 Weapon-category breadth

The corpus supports broad weapon families including:

- sword;
- katana / saber-like blade;
- spear / lance / polearm;
- bow;
- staff / scepter;
- firearm;
- paired blades;
- gauntlet / arm-mounted weapon;
- modular mechanical weapon;
- multi-part weapon system;
- body-interface weapon system.

Character Creation concepts should decide the best category where possible.

Do not default to a sword simply because Fate is famous for sword users.

## 6.4 Macro silhouette

At thumbnail scale the weapon should have:

- a clear primary axis;
- a readable grip/interface;
- a distinct head/blade/body mass;
- one obvious signature hook.

The weapon may be:

- restrained;
- conventional-sized;
- oversized;
- elongated;
- compact;
- modular;
- paired;
- mechanically articulated.

Scale is character-specific.

## 6.5 Signature-hook types

Choose **one major** and optionally **one secondary** signature hook:

- unusual guard geometry;
- split or bifurcated blade;
- circular/ring head;
- asymmetric blade profile;
- integrated sheath/mechanism;
- energy channel/core;
- modular folding/transforming section;
- mechanical articulation;
- body-interface mount;
- paired complementary forms;
- multi-limb distribution;
- one symbolic cutout/crest shape.

Do not combine five signature hooks in one weapon.

## 6.6 Detail hierarchy

Primary read:

- weapon class;
- silhouette;
- character-specific deformation.

Secondary read:

- guard ornament;
- core/gem;
- trim;
- small motif;
- material transition.

Tertiary micro-detail should remain sparse.

## 6.7 Material language

Use a controlled stack such as:

- steel / dark steel;
- polished silver;
- gold/bronze accent;
- lacquered shaft;
- crystal / energy material;
- bone/organic material if concept-driven;
- clean mechanical plating;
- cloth/cord wrap.

Do not use gold, gems, or glowing cores by default.

## 6.8 Outfit / weapon relationship

The weapon should usually echo **1–2** character features through:

- palette;
- one shape motif;
- material;
- emblem;
- line direction;
- asymmetry;
- transformed-body interface.

It does **not** need to match the outfit's historical genre.

Selective echo is preferable to total repetition.

## 6.9 Weapon + transformed body

Phase 3 must respect the frozen Phase-1 body.

Allowed:

- weapon handles sized for altered hands;
- mounts attached to existing mechanical anatomy;
- extra arms carrying parts of one weapon system;
- wing/tail clearance;
- harness or brace interfaces through Phase-2 clothing if already frozen and compatible;
- paired or distributed weapons for multi-limb characters.

Not allowed:

- growing new arms in Phase 3;
- changing hand topology to fit the weapon;
- adding a tail only to mount a weapon;
- changing body proportions;
- retroactively rewriting Phase 1.

If the desired body-integrated weapon requires a body structure that was not frozen in Phase 1, redesign the weapon around the existing body rather than modifying the body.

## 6.10 Multi-limb weapon rule

Extra arms do **not** mean every hand must hold an independent weapon.

Preferred options:

- one primary weapon + support grips;
- one modular weapon split across hands;
- paired weapons + free utility hands;
- one ritual/mechanical system controlled by several hands;
- a small coordinated arsenal only when conceptually justified.

Keep the global read coherent.

## 6.11 Scale relationship

### Petite / delicate character

Weapon may be:

- compact for elegance;
- equal-scale for balance;
- deliberately oversized for contrast.

No option is mandatory.

### Voluptuous / glamorous character

Do not automatically answer body volume with giant weapon volume.

Use the category and role to determine scale.

### Athletic / powerful character

Heavier mass, longer leverage, larger grips, or stronger mechanical forms may fit.

### Highly transformed character

Let the weapon interface respond to anatomy rather than merely increasing ornament.

## 6.12 Anti-copy / Noble-Phantasm rule

Do not recreate:

- Excalibur-like silhouettes;
- Gáe Bolg-like forms;
- famous Fate bows;
- famous shields;
- famous staffs;
- known Noble Phantasm mechanisms;
- recognizable signature guard/color/motif combinations.

The weapon should feel **Fate-compatible**, not “an off-brand version of a known Servant weapon.”

## 6.13 Phase-3 Fate style test

Phase 3 passes only if:

- the weapon respects the frozen body and outfit;
- weapon category or functional logic is readable;
- it has one memorable signature hook;
- it does not rely on dense ornament;
- scale is character-specific;
- transformed anatomy is accommodated without changing Phase 1;
- it does not copy a known Fate weapon;
- it remains legible at thumbnail scale.

---

# 7. CROSS-PHASE FATE COHERENCE COMPILER

## 7.1 Identity compression procedure

Before freezing the complete design, identify the character's **3–5 strongest anchors**.

A useful distribution is:

1. one head/hair anchor;
2. one body/topology anchor;
3. one outfit macro anchor;
4. one concept-symbol anchor;
5. one weapon anchor.

Some characters may merge two roles into one structure.

## 7.2 Phase 1 → Phase 2

Phase 2 should answer:

- what body needs to remain visible?
- what body needs support/protection?
- what topology needs openings or interfaces?
- which Phase-1 identity anchor should clothing frame rather than cover?
- which concept motif has not yet been expressed and belongs in clothing?

Do not simply “dress the character in Fate clothes.”

## 7.3 Phase 2 → Phase 3

Phase 3 should answer:

- what weapon category best serves the character?
- what physical interface is available?
- what single shape/material/color echo from the outfit is useful?
- what concept element remains available to become the weapon's signature hook?

Do not redesign the outfit to fit the weapon.

## 7.4 Full-character relationship

A strong Fate-direction character often contains productive contrast:

- soft body + hard weapon;
- petite body + severe equipment;
- ceremonial dress + modern firearm;
- modern fashion + historical blade;
- transformed body + simple clothing;
- heavy armor + refined face;
- minimal clothing + complex weapon;
- ornate clothing + restrained weapon.

Contrast is valid if the total identity remains deliberate.

## 7.5 Cross-phase echo limit

Normally repeat any one motif across **no more than 2–3 systems**.

Example:

- triangular motif in hair clasp + weapon guard + one waist panel = enough.

Do not put the same triangle on every cuff, boot, cape, blade, eye, tattoo, and belt.

## 7.6 Full Fate read test

Ask:

> If the franchise name were removed, would this look like a deliberately authored anime heroic/legendary character with a compressed identity, concept-translated outfit, and character-specific signature weapon — or merely generic fantasy/gacha design?

If it is generic, strengthen structural identity rather than adding tokens.

---

# 8. STYLE-SPECIFIC DECISION PRIORITIES

When several valid alternatives exist, choose in this order:

1. **Character Creation compliance**;
2. **complete concept consequences**;
3. **clear anime face/head identity**;
4. **distinct body/topology regime**;
5. **one memorable hair/head hook**;
6. **strong macro silhouette**;
7. **outfit authored specifically for the frozen body**;
8. **one concept-specific symbolic system**;
9. **signature weapon category + hook**;
10. **controlled color/material relationship**;
11. **localized ornament**;
12. **micro-detail last**.

Never reverse the order so that gold trim, capes, crowns, horns, or weapon spectacle drive the design first.

---

# 9. ANTI-DRIFT RULES

## 9.1 Generic anime drift

FAIL if the body, hair, outfit, and weapon could be swapped among unrelated characters with little loss of identity.

Correction:

- strengthen 3–5 identity anchors;
- reduce generic filler details;
- bind one concept consequence to each major phase.

## 9.2 Ornate-fantasy drift

FAIL if “Fate” has been interpreted as:

- gold everywhere;
- filigree everywhere;
- cathedral + crown + giant sword by default;
- generic aristocratic fantasy;
- random gems and halos.

Correction:

- remove decorative tokens;
- rebuild the macro silhouette;
- return to concept compression.

## 9.3 Historical-cosplay drift

FAIL if a historical/cultural influence is reproduced literally with little anime reinterpretation.

Correction:

- keep 1–3 source cues;
- simplify;
- hybridize with body and concept;
- create a new silhouette decision.

## 9.4 Seasonal-variant drift

FAIL if random generation defaults to:

- minimal beachwear;
- beachwear;
- Halloween/gothic novelty;
- bridal novelty;
- service uniform;
- casual event variant

without user/theme justification.

Seasonal references prove range, not default identity.

## 9.5 Saber-clone / known-character drift

FAIL if the design resembles an existing Fate character because it duplicates several of:

- hair geometry;
- face/eye combination;
- palette;
- costume silhouette;
- crown/headpiece;
- weapon type and silhouette;
- body proportions;
- signature motif.

One coincidental similarity can occur; clusters of similarities must be redesigned.

## 9.6 display-forward styling-only drift

FAIL if the design's only distinguishing feature is exposure or body exaggeration.

Correction:

- add real head/hair identity;
- strengthen outfit macro logic;
- give the weapon a character-specific hook;
- preserve glamour only as one layer.

## 9.7 Weapon-spectacle drift

FAIL if the weapon becomes “more Fate-like” only by becoming larger, more glowing, or more ornate.

Correction:

- simplify the category;
- introduce one stronger signature mechanism or shape hook;
- reduce excess effects.

## 9.8 Rear-structure ownership drift

FAIL if a halo, wing, throne, ring, floating frame, mechanical ar, or companion is added without clear phase ownership.

Correction:

- assign body / outfit / weapon / presentation owner;
- delete it if no owner is justified.

## 9.9 Cross-entry drift

Do not import Shoujo Kaisen rules merely because both entries include glamorous gacha-adjacent female designs.

In particular, Fate does **not** inherit:

- Shoujo Kaisen's stronger voluptuous bias;
- Shoujo Kaisen's mobile-gacha silhouette assumptions;
- Shoujo Kaisen's specific glamour hierarchy;
- Shoujo Kaisen's weapon styling rules.

DD0002 stands alone.

## 9.10 Rendering drift

Do not compensate for weak design by specifying:

- dramatic lighting;
- ultra-detailed painting;
- particles;
- cinematic camera;
- ornate background;
- high-resolution texture.

Those are renderer decisions, not Fate design-direction fixes.

---

# 10. EXECUTION INTERFACE FOR CHARACTER CREATION

## 10.1 Phase 1 interface

Given the concept set and active `Design Direction: Fate`:

1. resolve all mandatory concept consequences;
2. independently choose the body regime;
3. preserve any required transformed topology;
4. create a clean anime face/head identity;
5. choose one strong hair/head hook;
6. organize nonhuman/body-surface systems into readable groups;
7. identify 1–2 Phase-1 identity anchors before freeze;
8. validate with §4.12.

## 10.2 Phase 2 interface

Given the frozen body:

1. choose one garment/role archetype appropriate to concepts and character;
2. reinterpret it rather than reproduce it literally;
3. establish 2–4 macro groups;
4. map coverage/open regions intentionally;
5. accommodate all unusual anatomy;
6. choose 2–3 material families;
7. choose a disciplined palette;
8. place ornament only at focal zones;
9. ensure one outfit macro anchor is memorable;
10. validate with §5.13.

## 10.3 Phase 3 interface

Given frozen body + frozen outfit:

1. select the weapon category from concepts/role, not franchise habit;
2. establish a readable macro silhouette;
3. choose one major signature hook;
4. adapt grip/interface to the frozen body;
5. echo 1–2 character/outfit features selectively;
6. keep ornament subordinate;
7. preserve phase ownership;
8. validate with §6.13.

## 10.4 Random-character / no-theme execution

For an existing supplied/resolved concept set assigned Fate, with no additional theme:

- do not choose a seasonal/event variant by default;
- do not copy a famous class/Servant template;
- use the neutral heroic-conceptual baseline in §2.3;
- refine a valid concept-derived body regime instead of defaulting voluptuous; canonical body-plan constraints remain frozen;
- generate a fresh head/hair anchor;
- in Phase 2, use one clear garment family;
- in the authorized later phase, use a concept-derived motif only where allowed;
- in Phase 3, author one readable signature weapon for the frozen character;
- keep total detail moderate until stronger concept needs appear.

---

# 11. STANDALONE SUFFICIENCY TEST

DD0002 v1.0 is sufficient only if an executor using **only**:

- `Character_Creation.md`;
- the concept inputs;
- this DD0002 entry

can create a new Fate-compatible female character without consulting screenshots.

The executor must be able to answer from this document alone:

- what the core Fate structural identity is;
- how broad the body range is;
- how to design petite, voluptuous, athletic, transformed, multi-limbed, or unusual bodies;
- how faces, eyes, and hair should be authored;
- how to turn source concepts into an outfit without cosplay-literalism;
- how much detail and ornament to use;
- how to accommodate unusual anatomy in clothing;
- how to build a Fate-compatible signature weapon;
- how to prevent copying known characters;
- how to validate the final full-character read.

If any of these requires the original corpus, this entry is insufficient.

---

# 11A. HARD SELF-CHECK BEFORE FREEZE

Before freezing any Fate body, outfit, or weapon, answer these in order:

1. **If all gold, gems, particles, filigree, capes, halos, and background effects were removed, would the core design still have a distinct authored identity?** If no: FAIL.
2. **Can the character be recognized at thumbnail scale through face/head, body/silhouette, and a few major structures?** If no: FAIL.
3. **Did any theme element appear only because it was seen often in the Fate corpus rather than because this character needed it?** If yes: remove it.
4. **Is the source concept being translated into design logic rather than literal costume symbols?** If no: rebuild.
5. **Does every major rear/radial/support structure have an explicit phase owner?** If no: resolve ownership or remove it.
6. **Is the Phase-3 weapon a real character-specific weapon under Character Creation, rather than a prop substituted because some Fate references lacked visible weapons?** If no: rebuild Phase 3.
7. **Does the design avoid clustering into a recognizable existing Fate character?** If no: redesign the overlapping anchors.
8. **Is premium/heroic specificity coming from strong decisions rather than ornament count?** If no: simplify.

Only after all eight checks pass may ordinary phase validation continue.

---

# 12. VALIDATION CHECKLIST

## Phase 1

- [ ] Complete concept consequences are present.
- [ ] Body regime was chosen independently for this character.
- [ ] No global Fate chest/body template was imposed.
- [ ] Face/head has clean anime readability.
- [ ] Hair/head contains at least one memorable structural hook.
- [ ] Required transformed anatomy remains intact.
- [ ] Markings/material changes follow a coherent system.
- [ ] Multi-limb/nonhuman topology is readable rather than tangled.
- [ ] No known Fate character is being reconstructed.

## Phase 2

- [ ] Outfit is built around the frozen body.
- [ ] Unusual anatomy has real openings/interfaces.
- [ ] Garment architecture resolves into a few macro groups.
- [ ] Coverage is intentional.
- [ ] Historical/cultural cues are reinterpreted rather than copied literally.
- [ ] Seasonal/novelty styling is justified if present.
- [ ] Palette is disciplined.
- [ ] Material count is controlled.
- [ ] Ornament is localized.
- [ ] Fine trim is not carrying the entire design.

## Phase 3

- [ ] A weapon exists whenever Character Creation requires Phase 3.
- [ ] Weapon category/function is readable.
- [ ] One major signature hook is clear.
- [ ] Weapon fits frozen anatomy and outfit.
- [ ] Body topology was not changed to fit the weapon.
- [ ] Weapon scale is character-specific.
- [ ] Outfit/weapon genre matching was not forced unnecessarily.
- [ ] Weapon does not copy a known Noble Phantasm.
- [ ] Weapon remains legible without particle effects.

## Full character

- [ ] 3–5 strong identity anchors are identifiable.
- [ ] One anchor remains dominant.
- [ ] Phase ownership is explicit.
- [ ] Body, outfit, and weapon feel authored for the same individual.
- [ ] The design is not generic ornate fantasy.
- [ ] The design is not a seasonal-event default unless requested.
- [ ] The design is not a renamed existing Fate character.
- [ ] Removing the rendering/background would leave the design intact.

---

# 13. CORPUS NOTES, CONFIDENCE, AND NON-RESTRICTIONS

## 13.1 High-confidence recurring grammar

Based on the complete 90-reference corpus, the highest-confidence structural findings are:

- broad body-type tolerance;
- clean anime face/head readability;
- hair/head architecture as a major identity carrier;
- wide costume-family tolerance;
- strong macro authorship over one fixed theme;
- selective rather than all-over ornament;
- cultural/historical/modern/futuristic reinterpretation rather than literalism;
- large range of garment extent;
- transformed/nonhuman anatomy is compatible;
- clothing adapts to topology;
- weapon categories are broad;
- outfit/weapon genre matching is optional;
- internal character authorship matters more than franchise token stacking;
- rear/support structures require explicit ownership.

## 13.2 Important non-restrictions

The following are **not** Fate requirements:

- blonde hair;
- long hair;
- large chest proportions;
- small chest proportions;
- armor;
- a cape;
- a crown;
- horns;
- wings;
- white/blue/gold;
- black/red;
- gothic clothing;
- bridal clothing;
- seasonal/display-forward styling;
- historical clothing;
- a sword;
- a giant weapon;
- dense ornament;
- highly open treatment;
- highly covered treatment.

## 13.3 Derived extensions for Character Creation

Some body regimes required by Character Creation may exceed what the 90-image corpus showed clearly, especially:

- fully non-bipedal lower bodies;
- severe skeletal/topological rewrites;
- extremely muscular bodies;
- hollow or materially impossible bodies.

For those cases, do **not** reject the body as “not Fate.”

Apply the observed Fate structural grammar by preserving:

- anime head/face identity where conceptually possible;
- clear silhouette grouping;
- one or two strong symbolic systems;
- topology-aware clothing;
- readable weapon interface;
- controlled detail hierarchy.

This is a deliberate Character-Creation compatibility extension, not an attempt to claim that every such topology was explicitly present in the corpus.

## 13.4 Rendering caveat

The corpus contains multiple illustration/rendering approaches.

DD0002 extracts **design direction**, not a unified Fate drawing style.

Do not infer brushwork, line style, shading, or rendering parameters from this entry.

---

# 14. DD0002 VERSION HISTORY

## v1.0

- Compiled the final executable Fate Design Direction from all 90 preserved references.
- Promoted structural conclusions only after the nine-set intake was complete.
- Defined Fate's hard hierarchy, default no-theme baseline, body/identity grammar, transformed-body extension, outfit grammar, Phase-3 weapon grammar, cross-phase compiler, anti-drift rules, execution interface, standalone sufficiency test, and validation checklist.
- Corrected the provisional Phase-3 overreach from intake: corpus presentations without weapons do not override Character Creation; Phase 3 remains weapon design under the workflow.
- Added explicit phase-ownership handling for rear/radial/support structures.
- Added explicit non-seasonal default rules to prevent random generation from overfitting to beach, bridal, gothic, knight, cyber, or other flashy corpus branches.
- Marked DD0002 Fate ACTIVE / COMPLETE.

---

## DD0003 — ARKNIGHTS

**Canonical label:** Arknights  
**Entry ID:** DD0003  
**Registry status:** ACTIVE / COMPLETE  
**Current entry version:** 1.0  
**Construction provenance:** corpus intake completed at construction v0.2  
**Source corpus:** 2 user-supplied sets × 10 references = 20 references, fully processed  
**Phase coverage:** Phase 1 character/body/identity + Phase 2 outfit + Phase 3 weapon  
**Activation:** standalone under `Character_Creation.md`; source images are not required for ordinary execution.


### 0. FINAL DD0003 v1.0 EXECUTABLE SPECIFICATION

This section is the **authoritative Arknights Design Direction** for ordinary Character Creation execution.

The construction evidence and provisional v0.1–v0.2 ledgers remain preserved in `provenance/Design_Direction_Construction_History.md`. They are not active generation rules. If a provisional intake hypothesis conflicts with this final v1.0 section, **v1.0 controls**.

#### 0.1 Authority and scope

`Character_Creation.md` remains superior to DD0003.

Therefore:

- concepts and mandatory material/anatomical consequences are resolved first;
- Phase 1 owns the body and identity;
- Phase 2 owns clothing only and must preserve the frozen Phase-1 body;
- Phase 3 owns the weapon only and must preserve the frozen body + outfit;
- unusual anatomy is never normalized merely to resemble a familiar operator;
- population differentiation, freezing, validation, and renderer handoff remain unchanged.

Within the valid solution space left open by Character Creation, DD0003 governs the **Arknights-compatible design language** of intrinsic body design, functional fashion, equipment integration, material hierarchy, accent logic, and weapon engineering. Presentation geometry remains owned by `Character_Creation.md`.

Role/environment terminology below describes physical fit, construction, use and interface constraints derived from the authorized inputs. It does not authorize inventing an occupation, biography, setting or scene; `Character_Creation.md` §§1.5 and 1.8 remain binding. Cross-phase baseline lists are routed by the active phase before use.

#### 0.2 Core design DNA

A successful original female Arknights design should read as:

> **an individually authored operator whose extraordinary biology or identity is treated as matter-of-fact, whose clothing feels materially constructed for a real role and environment, and whose weapon/equipment has readable functional interfaces even when its scale or supernatural mechanism is extreme.**

The style is **not** defined by “black techwear,” animal ears, tactical pouches, long coats, or oversized weapons by themselves.

Its strongest recurring structure is:

1. **clear anime female identity;**
2. **one legible body/species/topology state;**
3. **occupation-/environment-aware clothing architecture;**
4. **visible construction and attachment logic;**
5. **one function-readable signature weapon/system;**
6. **controlled signal-color or material accents;**
7. **selective detail rather than indiscriminate greebling.**

#### 0.3 HARD STYLE HIERARCHY — STRUCTURE BEFORE TOKENS

##### Tier 0 — defining structural identity

The following carry the greatest weight:

- a readable anime face/head identity;
- concept-valid horns, ears, tails, scales, antlers, wings, or other anatomy integrated as real body structures rather than accessories;
- a strong but practical macro silhouette;
- clothing whose layers, openings, seams, closures, and equipment interfaces look intentionally constructed;
- one coherent material hierarchy;
- one primary role/environment logic;
- an engineered or otherwise function-readable Phase-3 weapon;
- controlled accent repetition across body/outfit/weapon.

If these are weak, adding utility straps, warning labels, tactical boots, or neon accents will not make the result Arknights-like.

##### Tier 1 — strong construction tendencies

Frequently useful but not mandatory:

- fitted inner garment under a looser structural outer layer;
- coat, shell, cape, split robe, dress, jacket, or armor panels used as one dominant peripheral mass;
- belts, harnesses, zips, clips, lacing, modular pouches, ID tags, labels, or attachment rails that visibly do something;
- material contrast between cloth / polymer / leather / metal / fur / mineral / energy;
- deliberate asymmetry in leg coverage, coat opening, pouches, sleeve treatment, or equipment placement;
- neutral/desaturated base colors with one or two sharper accents;
- cultural, formal, gothic, frontier, clinical, or ceremonial silhouettes translated through modern/technical construction.

##### Tier 2 — conditional vocabulary only

The corpus contains all of the following, but none is an Arknights-wide default:

- white laboratory coats;
- black tactical shorts;
- translucent rain shells;
- military dresses;
- gothic ruffles;
- fur cloaks;
- East-Asian robe silhouettes;
- medical/industrial labels;
- high-visibility yellow accents;
- cyan luminous cables;
- horns, animal ears, tails, antlers;
- riot shields;
- giant lances;
- compound bows;
- firearms;
- mechanized crossbows;
- Arts staffs;
- crystalline or flame-like intrinsic weapon materials and geometry.

Use these only when the character's concept, role, environment, body, or weapon logic independently supports them.

#### 0.4 No-theme / random-character baseline

After supplied/resolved concepts, anchors and symbiotic constraints define the valid body-solution space, apply Arknights with **no additional theme** through the following aesthetic baseline. Operator grammar is a design treatment, not an invented occupation, concept set or independent creation path. Each later-phase item applies only in its owning phase.

Use:

- a concept-valid female body whose proportions receive grounded Arknights refinement within the frozen body plan;
- body type selected from the concept/character, not a fixed franchise mannequin;
- all concept-required body anchors, plus compatible refined identity features justified by the frozen concepts; no arbitrary feature-count cap;
- in Phase 2, one fitted or simple inner garment;
- in Phase 2, one practical outer layer whose construction is justified by the authorized character inputs;
- in Phase 2, one visible fastening/interface system;
- a restrained material palette;
- one signal accent color;
- in Phase 3, one signature weapon with readable grip/handling/engineering;
- medium complexity by default.

Avoid defaulting to all-black tactical clothing, medical coats, random caution labels, or a giant gun merely because they are easy Arknights shorthand.

#### 0.5 Identity-anchor budget

A strong Arknights original normally needs **3–5 primary recognition anchors** across the full design.

Good anchors include:

- horn/ear/tail/head topology;
- hair mass or color break;
- one dominant outerwear silhouette;
- one material or fastening system;
- one occupation/role cue;
- one weapon macro silhouette;
- one signal accent or repeated geometric motif.

Do not make every strap, pouch, seam, decal, or mechanical joint an identity anchor.

---

### 0A. PHASE 1 — BODY / IDENTITY GRAMMAR

#### 0A.1 Body philosophy

Arknights Phase 1 should feel **grounded first, extraordinary second**.

The body should remain anatomically readable even when it includes nonhuman structures. The corpus supports slender, athletic, and curvier female bodies; it does **not** justify one mandatory chest, waist, height, or leg proportion.

Default behavior:

- preserve realistic-enough balance under anime stylization;
- avoid extreme pin-up exaggeration unless concepts require it;
- allow long limbs and elegant proportions, but keep weight distribution coherent;
- let nonhuman structures carry silhouette before inventing extra costume bulk.

#### 0A.2 Face grammar

Prefer:

- clean anime face construction;
- relatively restrained lower-face detail;
- eyes that can read calm, competent, reserved, severe, or quietly expressive;
- sharper eye shapes when compatible with the character;
- one controlled facial identity hook rather than dense facial markings by default.

Avoid making every Arknights female character look expressionless or militarily stern. The corpus includes elegant, playful, confident, and soft expressions as well.

#### 0A.3 Hair grammar

Hair is a major identity system but should not become ornamental noise.

Valid patterns include:

- short bobs;
- layered shoulder-length cuts;
- long practical ponytails;
- very long flowing hair;
- controlled color gradients or accent streaks;
- hair that visually interacts with ears, horns, collars, hoods, and equipment.

Use a strong outer hair mass plus selective internal breaks. Do not add random multicolor streaking without identity or palette logic.

#### 0A.4 Nonhuman anatomy — HARD

Animal, horned, draconic, serpentine, or other nonhuman traits are treated as **normal anatomy**, not costume props.

When present:

- ears emerge from plausible cranial positions;
- horns/antlers affect hair, headwear, collars, and hood geometry;
- tails have a clear base and motion path;
- scales/fur/segmentation have a deliberate body-material transition;
- large tails or appendages count toward the silhouette budget;
- clothing openings and closures must accommodate them.

Do not paste ears/tails onto an otherwise unrelated generic anime body.

#### 0A.5 Strong transformation / unusual topology

If Character Creation produces a more radical body than the 20-reference corpus directly shows, DD0003 must extrapolate from its structural logic rather than normalize the body.

Use this procedure:

1. preserve the concept-valid topology;
2. define the biological/mineral/mechanical material system cleanly;
3. keep the face/head identity readable where possible;
4. give transformed masses clear silhouette ownership;
5. let Phase 2 adapt around the body;
6. let Phase 3 use body-specific interfaces when useful.

Arknights compatibility comes from **functional integration**, not from returning the body to ordinary human proportions.

#### 0A.6 Skin / fur / scale / mineral treatment

Use nonhuman surface systems selectively and coherently:

- localized scales or plated regions;
- fur transitions near ears/tails/limbs;
- mineral/crystalline growth tied to concept or setting logic;
- scars, markings, or contamination-like features when conceptually justified;
- restrained interface zones between ordinary skin and transformed material.

Do not cover the entire body with arbitrary glowing circuitry merely to make it look “sci-fi.”

#### 0A.7 Phase-1 Arknights test

Before freezing Phase 1, verify:

- Is the female identity readable without clothing?
- Are nonhuman traits anatomically owned rather than accessory-like?
- Is the silhouette distinctive without relying on generic operator clothing?
- Is body topology preserved rather than normalized?
- Are hair/head choices compatible with ears, horns, or other structures?
- Is the body plausible enough to support the intended role/equipment later?

---

### 0B. PHASE 2 — OUTFIT GRAMMAR

#### 0B.1 Core outfit thesis

Arknights clothing should feel like **fashion designed under real operational constraints**.

The strongest construction model is:

> **inner layer → role garment → outer protection / silhouette layer → attachment/interface system → mission or occupation equipment → selective graphic accents.**

Not every design needs every layer.

#### 0B.2 Functional layering

A strong outfit normally makes it possible to answer:

- what is worn directly on the body?
- what provides structure, enclosure or support?
- what protects from weather, impact, contamination, heat, or movement?
- what carries equipment?
- where does the weapon attach or rest?
- how do large ears/horns/tails pass through or around the garment?

Layers should read as separable physical objects, even when stylized.

#### 0B.3 Supported outfit families

The corpus directly supports a wide range:

- clinical / institutional coats;
- professional technical garments;
- field/hunter clothing;
- transparent weather shells;
- gothic/fashion dresses;
- formal dresses with armor integration;
- knightly armor/cape systems;
- technicalized East-Asian silhouettes;
- long robes and split coats;
- tactical-civilian hybrids;
- fur-trimmed outerwear;
- short fitted combat clothing;
- ceremonial silhouettes with industrial fastening logic.

No single family is the style.

#### 0B.4 Silhouette architecture

Prefer a small number of large masses:

- fitted body core;
- one main coat/cape/robe/dress mass;
- one equipment or weapon mass;
- nonhuman appendage mass if applicable.

If the tail, weapon, and coat are all enormous, simplify one of them.

Readable silhouette hierarchy matters more than maximal layer count.

#### 0B.5 Garment extent and boundary logic

Garment extent is role- and character-dependent. The corpus supports technical garments, shorts and skirts, fitted dresses, armor systems, asymmetrical boundary layouts, layered translucent shells, coats and other practical or fashion-driven constructions.

Do not make one degree of enclosure the default. Use body access, weather, movement, protection, fashion intent and species anatomy as the decision basis, then resolve the result through concrete garment panels, edges, layers and interfaces.

#### 0B.6 Construction hardware

Commonly useful elements include:

- zippers;
- webbing;
- buckles;
- straps;
- clips;
- carabiner-like connections;
- lacing;
- hooks;
- ID tags;
- harnesses;
- pouches;
- equipment loops;
- cable guides;
- rigid attachment plates;
- reinforced seams;
- utility pockets;
- modular panels.

**HARD RULE:** every visible hardware cluster must justify itself through function, silhouette, role, material transition, or equipment interface. Random “techwear clutter” is drift.

#### 0B.7 Material language

Strong Arknights material contrast may include:

- matte technical cloth / nylon;
- coated textile;
- translucent polymer;
- rubberized protection;
- leather;
- brushed or painted metal;
- ceramic/composite armor;
- fur / hide;
- knit/scarf materials;
- regional woven or robe textiles;
- crystal/mineral components used selectively.

Use one dominant material family, one secondary contrast, and limited tertiary accents rather than giving every panel a different finish.

#### 0B.8 Color architecture

A reliable starting model is:

- one neutral / low-saturation base family;
- one secondary structural color;
- one signal accent.

Examples of signal accents seen in the corpus include yellow, cyan, red, orange, purple, and blue.

This is a tendency, not a palette law. Culturally specific, ceremonial, or fashion-led characters may use richer palettes.

The accent should normally repeat **2–4 times** across meaningful sites such as:

- collar / hair detail;
- closure / cable;
- weapon power source;
- shoe / glove detail;
- tail or nonhuman accent;
- ID or equipment tag.

#### 0B.9 Cultural / fantasy reinterpretation

When the concept suggests gothic, knightly, frontier, East-Asian, ceremonial, religious, or aristocratic clothing:

- preserve the recognizable source silhouette;
- modernize construction through seams, closures, panels, belts, equipment mounts, technical fabrics, or protective inserts;
- avoid literal historical cosplay;
- avoid replacing the source with generic military gear.

The result should feel **translated into an industrial-fantasy operator world**, not merely accessorized with pouches.

#### 0B.10 Nonhuman-body accommodation

Phase 2 must adapt to the frozen body:

- horns → headwear/hood/collar clearance;
- ears → hair/headgear opening and sound access;
- tail → explicit rear opening, split coat, raised belt line, or tail-safe layering;
- large tail → silhouette space and movement clearance;
- scales/fur → friction and coverage choices;
- wings / extra limbs / unusual torso → redesigned armholes, cape splits, harness routing, and equipment placement.

Never hide or relocate anatomy merely to reuse a normal human outfit template.

#### 0B.11 Ornament grammar

Arknights ornament tends to look **graphic, institutional, regional, or functional**, not jewelry-for-jewelry's-sake.

Useful ornament sources:

- labels / ID strips;
- unit or organization marks;
- geometric edging;
- woven cultural motifs;
- warning/signal blocks;
- cords and knots with attachment logic;
- repeated hardware geometry;
- localized embroidery or trim.

Avoid dense filigree unless the character's culture/role independently requires it.

#### 0B.12 Phase-2 Arknights test

Before freezing Phase 2, verify:

- Can each major layer be physically understood?
- Does every hardware cluster have a reason?
- Does the outfit accommodate frozen anatomy?
- Does the silhouette have one dominant outer mass rather than indiscriminate layering?
- Are material contrasts intentional?
- Are color accents controlled?
- Does the outfit express a role/environment without collapsing into generic military techwear?

---

### 0C. PHASE 3 — WEAPON GRAMMAR

#### 0C.1 Core weapon thesis

An Arknights weapon should usually read as:

> **a recognizable functional category re-authored through engineered construction, unusual material/power logic, or character-specific interfaces.**

Even supernatural or oversized weapons should normally answer practical questions about how they are held, aimed, braced, carried, deployed, powered, or connected.

#### 0C.2 Supported categories

The corpus directly supports:

- swords / great blades;
- lances / rail-lances / spear systems;
- polearms / halberds;
- staffs / Arts implements;
- firearms;
- shield + firearm combinations;
- compound bows / crossbows;
- giant mechanized crossbow/ballista rigs;
- drill/lance systems;
- crystalline/energy bows;
- weapon + support-device combinations;
- modular artillery / missile-like systems;
- large weapon cases and deployment rigs.

Do not default to firearms merely because the setting feels modern.

#### 0C.3 Engineering readability

Where applicable, define:

- grip;
- trigger;
- stock;
- barrel;
- rail;
- limb/string;
- hinge;
- joint;
- guard;
- shaft;
- mounting bracket;
- magazine or energy housing;
- counterweight;
- brace;
- optic;
- cable;
- shield frame;
- case or deployment support.

Not every weapon needs all of these. It needs enough visible logic that its function is believable.

#### 0C.4 Oversized weapons

Oversized weapons are valid, but size must create a handling consequence.

Use one or more:

- two-handed grip;
- bracing surface;
- shoulder/rest support;
- mechanical assistance;
- counterweight;
- suspension;
- folding/deployment geometry;
- large shield integration;
- body-specific strength/anatomy;
- Arts/mineral energy assistance.

Do not enlarge a weapon only for spectacle while keeping it physically unsupported.

#### 0C.5 Supernatural / Arts / mineral overlay

Magic-like design must remain readable through the resolved weapon's physical material, construction and interfaces under `Character_Creation.md` §§7.13, 7.14 and 7.22.4. A device origin does not authorize an emitted effect.

Construction examples, never templates:

- a crystal blade grown from a mechanical frame;
- bounded luminous constituent matter within a staff head;
- resolved material channels contained within a ring, housing, cable or catalyst component;
- a bow with a defined grip, limbs and bounded intrinsic luminous material;
- the physical launcher, housing and interfaces of a projectile system, without emitted projectiles or trails.

These examples retain category readability and character-specific engineering. They do not create a nonphysical-weapon exception.

#### 0C.6 Weapon + body interface

If the body has unusual anatomy:

- tails may counterbalance or route around equipment;
- extra limbs may support additional grips;
- horns/ears affect shoulder stock and head clearance;
- large wings/tails may constrain back-mounted weapons;
- transformed arms may become direct weapon interfaces;
- unusual scale/strength may justify weapon mass.

Body and weapon should feel co-engineered, not independent.

#### 0C.7 Weapon + outfit relationship

Weapon and outfit do not need identical motifs.

Cohesion may come from:

- shared material finish;
- one accent color;
- repeated fastener geometry;
- matching industrial/cultural construction logic;
- explicit holster, sheath, sling, harness, brace, or case interface.

Functional compatibility is stronger than decorative matching.

#### 0C.8 Weapon detail hierarchy

Use:

1. clear category silhouette;
2. clear handling geometry;
3. one signature mechanism/material hook;
4. secondary hardware;
5. limited labels/marks/details.

Do not start with micro-greeble.

#### 0C.9 Phase-3 Arknights test

Before freezing Phase 3, verify:

- Is the weapon category or function readable?
- Can the character plausibly hold/carry/deploy it?
- Is its supernatural-looking construction intrinsic to the resolved weapon material and geometry, with clear interfaces and no emitted effect?
- Does its scale have support logic?
- Does it fit the frozen body and outfit physically?
- Is there one strong signature mechanism rather than random complexity?
- Is it original rather than a copied operator weapon silhouette?

---

### 0D. CROSS-PHASE COMPILER

#### 0D.1 Identity compression procedure

Before Phase 2, identify the character's strongest design relationships for selective outfit emphasis: geometry, material, colour, proportion and intrinsic identity where applicable. Before Phase 3, identify the frozen body/outfit relationships most useful to weapon authorship: macro form, material, accent and actual interface relationships. These are aesthetic emphasis views, not reduced replacements for the frozen source.

Always retain and consume the complete certified body and outfit records for fit, canonical anchors, topology, material boundaries, attachments, interfaces and validation. No anchor count limits preservation or handoff fidelity. Selecting a few useful motifs does not delete unselected visible decisions. Use only phase-valid, concept-supported vocabulary; the aesthetic orientation supplies no biography, scene or unsupported concept.

A direction refines the strongest valid heroine/main-antagonist-grade female identity. It cannot impose ordinary realism, a stock face or a conventional body template. Every character remains independently authored within its canonical concepts. Do not copy every visual motif into every later-phase system merely to demonstrate coherence.

#### 0D.2 Silhouette budget

Count the following as major silhouette masses:

- large hair;
- horns/antlers;
- tail;
- coat/cape/robe;
- shield;
- giant weapon;
- back rig / support device.

If two or more are already dominant, restrain the next one.

Arknights complexity should feel **layered but legible**, not uniformly maximal.

#### 0D.3 Interface coherence

Cross-phase coherence is strongest when the viewer can understand:

- where equipment attaches;
- what the outer garment protects;
- how anatomy exits the clothing;
- where the weapon rests or deploys;
- how the character carries supplies;
- how signal colors/materials repeat selectively.

#### 0D.4 Genre mixing

Arknights may combine:

- animal anatomy + clinical clothing;
- demon/dragon traits + modern jacket;
- gothic dress + industrial weapon;
- knight armor + modern hardware;
- cultural robe + technical closures;
- formal dress + shield/firearm;
- frontier fur + crystalline spear.

The genres need not match literally. They need to look **authored for one operator**.

---

### 0E. DECISION PRIORITIES

When several valid choices exist, use this order:

1. concept and Character Creation constraints;
2. body topology and movement;
3. role / environment / occupation;
4. silhouette readability;
5. layer and interface logic;
6. weapon function and handling;
7. material hierarchy;
8. signal-color logic;
9. secondary detail.

Do not sacrifice the first six for decorative recognizability.

---

### 0F. ANTI-DRIFT / ANTI-COPY RULES

#### 0F.1 Generic techwear drift

Do not use black clothing + random straps + pouches + neon accent as a shortcut.

Every technical feature must have a reason.

#### 0F.2 Generic military drift

Arknights is not “anime soldier.”

Formal, clinical, aristocratic, gothic, frontier, cultural, and fashion-led silhouettes are all supported.

#### 0F.3 Generic cyberpunk drift

Do not flood the design with neon circuitry, holograms, chrome, and glowing panels unless concept/role requires them.

#### 0F.4 Pure-fantasy cosplay drift

Do not build medieval/fantasy clothing without material, closure, interface, or equipment logic that places it inside the Arknights design language.

#### 0F.5 Kemonomimi-token drift

Do not add ears/tails only to signal Arknights.

Nonhuman traits must be anatomically owned and character-specific.

#### 0F.6 Hardware-clutter drift

Do not add labels, carabiners, zippers, belts, clips, warning stripes, or ID cards unless they support role, silhouette, material transition, or equipment use.

#### 0F.7 Weapon-greeble drift

Do not replace a readable weapon category with arbitrary mechanical complexity.

Macro function comes first.

#### 0F.8 Existing-operator copy drift

Do not copy:

- an exact horn/ear/tail arrangement;
- a known coat cut;
- a famous operator's color stack;
- a recognizable weapon silhouette;
- a signature ID-tag arrangement;
- a distinctive hair gradient;
- a specific shield/rig/crossbow architecture.

The direction is a grammar, not a character kitbash library.

---

### 0G. CHARACTER CREATION EXECUTION INTERFACE

#### 0G.1 Phase 1 interface

After concept fusion has produced a valid body:

1. preserve mandatory anatomy;
2. choose grounded Arknights-compatible proportion refinement;
3. integrate horns/ears/tails/transformation as owned anatomy;
4. establish face/hair identity;
5. define 2–4 dominant body/head anchors;
6. freeze.

Do not design clothing or weapons during Phase 1.

#### 0G.2 Phase 2 interface

Using the frozen body:

1. identify role/environment consequence;
2. choose one main garment family;
3. define inner and outer layers;
4. accommodate anatomy;
5. add only necessary closures/interfaces;
6. establish material hierarchy;
7. apply controlled accent color;
8. verify movement and future weapon accommodation;
9. freeze.

#### 0G.3 Phase 3 interface

Using the frozen body + outfit:

1. choose the concept-valid weapon category;
2. define handling/deployment logic;
3. define one signature engineered/material hook;
4. fit weapon scale to body and support structure;
5. connect it to the outfit through one or two selective echoes;
6. verify carry/holster/sling/case/bracing interfaces;
7. freeze.

#### 0G.4 Random-character / no-theme execution

For a supplied/resolved concept set assigned Arknights, with no additional theme:

- do not start from a known operator archetype;
- derive functional construction constraints from the authorized concepts and frozen systems without inventing a role, environment or narrative;
- derive one body identity hook from the authorized concept materialization, without adding an unrequested species;
- in Phase 2, use medium-complexity functional garment layering;
- use a restrained palette with one signal accent;
- in Phase 3, choose a weapon category independently from famous characters;
- verify that every technical detail has a physical or role-based reason.

---

### 0H. STANDALONE SUFFICIENCY TEST

DD0003 is sufficient without reference images only if an executor can answer all of the following from this document alone:

- How should an Arknights body differ from a generic anime body?
- How are nonhuman traits integrated?
- How are transformed bodies preserved?
- How are outfit layers chosen and constructed?
- How are technical details justified?
- What materials are typical and how are they combined?
- How is color controlled?
- How are cultural/fantasy silhouettes modernized?
- How is a weapon made Arknights-compatible?
- How are oversized or supernatural weapons made functional?
- How are body, outfit, and weapon connected without overmatching motifs?
- What common shorthand must be avoided?

DD0003 v1.0 passes this test.

---

### 0I. VALIDATION CHECKLISTS

#### Phase 1

- [ ] Body follows Character Creation and concept constraints first.
- [ ] Proportions are grounded and character-specific.
- [ ] Nonhuman anatomy is physically owned.
- [ ] Hair/head design accommodates anatomy.
- [ ] The silhouette works without clothing tokens.
- [ ] No existing operator body/head combination has been copied.

#### Phase 2

- [ ] One main garment architecture is readable.
- [ ] Layers are physically understandable.
- [ ] Anatomy accommodation is explicit.
- [ ] Hardware has a reason.
- [ ] Material hierarchy is controlled.
- [ ] Accent color is deliberate.
- [ ] Cultural/fashion source is translated rather than cosplayed.
- [ ] Outfit is not generic military/techwear by default.

#### Phase 3

- [ ] Weapon category/function is readable.
- [ ] Handling/support/deployment logic is visible.
- [ ] Scale is justified.
- [ ] Supernatural-looking material is intrinsic to the resolved weapon construction; no emitted or environmental effect is used.
- [ ] Weapon physically fits body + outfit.
- [ ] One signature mechanism dominates detail hierarchy.
- [ ] Weapon does not copy an existing operator's silhouette.

#### Full character

- [ ] Body, outfit, and weapon read as one authored operator.
- [ ] No single franchise token carries the entire identity.
- [ ] Large silhouette masses are budgeted.
- [ ] Signal accents repeat selectively rather than everywhere.
- [ ] Extraordinary anatomy is treated matter-of-factly.
- [ ] Technical detail remains legible instead of becoming noise.

---

### 0J. CORPUS CONFIDENCE / NON-RESTRICTIONS

#### High-confidence recurring grammar

Across the 20-reference corpus, the strongest repeated signals are:

- matter-of-fact nonhuman anatomy;
- functional material-conscious layering;
- visible attachment/closure/interface logic;
- fashion/culture/role hybridized with technical construction;
- engineered or function-readable weapon systems;
- large but controlled silhouette masses;
- selective signal-color accents;
- restrained microdetail relative to macro structure.

#### Important non-restrictions

Arknights does **not** require:

- one body type;
- one chest proportion;
- one species feature;
- cat ears;
- horns;
- tails;
- black clothing;
- white coats;
- long coats;
- tactical shorts;
- ID cards;
- yellow accents;
- cyan accents;
- military styling;
- giant weapons;
- firearms;
- exposed midriff;
- neutral palettes only.

#### Corpus limit

The 20 references provide a strong executable direction but do not represent every Arknights race, culture, body topology, profession, or weapon category. When Character Creation produces an unrepresented valid case, preserve the concept and extrapolate from the **structural rules** above rather than from surface tokens.



### 0K. DD0003 entry version history

**v1.0:** Compiled the full twenty-reference corpus into the complete executable Phase-1/2/3 specification, cross-phase compiler, no-theme baseline, anti-drift controls, execution interface, sufficiency test and validation checklists; marked DD0003 ACTIVE / COMPLETE. This cleanup preserves that entry ID and version.

**Preserved construction history:** `provenance/Design_Direction_Construction_History.md` retains the complete DD0003 intake authority, twenty individual reference records, provisional ledgers, compilation contract and construction history. These govern evidence preservation during revision, not ordinary generation.

# 7. DATABASE EXPANSION RULE

Every future Design Direction is appended to this document as a new stable entry:

`DD0004`, `DD0005`, `DD0006`,... (after checking that the ID is unused; DD0001–DD0003 already exist)

The registry at the top must be updated whenever an entry is added or materially changes status/version.

Completed entries are never silently compressed when a new direction is added. Their full specifications remain present inside their own categories.

---

# 8. DOCUMENT VERSION HISTORY

## v2.2

- Completed Arknights Set 02/02 and the full 20-reference DD0003 corpus.
- Added ten detailed Set-02 evidence records and completed-corpus construction ledgers.
- Compiled and froze DD0003 Arknights v1.0 as ACTIVE / COMPLETE.
- Added standalone Arknights execution rules for Phase 1 body/identity, Phase 2 clothing, Phase 3 weapons, cross-phase coherence, transformed anatomy, anti-drift, validation, and no-theme generation.
- Preserved all Arknights intake evidence and v0.1–v0.2 construction history beneath the final v1.0 authority.

## v2.1

- Added DD0003 Arknights as the next Design Direction under construction.
- Defined a two-set, twenty-reference non-destructive corpus-intake framework using the same evidence-preservation standard established for Fate.
- Completed Arknights Set 01/02 (10/20 references) and advanced DD0003 construction version to 0.1.
- Added ten detailed immutable source records, Set-01 synthesis, provisional phase ledgers, range/exception evidence, and anti-drift controls.
- No Arknights Set-01 hypothesis has been promoted to final executable authority.

## v2.0

- Completed and froze DD0002 Fate v1.0 from the full 90-reference corpus.
- Added the complete standalone Fate execution specification for Phase 1 body/identity, Phase 2 outfit, Phase 3 weapon, transformed anatomy, cross-phase coherence, anti-drift, and validation.
- Marked Fate ACTIVE / COMPLETE in the registry.
- Corrected the final Fate Phase-3 ruling to preserve `Character_Creation.md` weapon ownership despite corpus examples with absent weapons, props, or support systems.
- Preserved all Fate intake evidence and provisional history beneath the final specification.

## v1.2

- Completed DD0002 Fate Set 01/09 (10/90 references) and advanced Fate construction version to 0.1.
- Added ten detailed immutable source records plus Set-01 synthesis, provisional phase hypotheses, range/exception evidence, and anti-drift controls.
- No Set-01 hypothesis was promoted to final Fate v1.0 authority.

## v1.1

- Added DD0002 Fate's non-destructive 90-reference corpus-intake and evidence-preservation framework.
- Reserved Fate Set 01–09 and stable per-reference IDs.
- Added detailed Phase 1 / Phase 2 / Phase 3 / cross-phase observation schema, hypothesis ledgers, exception tracking, and cumulative construction versioning.
- No Fate stylistic conclusions were invented before corpus intake.

## v1.0

- Created unified `Design_Direction.md` architecture.
- Replaced the one-file-per-style storage model with one canonical multi-entry Design Direction database.
- Added stable Design Direction IDs and registry.
- Added exact activation, entry isolation, phase routing, and standalone sufficiency rules.
- Embedded the complete Shoujo Kaisen specification as DD0001 without summarizing it.
- Reserved DD0002 for Fate as the next direction to construct.
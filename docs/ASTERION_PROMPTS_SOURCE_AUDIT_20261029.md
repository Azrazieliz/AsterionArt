# Asterion Art â€” source prompt retrieval (29 October 2026)

Repository source commit: 20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05

The passages below are exact implementation code, with source line numbers. They show the text actually assembled into model requests, including its dynamic substitutions. They are not invented final expanded prompts. User-specific canonical data, grading and frozen identities are not exported to the public repository.

The previously installed Character Creation renderer divides long positive prompts into chunks and runs separate native image generations. This is a defective design, not a valid transparent extension of a diffusion model text context.

## Private dynamic prompts not published here

The app stores completed, expanded sheet prompts in its internal character-phase1/plans/*.json. Complete Qwen request/response transcripts are not persisted. This report extracts source-defined prompts without leaking private plans or artwork.

## Character contract: original concepts, drawing, negative and layout

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/methodology/CharacterPhaseOne.kt#L12-L55

~~~kotlin
  12 |     const val NEGATIVE=
  13 |         "clothes, garment, lingerie, underwear, uniform, fabric underlayer, " +
  14 |         "armor, decorative props, environment, fashion poses, three-quarter view, " +
  15 |         "cropped limbs, detached appendages, duplicate body templates, " +
  16 |         "redesigned rear view, CGI, 3d rendering, auxiliary infographic panels"
  17 |     const val DRAWING=
  18 |         "Premium native hand-illustrated two-dimensional anime/manhwa style; " +
  19 |         "exact silhouette contours, grouped hard and soft illustration shadows, " +
  20 |         "purpose-designed facial planes and eyes, distinctive sculpted hair masses, " +
  21 |         "material-specific drawn highlight and edge treatment, independently " +
  22 |         "developed palettes and controlled value hierarchies. " +
  23 |         "Full concept integration must remain at major or strong meso scale. " +
  24 |         "Simple strong macro silhouette precedes intricate subordinated microdetail, " +
  25 |         "with quiet regions, coherent form, no photographic or 3D simulation."
  26 |     const val PRESENTATION=
  27 |         "A single exact 16:9 continuous white analytical anatomical reference sheet " +
  28 |         "with five non-overlapping left-to-right character regions. Every identity " +
  29 |         "has exactly three complete uncropped full-body views: one large true-front " +
  30 |         "neutral standing, one smaller exactly 90-degree side profile, one smaller " +
  31 |         "direct-rear view. All three show exactly the SAME frozen character, anatomy, " +
  32 |         "materials, proportions and rooted appendages. No accessory panels or captions. " +
  33 |         "Clinical unclothed female anatomical studies, with deliberate body proportions, " +
  34 |         "skeletal, joint and appendage support, fully visible body substrates; " +
  35 |         "no clothing, fashion layers, concealment garments or artificial coverage. " +
  36 |         "Intrinsically developed scales, fur, skin, minerals, membranes or other " +
  37 |         "concept-derived body matter remain anatomy rather than clothing. " +
  38 |         "No erotic posing or scene. White neutral field, strictly balanced standing."
  39 |     const val AUTHORING=
  40 |         "Design one completely original premium female anime/manhwa character from " +
  41 |         "the strongest coherent physical embodiment of every resolved primary concept. " +
  42 |         "No generic body plus decorative elements, label, glow, costume or skin recolor. " +
  43 |         "All primary concepts construct and mutually transform body substrate, " +
  44 |         "topology, silhouette, skeleton, proportion, face, eyes, hair, materials, " +
  45 |         "cavities or appendages. Every pair produces a consequential shared physical " +
  46 |         "structure; when three or more concepts are present, the complete set has " +
  47 |         "a consequential higher-order shared fusion structure. Preserve complete " +
  48 |         "recognizable concept vocabulary, clearly embodied at major or strong " +
  49 |         "meso scale, without obligatory saturation of every body region. " +
  50 |         "Independently author anatomy, meaningful asymmetry, face, hair, color, " +
  51 |         "materials and flagship identity; resolve appendage roots and joint load " +
  52 |         "paths. Preserve feminine human-character readability even through radical " +
  53 |         "concept anatomy. Finalize aesthetic hierarchy, taste and refinements before " +
  54 |         "image generation. Clinical unclothed reference views, no Outfit/Weapon " +
  55 |         "phase, no Design Direction, no supplementary sub-concepts."
~~~

## Qwen3 original-character system prompt

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/brain/CharacterBrainResolver.kt#L36-L50

~~~kotlin
  36 |     companion object {
  37 |         private const val SYSTEM=
  38 |             "Act as Asterion's specialist original female anime/manhwa character " +
  39 |             "designer. Author clinically unclothed anatomical reference characters, " +
  40 |             "never costumes, fetish poses, set dressing or external concept icons. " +
  41 |             "Every supplied primary concept physically constructs the body and " +
  42 |             "mutually transforms other concepts at major or meso scale. Three or " +
  43 |             "more concepts require consequential complete-set higher-order fusion. " +
  44 |             "Develop novel skeleton, mass distribution, face, eyes, hair, appendage " +
  45 |             "roots, anatomy, topology, material/value/colour architectures and " +
  46 |             "coherent hand-illustrated 2D appearance. Each character is independently " +
  47 |             "authored, aesthetically complete and legibly female despite radical " +
  48 |             "concept anatomy. No Design Direction, sub-concepts or uniform mannequins. " +
  49 |             "Return only the requested output. /no_think"
  50 |     }
~~~

## Qwen canonical evidence and identity JSON / repair / physical fusion prompts

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/brain/CharacterBrainResolver.kt#L127-L423

~~~kotlin
 127 |                 val canonicalFull=mutableMapOf<String,String>()
 128 |                 val canonicalSummaries=mutableMapOf<String,String>()
 129 |                 val explicitGrades=DetailedGradeStore(app).list()
 130 |                 suspend fun conceptEvidence(c:PrimaryConcept,summarize:Boolean):String {
 131 |                     val record=taxonomy.find(c.canonicalId?:c.name)
 132 |                         ?:return "FIXED OPEN LITERAL: "+c.name+
 133 |                             ". Derive complete physical meaning; never replace or invent SC IDs."
 134 |                     val full=canonicalFull.getOrPut(record.id) {
 135 |                         "CANONICAL "+record.id+": "+record.name+"\n"+record.fullRecord
 136 |                     }
 137 |                     // Reading source directly does not require generating hundreds
 138 |                     // of new Qwen tokens. Preserve the exact canonical text when
 139 |                     // the full design request fits the loaded model's context.
 140 |                     if(!summarize)return full
 141 |                     canonicalSummaries[record.id]?.let{return it}
 142 |                     val pages=BrainEvidencePager.pages(full,
 143 |                         (context-1900).coerceAtLeast(256)){engine.countTokens(it)}
 144 |                     val notes=mutableListOf<String>()
 145 |                     for(page in pages) {
 146 |                         progress(completed.size,plan.total,
 147 |                             "Interpreting canonical "+record.id+" "+page.index+"/"+page.total)
 148 |                         notes+=answer(
 149 |                             "Extract ALL mandatory major recognizable anatomical, material, " +
 150 |                                 "fusion, symbiosis and interpretation-boundary knowledge from " +
 151 |                                 "this canonical concept. No character yet. PAGE "+
 152 |                                 page.index+"/"+page.total+"\n"+page.text,
 153 |                             minOf(650,context/6),"canonical_evidence")
 154 |                     }
 155 |                     val evidence="CANONICAL "+record.id+" "+record.name+"\n"+
 156 |                         notes.joinToString("\n")
 157 |                     canonicalSummaries[record.id]=evidence
 158 |                     return evidence
 159 |                 }
 160 |                 for((index,selection) in selections.withIndex()) {
 161 |                     if(index<completed.size)continue
 162 |                     progress(index,plan.total,"Designing "+selection.id)
 163 |                     val descriptions=selection.concepts.map {conceptEvidence(it,false)}
 164 |                     val prior=completed.takeLast(10).joinToString("\n"){
 165 |                         it.id+": "+it.anatomy.take(180)+"; "+it.topology.take(120)+
 166 |                             "; "+it.fusion.take(160)+"; "+it.faceEyesHair.take(110)
 167 |                     }
 168 |                     val grading=GradingGuidance.evidence(
 169 |                         GradingGuidance.applicable(
 170 |                             explicitGrades,CreationMode.CHARACTER,selection.names))
 171 |                     fun authorityFor(evidence:List<String>)=buildString{
 172 |                         append("AUTHOR COMPLETE CHARACTER ").append(selection.id)
 173 |                         append("\nRESOLVED FIXED CONCEPTS: ")
 174 |                         append(selection.names.joinToString(" Ã— "))
 175 |                         append("\n").append(CharacterPhaseOne.AUTHORING)
 176 |                         append("\nDRAWING: ").append(CharacterPhaseOne.DRAWING)
 177 |                         append("\nPRIMARY CANONICAL CONCEPT EVIDENCE:\n")
 178 |                         append(evidence.joinToString("\n"))
 179 |                         append("\nEXISTING INDEPENDENT IDENTITIES (DO NOT COPY):\n")
 180 |                         append(prior)
 181 |                         if(grading.isNotBlank()){
 182 |                             append("\nPREVIOUS USER GRADING (scoped corrections, ")
 183 |                             append("not new methodology rules):\n")
 184 |                             append(grading)
 185 |                         }
 186 |                     }
 187 |                     // Model-generated designs are authoritative, but a single long
 188 |                     // JSON response must not be responsible for 8 fields, up to
 189 |                     // 28 pairwise interactions AND a higher-order structure.
 190 |                     // Missing schema fields caused repeated full expensive jobs
 191 |                     // on actual phone traces. Split the budget by output role.
 192 |                     val format="Return one FLAT valid JSON object with EXACTLY " +
 193 |                         "eight STRING keys: anatomy, face_eyes_hair, topology, " +
 194 |                         "fusion, materials, differentiation, render_description, " +
 195 |                         "audit_evidence. All values must be prose strings, NEVER " +
 196 |                         "nested JSON or arrays. Complete ALL eight fields " +
 197 |                         "before the token budget expires. Budget characters per " +
 198 |                         "field: anatomy 250-520, face_eyes_hair 160-450, " +
 199 |                         "topology 160-450, fusion 160-450, materials 160-400, " +
 200 |                         "differentiation 160-400, render_description 600-1100, " +
 201 |                         "audit_evidence 120-350. Do not spend the response " +
 202 |                         "on the first field. For exactly two concepts, the " +
 203 |                         "fusion field MUST name both and detail their actual " +
 204 |                         "shared anatomical load path and material interface. " +
 205 |                         "Fully design one original " +
 206 |                         "anatomical character, with " +
 207 |                         "complete feminine silhouette, original face/eyes/hair, " +
 208 |                         "rooted appendages, consequential integration of EVERY " +
 209 |                         "named primary concept, materials, identity-specific 2D " +
 210 |                         "drawing and independently authored facial structure. " +
 211 |                         "The render description is a complete positive self-contained " +
 212 |                         "2D reconstruction, not a brief summary. No clothes, no " +
 213 |                         "props, no scenes. Do not include pairwise or higher_order " +
 214 |                         "in THIS response; these are authored separately with an " +
 215 |                         "independent output budget. No Markdown or commentary."
 216 |                     // Keep all eight authored fields but avoid the 2300-token
 217 |                     // response budget that repeatedly ran into multi-minute
 218 |                     // decoding and truncated JSON on the phone.
 219 |                     val outputLimit=minOf(1400,context/3)
 220 |                     fun inputFor(evidence:String)=format+"\nCHARACTER "+selection.id+
 221 |                         "\nPRIMARY: "+selection.names.joinToString(" Ã— ")+
 222 |                         "\n"+CharacterPhaseOne.DRAWING+
 223 |                         "\nDESIGN EVIDENCE:\n"+evidence+"\n/no_think"
 224 |                     suspend fun fits(evidence:String):Boolean =
 225 |                         engine.countTokens(inputFor(evidence))+
 226 |                             engine.countTokens(SYSTEM)+outputLimit+160<context
 227 |                     // Route full canonical evidence to synthesis unchanged whenever
 228 |                     // it fits. Only call Qwen's expensive evidence summarization
 229 |                     // for requests that genuinely cannot fit the model's context.
 230 |                     val fullAuthority=authorityFor(descriptions)
 231 |                     val study=if(fits(fullAuthority)) {
 232 |                         progress(index,plan.total,"Using complete canonical evidence directly")
 233 |                         fullAuthority
 234 |                     } else {
 235 |                         val compressed=selection.concepts.map {conceptEvidence(it,true)}
 236 |                         val authority=authorityFor(compressed)
 237 |                         if(fits(authority))authority else {
 238 |                         val authorityPages=BrainEvidencePager.pages(authority,
 239 |                             (context-1650).coerceAtLeast(256)){engine.countTokens(it)}
 240 |                         val notes=mutableListOf<String>()
 241 |                         for(page in authorityPages) {
 242 |                             progress(index,plan.total,
 243 |                                 "Reading full design evidence "+page.index+"/"+
 244 |                                     page.total+" for "+selection.id)
 245 |                             notes+=answer(
 246 |                                 "Preserve all binding anatomical, material and " +
 247 |                                 "pairwise concept-fusion requirements; no character " +
 248 |                                 "output yet. PAGE "+page.index+"/"+page.total+
 249 |                                 "\n"+page.text,minOf(620,context/6),"design_authority_pages")
 250 |                         }
 251 |                         notes.joinToString("\n")
 252 |                         }
 253 |                     }
 254 |                     val baseInput=inputFor(study)
 255 |                     progress(index,plan.total,
 256 |                         "Qwen3 authoring full anatomical identity "+selection.id)
 257 |                     val raw=answer(baseInput,outputLimit,"identity_json")
 258 |                     val data=CharacterOutputSchema.parseOrRecover(raw).let{partial->
 259 |                         if(CharacterOutputSchema.missingFields(partial).size<
 260 |                             CharacterOutputSchema.identityFields.size)partial else {
 261 |                             // Only repeat the entire schema if not even one
 262 |                             // complete model-written field was recoverable.
 263 |                             CharacterOutputSchema.parseOrRecover(answer(
 264 |                                 "Your prior response lacked complete design fields. "+
 265 |                                 "Return exactly EIGHT flat string fields with anatomically "+
 266 |                                 "specific independent construction; use 600+ characters "+
 267 |                                 "for render_description. No nested objects. "+
 268 |                                 "PRIOR OUTPUT:\n"+raw.take(4500)+
 269 |                                 "\nPRIMARY: "+selection.names.joinToString(" Ã— ")+
 270 |                                 "\n/no_think",outputLimit,"identity_json_recovery"))
 271 |                         }
 272 |                     }
 273 |                     val names=CharacterOutputSchema.identityFields
 274 |                     // Recover several omitted fields in bounded pairs, not one
 275 |                     // expensive prompt-prefill/model call for every omitted key.
 276 |                     // Preserve the valid fields from the original response.
 277 |                     val omitted=CharacterOutputSchema.missingFields(data)
 278 |                     if(omitted.size>=3) {
 279 |                         for(group in omitted.chunked(2)) {
 280 |                             if(group.size<2)continue
 281 |                             progress(index,plan.total,"Recovering "+
 282 |                                 group.joinToString(" + ")+" for "+selection.id)
 283 |                             val retained=names.mapNotNull{key->
 284 |                                 (data.opt(key) as? String)?.takeIf{value->
 285 |                                     value.length>=CharacterOutputSchema.minimumLength(key)
 286 |                                 }?.let{key+": "+it.take(260)}
 287 |                             }.joinToString("\n")
 288 |                             val keys=group.joinToString(", ")
 289 |                             val requirements=group.joinToString("; "){field->
 290 |                                 field+" minimum "+CharacterOutputSchema.minimumLength(field)+
 291 |                                     " characters"
 292 |                             }
 293 |                             val batch=runCatching {
 294 |                                 CharacterOutputSchema.parseOrRecover(answer(
 295 |                                     "Return ONLY one FLAT JSON object with EXACTLY "+
 296 |                                     "these two prose STRING keys: "+keys+
 297 |                                     ". "+requirements+". SAME independent "+
 298 |                                     "character "+selection.id+
 299 |                                     "; preserve the already frozen design "+
 300 |                                     "details. Both values must physically embody "+
 301 |                                     "all primary concepts, not decorative "+
 302 |                                     "references. No nested data or commentary."+
 303 |                                     "\nPRIMARY: "+selection.names.joinToString(" Ã— ")+
 304 |                                     "\nRETAINED DESIGN:\n"+retained+
 305 |                                     "\nBINDING STUDY:\n"+study.take(1800)+
 306 |                                     "\n/no_think",
 307 |                                     minOf(1900,context/3),"field_repair_batch"))
 308 |                             }.onFailure {
 309 |                                 if(it is kotlinx.coroutines.CancellationException) throw it
 310 |                             }.getOrNull()
 311 |                             for(field in group) {
 312 |                                 val value=batch?.optString(field)?.trim().orEmpty()
 313 |                                 if(value.length>=CharacterOutputSchema.minimumLength(field))
 314 |                                     data.put(field,value)
 315 |                             }
 316 |                         }
 317 |                     }
 318 |                     // Repair only fields still absent after the batch. Never
 319 |                     // redo a complete character or overwrite a valid field.
 320 |                     for(field in names) {
 321 |                         val existing=data.opt(field)
 322 |                         val minimum=CharacterOutputSchema.minimumLength(field)
 323 |                         if(field !in CharacterOutputSchema.missingFields(data))
 324 |                             continue
 325 |                         progress(index,plan.total,"Repairing "+field+
 326 |                             " for "+selection.id)
 327 |                         val others=names.mapNotNull{key->
 328 |                             (data.opt(key) as? String)?.takeIf{it.length>=60}
 329 |                                 ?.let{key+": "+it.take(320)}
 330 |                         }.joinToString("\n")
 331 |                         val text=answer(
 332 |                             "Produce ONLY the missing character-design prose " +
 333 |                             "field "+field+" for the SAME independently authored "+
 334 |                             selection.id+". Minimum "+minimum+" characters. "+
 335 |                             "Use every primary concept in physically meaningful "+
 336 |                             "anatomy, not ornament; preserve the already designed "+
 337 |                             "identity. No JSON, no fields besides "+field+
 338 |                             ".\nPRIMARY: "+selection.names.joinToString(" Ã— ")+
 339 |                             "\nRETAINED DESIGN:\n"+others+
 340 |                             "\nBINDING STUDY:\n"+study.take(2400)+
 341 |                             "\n/no_think",
 342 |                             minOf(if(minimum>=600)1000 else 550,context/4),"field_repair")
 343 |                         require(text.length>=minimum) {
 344 |                             selection.id+": neural "+field+" remains incomplete"
 345 |                         }
 346 |                         data.put(field,text)
 347 |                     }
 348 |                     val narrative=names.joinToString("\n"){field->
 349 |                         field+": "+data.getString(field).take(420)
 350 |                     }
 351 |                     val pairs=mutableListOf<PairwisePhysicalFusion>()
 352 |                     val required=CharacterOutputSchema.requiredPairs(selection.names)
 353 |                     for(group in required.chunked(3)) {
 354 |                         progress(index,plan.total,
 355 |                             "Resolving physical concept pairs "+pairs.size+
 356 |                                 "/"+required.size+" for "+selection.id)
 357 |                         val specification=group.joinToString("\n"){
 358 |                             (first,second)->first+" + "+second
 359 |                         }
 360 |                         // One pair needs one explicit physical mechanism, not
 361 |                         // a JSON array round trip plus a second repair call.
 362 |                         // This is the common two-primary-concept Phase-1 path.
 363 |                         if(group.size==1) {
 364 |                             val (first,second)=group.single()
 365 |                             val authored=CharacterOutputSchema.validNamedFusion(
 366 |                                 data.getString("fusion"),first,second)
 367 |                             if(authored!=null) {
 368 |                                 pairs+=PairwisePhysicalFusion(first,second,authored)
 369 |                                 continue
 370 |                             }
 371 |                             val mechanism=answer(
 372 |                                 "Write ONLY one detailed 110+ character physical " +
 373 |                                 "fusion mechanism shared by "+first+" AND "+second+
 374 |                                 " in the SAME frozen anatomical character. " +
 375 |                                 "Specify joint, material or load-path integration " +
 376 |                                 "rather than ornaments or metaphor. Plain prose, " +
 377 |                                 "not JSON.\nIDENTITY:\n"+narrative+"\n/no_think",
 378 |                                 minOf(400,context/6),"pairwise_single")
 379 |                             require(mechanism.length>=65) {
 380 |                                 selection.id+": unresolved physical fusion "+first+" + "+second
 381 |                             }
 382 |                             pairs+=PairwisePhysicalFusion(first,second,mechanism)
 383 |                             continue
 384 |                         }
 385 |                         val response=runCatching {
 386 |                             CharacterOutputSchema.parseObject(answer(
 387 |                                 "Return exactly one JSON object with key pairwise "+
 388 |                                 "whose value is an ARRAY of "+group.size+
 389 |                                 " objects. Every object must have STRING keys "+
 390 |                                 "first, second and mechanism. Mechanism must be "+
 391 |                                 "at least 65 characters of concrete, shared "+
 392 |                                 "physical anatomy; no labels, metaphors, icons "+
 393 |                                 "or decorative attachment substitutes. The "+
 394 |                                 "primary names must match exactly. REQUIRED PAIRS:\n"+
 395 |                                 specification+"\nFROZEN DESIGN:\n"+narrative+
 396 |                                 "\n/no_think",minOf(1000,context/4),"pairwise_batch"))
 397 |                         }.onFailure{if(it is kotlinx.coroutines.CancellationException)throw it}
 398 |                             .getOrNull()?.optJSONArray("pairwise")
 399 |                         for((first,second) in group) {
 400 |                             val mechanism=CharacterOutputSchema.validMechanism(
 401 |                                 response,first,second)?:
 402 |                                 answer(
 403 |                                     "Write ONLY a concrete 90+ character shared "+
 404 |                                     "physical anatomical mechanism for "+first+
 405 |                                     " AND "+second+" within THIS frozen female "+
 406 |                                     "character. No metaphor or decorative "+
 407 |                                     "substitution.\nIDENTITY:\n"+narrative+
 408 |                                     "\n/no_think",minOf(400,context/6),"pairwise_repair")
 409 |                             require(mechanism.trim().length>=65){
 410 |                                 selection.id+": unresolved physical fusion "+
 411 |                                     first+" + "+second
 412 |                             }
 413 |                             pairs+=PairwisePhysicalFusion(first,second,mechanism)
 414 |                         }
 415 |                     }
 416 |                     val higher=if(selection.concepts.size<3)"" else answer(
 417 |                         "Write ONLY a concrete 140+ character anatomically "+
 418 |                         "consequential higher-order structure which physically "+
 419 |                         "requires ALL the primary concepts together, not merely "+
 420 |                         "pairwise associations. Same frozen identity. "+
 421 |                         "PRIMARY: "+selection.names.joinToString(" Ã— ")+
 422 |                         "\nDESIGN:\n"+narrative+"\n/no_think",
 423 |                         minOf(500,context/6),"higher_order")
~~~

## Image prompt builder per frozen character

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/methodology/CharacterPhaseOne.kt#L203-L229

~~~kotlin
 203 | object CharacterSheetCompiler {
 204 |     /** Complete character-specific visual state for controlled Alter/Fuse revisions. */
 205 |     fun individual(design:FrozenCharacter):String {
 206 |         design.validate()
 207 |         return buildString {
 208 |             append("One self-contained 9:16 clinical anatomical reference of a ")
 209 |             append("complete unclothed original female anime/manhwa character; ")
 210 |             append("dominant neutral full-body exact true-front and smaller exact ")
 211 |             append("90-degree profile and direct rear of identical frozen anatomy. ")
 212 |             append("Pure white field, no clothing or reference underlayer.\n")
 213 |             append(CharacterPhaseOne.DRAWING).append("\n")
 214 |             append("FROZEN CHARACTER ").append(design.id).append("\n")
 215 |             append("ANATOMY: ").append(design.anatomy).append("\n")
 216 |             append("FACE / EYES / HAIR: ").append(design.faceEyesHair).append("\n")
 217 |             append("TOPOLOGY AND APPENDAGES: ").append(design.topology).append("\n")
 218 |             append("CONCEPT FUSION: ").append(design.fusion).append("\n")
 219 |             design.pairwise.forEach{physical->
 220 |                 append("JOINT PHYSICAL STRUCTURE: ").append(physical.mechanism)
 221 |                     .append("\n")
 222 |             }
 223 |             if(design.higherOrder.isNotBlank())
 224 |                 append("HIGHER-ORDER COMPLETE-SET STRUCTURE: ")
 225 |                     .append(design.higherOrder).append("\n")
 226 |             append("MATERIAL AND PALETTE: ").append(design.materials).append("\n")
 227 |             append("FINAL VISUAL CHARACTER: ").append(design.renderDescription)
 228 |         }
 229 |     }
~~~

## Exact positive sheet prompt builder

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/methodology/CharacterPhaseOne.kt#L244-L280

~~~kotlin
 244 |     fun sheet(designs:List<FrozenCharacter>,sheet:Int):String {
 245 |         require(sheet>=1&&designs.size==CharacterPhaseOne.PER_SHEET)
 246 |         designs.forEachIndexed{index,identity->
 247 |             require(identity.number==(sheet-1)*CharacterPhaseOne.PER_SHEET+index+1)
 248 |             identity.validate()
 249 |         }
 250 |         return buildString {
 251 |             append(CharacterPhaseOne.PRESENTATION).append("\n")
 252 |             append(CharacterPhaseOne.DRAWING).append("\n")
 253 |             append("FROZEN SELF-CONTAINED ANATOMICAL CHARACTER PAYLOADS:\n")
 254 |             designs.forEach{design->
 255 |                 append("\nIDENTITY ").append(design.id).append("\n")
 256 |                 append("ANATOMY: ").append(design.anatomy).append("\n")
 257 |                 append("FACE, EYES AND HAIR: ").append(design.faceEyesHair).append("\n")
 258 |                 append("ROOTED TOPOLOGY: ").append(design.topology).append("\n")
 259 |                 append("FUSED BODY CONSTRUCTION: ").append(design.fusion).append("\n")
 260 |                 design.pairwise.forEach{physical->
 261 |                     append("SHARED STRUCTURE OF ").append(physical.first)
 262 |                         .append(" AND ").append(physical.second).append(": ")
 263 |                         .append(physical.mechanism).append("\n")
 264 |                 }
 265 |                 if(design.higherOrder.isNotBlank())
 266 |                     append("COMPLETE-SET PHYSICAL FUSION: ")
 267 |                         .append(design.higherOrder).append("\n")
 268 |                 append("ILLUSTRATED MATERIAL ARCHITECTURE: ")
 269 |                     .append(design.materials).append("\n")
 270 |                 append("FINAL SELF-CONTAINED CHARACTER: ")
 271 |                     .append(design.renderDescription.trim()).append("\n")
 272 |             }
 273 |             append("COMPLETE SAME-IDENTITY VIEW LOCK: independently preserve " +
 274 |                 "every body, attachment root, anatomy, material and physical " +
 275 |                 "fusion construction across exact front/profile/rear. The five " +
 276 |                 "dominant full bodies and ten subordinate views remain clinically " +
 277 |                 "unclothed, analytically neutral, equally high-quality, uncropped.")
 278 |         }
 279 |     }
 280 | }
~~~

## Image conditioning schedule

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/engine/ConditioningSchedule.kt#L1-L123

~~~kotlin
   1 | package com.asterion.art.engine
   2 |
   3 | /**
   4 |  * Lossless prompt scheduling for real, bounded native text encoders.
   5 |  *
   6 |  * This DOES NOT enlarge CLIP, Qwen image conditioning, or QNN tensors.
   7 |  * It schedules all evidence over distinct denoising passes, asking the
   8 |  * installed backend's real /tokenize endpoint to prove each pass fits.
   9 |  * No substring is quietly discarded. If even one unit cannot fit, fail.
  10 |  */
  11 | data class ConditioningChunk(
  12 |     val index:Int, val total:Int, val positive:String, val negative:String,
  13 |     val denoise:Float, val verifiedTokens:Int, val verifiedLimit:Int
  14 | )
  15 | data class ConditioningPlan(
  16 |     val sourceCharacters:Int, val emittedCharacters:Int,
  17 |     val chunks:List<ConditioningChunk>, val mode:String="sequential_passes"
  18 | )
  19 |
  20 | object ConditioningSchedule {
  21 |     private const val HEADER="Asterion design continuation. Preserve all previously established " +
  22 |         "identity, anatomy, composition, materials and visual anchors. Add or correct the following " +
  23 |         "required details without deleting previously rendered content:\n"
  24 |
  25 |     /**
  26 |      * tokenizer MUST count with the selected native model; don't use word estimates.
  27 |      * No backend model supports arbitrarily large simultaneous cross attention.
  28 |      */
  29 |     fun compile(prompt:String,negative:String,tokenizer:(String)->TokenCapacity,
  30 |                 maximumPasses:Int=32,
  31 |                 continuationHeader:String?=null):ConditioningPlan {
  32 |         require(prompt.isNotBlank())
  33 |         require(maximumPasses in 1..64)
  34 |         // The source-to-pass ledger is lossless, including owner whitespace.
  35 |         val positive=prompt
  36 |         val negativeClean=negative.trim()
  37 |         val negativeCount=if(negativeClean.isBlank())null else tokenizer(negativeClean).also {
  38 |             require(it.fits && it.limit>0){"Negative prompt exceeds native conditioning capacity"}
  39 |         }
  40 |         val direct=tokenizer(positive)
  41 |         require(direct.limit>0){"Native text encoder has not reported a measurable token limit"}
  42 |         if(direct.fits) {
  43 |             return ConditioningPlan(positive.length,positive.length,listOf(
  44 |                 ConditioningChunk(1,1,positive,negativeClean,1.0f,
  45 |                     direct.count,direct.limit)),"single_pass")
  46 |         }
  47 |         // A complete lossless span ledger. Binary search for the largest span
  48 |         // the model's ACTUAL tokenizer can encode; keep all source characters.
  49 |         val chunks=mutableListOf<String>()
  50 |         var offset=0
  51 |         val shorter="Preserve prior image. Add these required details:\n"
  52 |         val preferred=continuationHeader?.takeIf{it.isNotBlank()}
  53 |         if(preferred!=null)require(tokenizer(preferred).fits){
  54 |             "Character identity anchor exceeds native tokenizer capacity"
  55 |         }
  56 |         val continuation=when {
  57 |             preferred!=null -> preferred
  58 |             tokenizer(HEADER).fits -> HEADER
  59 |             tokenizer(shorter).fits -> shorter
  60 |             else -> error("Native context is too small for safe continuation")
  61 |         }
  62 |         while(offset<positive.length) {
  63 |             require(chunks.size<maximumPasses) {
  64 |                 "Required prompt needs more than $maximumPasses verified passes"
  65 |             }
  66 |             val prefix=if(chunks.isEmpty())"" else continuation
  67 |             var lo=1
  68 |             var hi=positive.length-offset
  69 |             var maxFit=0
  70 |             while(lo<=hi) {
  71 |                 val middle=lo+(hi-lo)/2
  72 |                 val candidate=prefix+positive.substring(offset,offset+middle)
  73 |                 val proof=tokenizer(candidate)
  74 |                 require(proof.limit>0){"Unmeasurable native encoder context"}
  75 |                 if(proof.fits) {maxFit=middle;lo=middle+1}
  76 |                 else hi=middle-1
  77 |             }
  78 |             require(maxFit>0){"A mandatory section cannot fit in native context"}
  79 |             var length=maxFit
  80 |             // Avoid cutting a UTF-16 surrogate pair in the original record.
  81 |             if(offset+length<positive.length &&
  82 |                 Character.isHighSurrogate(positive[offset+length-1]) &&
  83 |                 Character.isLowSurrogate(positive[offset+length])) {
  84 |                 length--
  85 |             }
  86 |             require(length>0)
  87 |             // Prefer natural word breaks, without losing intervening whitespace.
  88 |             if(offset+length<positive.length) {
  89 |                 val part=positive.substring(offset,offset+length)
  90 |                 val lastSpace=part.indexOfLast{it.isWhitespace()}
  91 |                 if(lastSpace>length/2 && lastSpace<length-1) {
  92 |                     val proposed=lastSpace+1
  93 |                     if(tokenizer(prefix+positive.substring(offset,offset+proposed)).fits)
  94 |                         length=proposed
  95 |                 }
  96 |             }
  97 |             val chunk=positive.substring(offset,offset+length)
  98 |             require(tokenizer(prefix+chunk).fits)
  99 |             chunks+=chunk
 100 |             offset+=length
 101 |         }
 102 |         require(chunks.isNotEmpty())
 103 |         require(chunks.joinToString("")==positive) {
 104 |             "Prompt content was changed or lost during scheduling"
 105 |         }
 106 |         val parts=chunks.mapIndexed { i,chunk ->
 107 |             val text=if(i==0)chunk else continuation+chunk
 108 |             val proof=tokenizer(text)
 109 |             require(proof.fits && proof.limit>0)
 110 |             // Subsequent passes are controlled refinement, NOT a claim of
 111 |             // simultaneous attention to earlier textual evidence.
 112 |             val strength=when {
 113 |                 i==0 -> 1f
 114 |                 i<3 -> .44f
 115 |                 i<8 -> .29f
 116 |                 else -> .17f
 117 |             }
 118 |             ConditioningChunk(i+1,chunks.size,text,negativeClean,strength,
 119 |                 proof.count,proof.limit)
 120 |         }
 121 |         return ConditioningPlan(positive.length,chunks.sumOf{it.length},parts)
 122 |     }
 123 | }
~~~

## Character image prompt and native image pass invocation

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/work/GenerationExecutor.kt#L243-L402

~~~kotlin
 243 |             if(characterPlan!=null) {
 244 |                 GeneratedImagePublisher.atomicWrite(File(applicationContext.filesDir,
 245 |                     "generation-checkpoints/"+job.id+"/model-load-ms.txt"),
 246 |                     loadElapsed.toString().toByteArray())
 247 |                 repo.updateTaskProgress(taskId,0,0,
 248 |                     "QNN model ready after "+loadElapsed+" ms")
 249 |             }
 250 |             // Character Creation prompts are per-sheet and must be checked AFTER
 251 |             // the actual five blueprint descriptions are compiled.
 252 |             val capacityCheck = runCatching {
 253 |                 if (job.creationMode != CreationMode.CHARACTER) {
 254 |                     recipe.passes.forEach { pass ->
 255 |                         engine.verifyText(pass.positive)
 256 |                         if (pass.negative.isNotBlank()) engine.verifyText(pass.negative)
 257 |                     }
 258 |                 }
 259 |             }
 260 |             if (capacityCheck.isFailure && model.ditKind != "qwen21" &&
 261 |                 model.ditKind != "klein" && visualModel != null) {
 262 |                 // Don't truncate authoritative prompt text. Switch to a genuine
 263 |                 // reference-conditioned engine instead of dropping source facts.
 264 |                 model = visualModel
 265 |                 recipe = if(characterPlan!=null) GenerationRecipe(
 266 |                     job.mode,listOf(GenerationPass(1,characterPlan.prompts.first(),
 267 |                         CharacterPhaseOne.NEGATIVE,1f)),job.sheetIds,job.settings)
 268 |                 else if(!job.designPrompt.isNullOrBlank()) GenerationRecipe(
 269 |                     job.mode,listOf(GenerationPass(1,job.designPrompt,
 270 |                         job.designNegative.orEmpty(),1f)),job.sheetIds,job.settings)
 271 |                 else if(job.creationMode in listOf(CreationMode.CHARACTER,CreationMode.CREATURE))
 272 |                     GenerationPlanner.compileConcepts(
 273 |                         job.concepts,requireNotNull(job.creationMode),job.mode,job.settings,taxonomy)
 274 |                 else GenerationPlanner.compileVisual(evidence,job.mode,job.settings,
 275 |                     job.creationMode,job.concepts,job.sourceImageUri!=null,taxonomy)
 276 |                 size = model.generationSize.coerceIn(512, 1536)
 277 |                 // A fallback QNN engine must preserve the narrow, upright
 278 |                 // individual-character triptych, never switch to the final
 279 |                 // 16:9 five-character sheet's landscape resolution.
 280 |                 resolution = com.asterion.art.nativeengine.NativeResolutionPolicy
 281 |                     .choose(model,if(characterPlan!=null)"16:45" else job.settings.ratio)
 282 |                 foreground("Prompt exceeded text context; using " + model.name + " visual conditioning")
 283 |                 engine.prepare(model, resolution.first, resolution.second) { stopped() }
 284 |                 recipe.passes.forEach { pass ->
 285 |                     check(engine.inspectText(pass.positive).limit>0) {
 286 |                         "Selected model does not report native token capacity"
 287 |                     }
 288 |                 }
 289 |             } else {
 290 |                 if(capacityCheck.isFailure &&
 291 |                     capacityCheck.exceptionOrNull()?.message
 292 |                         ?.contains("exceeds",ignoreCase=true)!=true) {
 293 |                     capacityCheck.getOrThrow()
 294 |                 }
 295 |             }
 296 |             val extraOriginal = job.sourceImageUri?.let { uri ->
 297 |                 SheetRecord(role=SheetRole.CHARACTER,label="Original source image",
 298 |                     sourceUris=listOf(uri),referenceSlots=listOf("PRIMARY"))
 299 |             }
 300 |             // The GENERATED authoritative master sheet must reach the
 301 |             // neural reference-conditioning path. Keep original source pixels
 302 |             // available as secondary evidence, without replacing the master.
 303 |             val combinedSourceUris=sheets.flatMap { sheet ->
 304 |                 listOfNotNull(sheet.outputUri)+sheet.sourceUris
 305 |             }.distinct()+
 306 |                 listOfNotNull(extraOriginal?.sourceUris?.firstOrNull())+
 307 |                 job.designReferenceUris
 308 |             val referenceInputs = if (model.ditKind == "qwen21" ||
 309 |                 model.ditKind == "klein") {
 310 |                 if(combinedSourceUris.isEmpty())emptyList()
 311 |                 else SourceReferenceEncoder.encodeUris(
 312 |                     applicationContext,combinedSourceUris)
 313 |             } else emptyList()
 314 |             // Render changes from the selected individual design's real pixels,
 315 |             // never a full multi-character catalogue or only its saved prompt.
 316 |             val originalSource=job.designReferenceUris.singleOrNull()
 317 |                 ?:extraOriginal?.sourceUris?.firstOrNull()
 318 |                 ?:job.directReferenceUris.values.flatten().distinct().singleOrNull()
 319 |                 // Anima/SDXL cannot consume multi-reference images as a
 320 |                 // separate conditioning tensor. Keep the actual primary
 321 |                 // character pixels as an img2img source rather than silently
 322 |                 // replacing the selected character with prose alone.
 323 |                 ?:if(job.creationMode==null)
 324 |                     sheets.flatMap{it.sourceUris}.distinct().singleOrNull()
 325 |                   else null
 326 |             val originalImage=originalSource?.let {
 327 |                 SourceReferenceEncoder.encodeUris(applicationContext,listOf(it)).single()
 328 |             }
 329 |
 330 |             val intermediate = File(applicationContext.filesDir,
 331 |                 "generation-checkpoints/" + job.id).also { it.mkdirs() }
 332 |             if(characterPlan!=null) {
 333 |                 val capsules=JSONArray()
 334 |                 characterPlan.frozen.forEach { d ->
 335 |                     capsules.put(JSONObject().apply {
 336 |                         put("id",d.id)
 337 |                         put("index",d.number)
 338 |                         put("concepts",JSONArray(d.primary.map{it.name}))
 339 |                         put("anatomy",d.anatomy)
 340 |                         put("face_eyes_hair",d.faceEyesHair)
 341 |                         put("topology",d.topology)
 342 |                         put("fusion",d.fusion)
 343 |                         put("materials",d.materials)
 344 |                         put("differentiation",d.differentiation)
 345 |                         put("render_description",d.renderDescription)
 346 |                         put("audit_evidence",d.auditEvidence)
 347 |                         put("frozen",true)
 348 |                     })
 349 |                 }
 350 |                 GeneratedImagePublisher.atomicWrite(
 351 |                     File(applicationContext.filesDir,
 352 |                         "generation-plans/"+job.id+"-capsules.json"),
 353 |                     JSONObject().put("schema","asterion.character-frozen.v2")
 354 |                         .put("job_id",job.id)
 355 |                         .put("design_plan_id",characterPlan.id)
 356 |                         .put("designs",capsules)
 357 |                         .toString(2).toByteArray(Charsets.UTF_8))
 358 |             }
 359 |             val out = mutableListOf<JSONObject>()
 360 |             var technicalLeaderScore = Double.NEGATIVE_INFINITY
 361 |             var technicalLeader: ByteArray? = null
 362 |             var technicalLeaderUri: android.net.Uri? = null
 363 |             val tunedSettings=com.asterion.art.nativeengine.NativeTurboDefaults
 364 |                 .apply(job.settings,model,freeArtwork=job.creationMode==null &&
 365 |                     job.designPrompt.isNullOrBlank())
 366 |             val turboAuto=com.asterion.art.nativeengine.NativeTurboDefaults
 367 |                 .applies(job.settings,model)
 368 |             if(turboAuto)repo.updateTaskProgress(taskId,0,0,
 369 |                 "Anima Turbo Â· optimized for on-device NPU: 8 steps, CFG 1")
 370 |             val count = com.asterion.art.engine.OriginalOutputPolicy.imageCount(
 371 |                 job.creationMode,!job.designPrompt.isNullOrBlank(),
 372 |                 characterPlan?.sheets,tunedSettings.batch)
 373 |             // v3 Character Creation explicitly prescribes ONE generation call
 374 |             // per fully specified analytical sheet; artwork uses five passes.
 375 |             val oneShotCharacterSheet = !job.designPrompt.isNullOrBlank() ||
 376 |                 characterPlan!=null
 377 |             val stageSequence = if (oneShotCharacterSheet)
 378 |                 listOf(recipe.passes.first()) else recipe.passes
 379 |             val passesPerCandidate = stageSequence.size
 380 |             repo.updateTaskProgress(taskId,0,(count*passesPerCandidate+2)*100,"Beginning generation")
 381 |             for (candidate in 1..count) {
 382 |                 if (stopped()) throw CancellationException("Job stopped")
 383 |                 val seed = job.settings.seed ?: ((job.createdAt + candidate * 1049L) and 0x7FFFFFFFL)
 384 |                 // Turbo distillation is calibrated to CFG=1; automatic
 385 |                 // preference perturbations must not silently re-enable the
 386 |                 // prohibitively expensive unconditional guidance path.
 387 |                 val candidateSettings = if(turboAuto)tunedSettings else
 388 |                     PreferenceLearning.candidate(tunedSettings,candidate - 1)
 389 |                 val image:ByteArray=if(characterPlan!=null) {
 390 |                     // Five independent Character Creation triptychs use a
 391 |                     // 16:45 output crop. Anima/SDXL still load their fixed
 392 |                     // 1024-square QNN CANVAS; the native server crops 16:45.
 393 |                     val nativeSettings=candidateSettings.copy(ratio="16:45",batch=1)
 394 |                     val tiles=(0 until CharacterPhaseOne.PER_SHEET).map{cell ->
 395 |                         if(stopped())throw CancellationException("Generation cancelled")
 396 |                         val d=characterPlan.frozen[(candidate-1)*5+cell]
 397 |                         val checkpoint=File(intermediate,
 398 |                             "sheet_"+candidate+"_"+d.id+"_triptych.png")
 399 |                         val triptych=if(checkpoint.isFile && checkpoint.length()>1024L) {
 400 |                             val existing=checkpoint.readBytes()
 401 |                             check(TechnicalImageScorer.score(existing).score.isFinite())
 402 |                             existing
~~~

## Other original creation methodology prompts

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/methodology/DetailedMethodologies.kt#L62-L148

~~~kotlin
  62 |     val all: Map<CreationMode,MethodologyContract> = listOf(
  63 |         MethodologyContract(
  64 |             CreationMode.CHARACTER,"Character Creation v3",
  65 |             "Character Creation v3/characters_creation.md + Concepts.md + Design_Direction.md",
  66 |             emptySet(),SourcePolicy.NONE,2,
  67 |             OutputGeometry("16:9",5,3,5,1,frontAndRearViews=true),
  68 |             OutputGeometry("9:16",1,1,1,1,frontAndRearViews=true),
  69 |             emptySet(),setOf("Original body architecture","Face","Hair","Concept fusion"),
  70 |             "Character Creation Phase 1 ONLY. User chooses 1â€“5 sheets; each is a continuous 16:9 clinical unclothed anatomy sheet of exactly five independently authored premium 2D anime/manhwa female characters, each with an uncropped large true front, smaller exact 90-degree profile and direct rear of the SAME body. All five times selected sheet count must be fully resolved, independently differentiated and frozen BEFORE rendering. Every concept is physically embodied in each original character; pairwise and higher-order shared fusion is mandatory. All characters require coherent skeletal and appendage roots, uniquely authored bodies, face/eyes/hair, palette/material logic, flagship aesthetic quality and specific line-led illustrated 2D rendering grammar. No clothes, anatomical covering underlayers, fashion presentation, props or scenes. Genuine skin and concept-derived body substrates remain visible anatomical matter, never garments. There are no Design Directions, secondary sub-concepts, Outfit/Weapon phases or Final Assembly within Character Creation. The user authorizes rendering using Validate and generate.",
  71 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW),
  72 |             mustFreezeBeforeNextPhase=true
  73 |         ),
  74 |         MethodologyContract(
  75 |             CreationMode.STYLE_CORRECTION,"Art Style Correction",
  76 |             "Art Style Correction/art style sanitisation.md",
  77 |             setOf(SheetRole.ART_STYLE),SourcePolicy.REQUIRED,0,source,source,
  78 |             identity + setOf("Pose","Composition","Scene","Outfit","Props","Camera","Source colors as content anchors"),
  79 |             setOf("Drawing grammar","Linework","Shading","Render materials","Rendering finish"),
  80 |             "Read target Art Style Sheet as style-only authority, and the original illustration as content-only authority. Preserve subjects, identity, camera, crop, pose, clothing, scene, color identities and geometry. Replace entire source rendering treatment with the target style; do not hybridize old/new styles or import characters depicted on the style sheet. One final re-render per original, same aspect ratio and framing.",
  81 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
  82 |         ),
  83 |         MethodologyContract(
  84 |             CreationMode.OUTFIT,"Outfit Creation",
  85 |             "Outfit Creation/Outfit Creation.md",
  86 |             emptySet(),SourcePolicy.CHARACTER_OR_IMAGE,0,ten,portrait,
  87 |             freeze,setOf("Garment construction","Coverage","Materials","Outfit colors","Ornament"),
  88 |             "Freeze source character body, face, hair, proportions and concept anatomy. Default exactly ten structurally and materially different outfits on one 16:9 white 2x5 catalogue. Use analytical headless forms matching frozen morphology when needed. Do not reauthor body, insert weapons or scenes. Selected preview: exact 9:16 white full-body original character wearing chosen outfit.",
  89 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW,StageAction.CONSTRUCTION)
  90 |         ),
  91 |         MethodologyContract(
  92 |             CreationMode.WEAPON,"Weapon Creation",
  93 |             "Weapon Creation/Weapon Creation.md",
  94 |             emptySet(),SourcePolicy.CHARACTER_OR_IMAGE,0,ten,portrait,
  95 |             freeze + setOf("Selected frozen outfit"),setOf("Category","Weapon geometry","Grip","Materials","Ornament"),
  96 |             "Freeze character and selected outfit. Default exactly ten distinct, complete, ISOLATED readable weapons on a single white 16:9 two-row by five-column catalogue. No character, hand, scene or decorative effects. Simple category-recognizable macro structure with premium character-specific fine construction. Preview may show original full-body character holding selected weapon only after user selection.",
  97 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW,StageAction.CONSTRUCTION)
  98 |         ),
  99 |         MethodologyContract(
 100 |             CreationMode.TRANSFORMATION,"Transformation Creation",
 101 |             "Transformation Creation/Transformation Creation.md",
 102 |             emptySet(),SourcePolicy.CHARACTER_OR_IMAGE,0,five,portrait,
 103 |             identity,setOf("Body state","Materialization","Appendage state","Outfit adaptation"),
 104 |             "Same frozen individual in coherent alternate forms. Default five distinct uncropped, full-body neutral characters across one 16:9 analytical white sheet. No new identity, scenery or unrelated props. Single selected preview is 9:16.",
 105 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
 106 |         ),
 107 |         MethodologyContract(
 108 |             CreationMode.MUTATION,"Mutation Creation",
 109 |             "Mutation/Mutation Creation.md",
 110 |             emptySet(),SourcePolicy.CHARACTER_OR_IMAGE,0,five,portrait,
 111 |             setOf("Source continuity","Selected protected anchors"),
 112 |             setOf("Morphology","Asymmetry","Material","Appendages","Silhouette"),
 113 |             "Mutation is controlled deviation, not a power-state transformation. Default 70 percent source identity retention and 30 percent mutation pressure; five radically different uncropped full-body mutants on one 16:9 white analytical sheet. Mutation must affect structure; do not use generic costume swaps.",
 114 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
 115 |         ),
 116 |         MethodologyContract(
 117 |             CreationMode.CREATURE,"Creature Creation",
 118 |             "Creature/Creature Creation.md",
 119 |             emptySet(),SourcePolicy.OPTIONAL,1,five,portrait,
 120 |             emptySet(),setOf("Body plan","Locomotion","Sensory anatomy","Material fusion"),
 121 |             "Create original creatures with coherent full body plans, locomotion, sensory systems and structural concept symbiosis. Five different complete, uncropped creatures in one continuous 16:9 white analytical catalogue; no characters, props or scenes. The source owner is optional and relational, not a source to redraw.",
 122 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
 123 |         ),
 124 |         MethodologyContract(
 125 |             CreationMode.ALTERNATE_UNIVERSE,"Alternate Universe",
 126 |             "Alternate Version/Alternate Universe Creation.md",
 127 |             emptySet(),SourcePolicy.CHARACTER_OR_IMAGE,1,five,portrait,
 128 |             identity,setOf("World logic","Equipment","Technology","Clothing","Role"),
 129 |             "The SAME source character must naturally belong to five systematically different alternate universes on a 16:9 white analytical five-design catalogue. Preserve distinctive identity while reauthoring outfit, weapons and material logic for each world. No generic cosplay costume swaps or environment; single AU preview is a neutral uncropped 9:16 portrait.",
 130 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
 131 |         ),
 132 |         MethodologyContract(
 133 |             CreationMode.FEMINISATION,"Feminisation",
 134 |             "Feminisation/feminisation_reconstructed_current.md",
 135 |             emptySet(),SourcePolicy.REQUIRED,0,portrait,portrait,
 136 |             setOf("Recognizable identity anchors","Source concept","Canonical non-gender identity"),
 137 |             setOf("Female anatomy","Head and face","Hair","Outfit","Body proportions"),
 138 |             "Create one complete, uncropped 9:16 full-body female character on a plain white field. " +
 139 |                 "Reconstruct head shape, face, eye architecture, hair design, anatomical proportions, " +
 140 |                 "silhouette, clothing and materials as an independently authored female design. " +
 141 |                 "Preserve recognizable source concept anchors without copying the original masculine " +
 142 |                 "body template. Rebuild garments around the new body, never merely open the male outfit. " +
 143 |                 "Level 4.5 means comprehensive coordinated female redesign across every body system, " +
 144 |                 "not a numeric instruction for the diffusion model. One figure only, no extra panels.",
 145 |             setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
 146 |         )
 147 |     ).associateBy { it.mode }
 148 |
~~~

## General artwork, source-image and multimodal prompt builders

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/engine/GenerationPlanner.kt#L33-L270

~~~kotlin
  33 | object GenerationPlanner {
  34 |     private val alphaNum = Regex(".*[\\p{L}\\p{N}].*")
  35 |     private fun clean(words: List<String>): List<String> =
  36 |         words.map { it.trim().replace(Regex("\\s+"), " ") }
  37 |             .filter { it.length in 3..280 && alphaNum.matches(it) }
  38 |             .distinct()
  39 |
  40 |     private fun join(words: List<String>) = clean(words).joinToString(", ")
  41 |
  42 |     private fun enforceRoles(sources: List<SourceEvidence>, creation: CreationMode?,
  43 |                              concepts: List<String>, sourceIllustration: Boolean) {
  44 |         val selected = sources.groupingBy { it.role }.eachCount()
  45 |         val errors = if (creation==null) {
  46 |             // Artwork may begin from ONE actual image or any selected image
  47 |             // authority; it must not require three unrelated sheet types.
  48 |             if(sourceIllustration||sources.isNotEmpty())emptyList()
  49 |             else listOf("Select at least one image or master-sheet reference")
  50 |         } else MethodologyRegistry.validate(
  51 |             creation,selected,concepts.size,sourceIllustration)
  52 |         require(errors.isEmpty()) { errors.joinToString("; ") }
  53 |     }
  54 |     private fun directive(creation: CreationMode?, sources: List<SourceEvidence>,
  55 |                           concepts: List<String>, sourceIllustration: Boolean): String =
  56 |         if (creation == null) "" else MethodologyRegistry.compileDirective(
  57 |             creation,sources.groupingBy { it.role }.eachCount(),
  58 |             concepts.size,sourceIllustration)
  59 |
  60 |     fun compile(
  61 |         sources: List<SourceEvidence>,
  62 |         mode: RenderMode,
  63 |         settings: GeneratorSettings,
  64 |         creation: CreationMode? = null,
  65 |         concepts: List<String> = emptyList(),
  66 |         sourceIllustration: Boolean = false,
  67 |         resolver:ConceptResolver?=null
  68 |     ): GenerationRecipe {
  69 |         enforceRoles(sources,creation,concepts,sourceIllustration)
  70 |         require(sources.all { it.all().isNotEmpty() }) {
  71 |             "At least one selected sheet has no machine-readable visual specification. " +
  72 |             "Do not invent missing details."
  73 |         }
  74 |         val characters = sources.filter { it.role == SheetRole.CHARACTER }
  75 |         val themes = sources.filter { it.role == SheetRole.THEME }
  76 |         val styles = sources.filter { it.role == SheetRole.ART_STYLE }
  77 |         val elements = sources.filter { it.role == SheetRole.ELEMENT }
  78 |
  79 |         fun pieces(items: List<SourceEvidence>, vararg slots: String): List<String> =
  80 |             items.flatMap { it.select(*slots) }
  81 |         fun everything(items: List<SourceEvidence>): List<String> = items.flatMap { it.all() }
  82 |
  83 |         // Visual metadata in the authoritative order:
  84 |         // identity, outfit+pose, theme, environment+elements, style+render+camera+lighting.
  85 |         val character = join(pieces(characters, "PRIMARY", "FACE", "EYES", "HAIR"))
  86 |         val outfitAndPose = join(
  87 |             pieces(characters, "OUTFIT", "BODY", "DISTINCTIVE") +
  88 |                 pieces(themes, "POSE")
  89 |         )
  90 |         val theme = join(pieces(themes, "PRIMARY", "VIEW"))
  91 |         val environmentAndElements = join(
  92 |             pieces(themes, "ENVIRONMENT", "ELEMENT") +
  93 |                 everything(elements)
  94 |         )
  95 |         val appearance = join(
  96 |             pieces(styles, "PRIMARY", "FINGERPRINT", "SHAPE", "COMPOSITION", "LIGHT", "COLOR")
  97 |         )
  98 |         val core = listOf(directive(creation,sources,concepts,sourceIllustration),
  99 |             concepts.joinToString(", "), character, outfitAndPose, theme,
 100 |             environmentAndElements, appearance)
 101 |             .filter { it.isNotBlank() }.joinToString("\n\n")
 102 |         require(core.isNotBlank()) { "No interpretable prompt content in selected reference images." }
 103 |
 104 |         val positiveDetails = listOf(
 105 |             pieces(characters, "FACE", "EYES", "HAIR", "BODY", "OUTFIT"),
 106 |             pieces(styles, "MATERIAL", "LIGHT", "COLOR") +
 107 |                 pieces(themes, "LIGHT", "ENVIRONMENT"),
 108 |             pieces(characters, "DISTINCTIVE", "DIAGNOSTIC") +
 109 |                 pieces(elements, "STRUCTURE", "PLACEMENT", "VARIATIONS", "MATERIAL", "COLOR"),
 110 |             pieces(styles, "FINGERPRINT", "COMPOSITION") +
 111 |                 pieces(themes, "VIEW")
 112 |         )
 113 |         val negative = join(
 114 |             pieces(styles, "NEGATIVE") +
 115 |                 pieces(themes, "NEGATIVE") +
 116 |                 pieces(elements, "NEGATIVE")
 117 |         )
 118 |
 119 |         val denoise = listOf(1f, 0.48f, 0.32f, 0.22f, 0.13f)
 120 |         val passes = (1..5).map { n ->
 121 |             val extra = if (n == 1) emptyList() else positiveDetails[n - 2]
 122 |             val full = if (extra.isEmpty()) core else core + "\n\n" + join(extra)
 123 |             GenerationPass(n, full, negative, denoise[n - 1])
 124 |         }
 125 |         return UniversalCreativePlanner.enrich(
 126 |             GenerationRecipe(mode, passes, sources.map { it.sheetId }, settings),
 127 |             creation,concepts,sources,resolver)
 128 |     }
 129 |
 130 |     /**
 131 |      * One-source artwork: the actual image remains the neural img2img input.
 132 |      * This is not a prompt-only substitute and requires source pixels in the
 133 |      * corresponding GenerationWorker invocation.
 134 |      */
 135 |     fun compileFromSource(mode:RenderMode,settings:GeneratorSettings):GenerationRecipe {
 136 |         val base=when(mode) {
 137 |             RenderMode.ANIME -> "Create a coherent high-quality original anime illustration"
 138 |             RenderMode.REALISTIC -> "Create a coherent photorealistic illustration"
 139 |             RenderMode.COSPLAY -> "Create a cohesive realistic cosplay interpretation"
 140 |             RenderMode.OTHER -> "Create a coherent illustration for the selected visual category"
 141 |             RenderMode.ANIMATION -> error("Use the video engine for animation")
 142 |         }+" conditioned on the PROVIDED SOURCE IMAGE PIXELS. Preserve exact "+
 143 |             "recognizable identity, face and hair architecture, character anatomy, "+
 144 |             "established clothing and materials, source objects and composition "+
 145 |             "unless the user explicitly requests a localized change. Do not invent "+
 146 |             "alternate characters or erase the provided source authority."
 147 |         val corrections=listOf(
 148 |             "Reconstruct source-consistent geometry and material appearance.",
 149 |             "Refine face, hair, anatomy and preserved primary identity.",
 150 |             "Improve local shading, physical material behavior and perspective.",
 151 |             "Clarify small clothing and object construction without changing identity.",
 152 |             "Finalize line quality and visual coherence without scene drift."
 153 |         )
 154 |         val strength=listOf(settings.denoise,
 155 |             minOf(settings.denoise,0.45f),0.30f,0.20f,0.12f)
 156 |         return GenerationRecipe(mode,corrections.mapIndexed{i,detail->
 157 |             GenerationPass(i+1,base+" "+detail,"",strength[i])
 158 |         },emptyList(),settings)
 159 |     }
 160 |
 161 |     /**
 162 |      * Real multimodal alternative for Qwen/FLUX reference-editing engines.
 163 |      * Blank OCR does not imply blank reference images. No characteristics are
 164 |      * invented by this prompt: the native model must inspect source pixels.
 165 |      */
 166 |     fun compileVisual(
 167 |         sources: List<SourceEvidence>,
 168 |         mode: RenderMode,
 169 |         settings: GeneratorSettings,
 170 |         creation: CreationMode? = null,
 171 |         concepts: List<String> = emptyList(),
 172 |         sourceIllustration: Boolean = false,
 173 |         resolver:ConceptResolver?=null
 174 |     ): GenerationRecipe {
 175 |         enforceRoles(sources,creation,concepts,sourceIllustration)
 176 |         val groups=sources.groupBy { it.role }
 177 |         val roleLabels=listOf(
 178 |             SheetRole.CHARACTER to "character identity and physical form",
 179 |             SheetRole.ART_STYLE to "visual rendering style",
 180 |             SheetRole.THEME to "scene arrangement, pose and interaction",
 181 |             SheetRole.ELEMENT to "visual elements and their placement"
 182 |         ).filter { (role,_) -> groups[role].orEmpty().isNotEmpty() }
 183 |         val numbered=roleLabels.mapIndexed { i,(_,label) ->
 184 |             "Reference group "+(i+1)+" establishes "+label+"."
 185 |         }.joinToString(" ")
 186 |         val look=when(mode) {
 187 |             RenderMode.ANIME,RenderMode.ANIMATION ->
 188 |                 "Create an anime illustration."
 189 |             RenderMode.REALISTIC ->
 190 |                 "Create a photographically realistic fantasy illustration; preserve any nonhuman traits."
 191 |             RenderMode.COSPLAY ->
 192 |                 "Create a photoreal cosplay interpretation; preserve the canonical wings, horns, ears, tails and marks."
 193 |             RenderMode.OTHER ->
 194 |                 "Create a faithful image for the selected custom category; use provided sources as authority."
 195 |         }
 196 |         val base=look+" "+directive(creation,sources,concepts,sourceIllustration)+" "+concepts.joinToString(", ")+
 197 |             " Use ONLY actual reference images for character identity, style, scene and mandatory details. "+
 198 |             numbered+(if(creation==null) " Render one coherent artwork, not a labeled reference-sheet infographic. "+
 199 |                 "Never invent a missing canonical trait or omit an established one."
 200 |              else " Follow the selected methodology's EXACT output layout, view count and " +
 201 |                 "design count. Catalogue sheets and analytical labels are permitted ONLY when " +
 202 |                 "required by that methodology; keep source identity constraints intact.")
 203 |         val modifiers=listOf(
 204 |             "",
 205 |             "Preserve established character identity, facial features, outfit geometry and body interactions.",
 206 |             "Refine material response, lighting, surface reflections and environmental coherence.",
 207 |             "Refine existing fine details, ornaments, hair, accessories and element microstructure.",
 208 |             "Final rendering coherence without altering the original composition or established subjects."
 209 |         )
 210 |         val denoise=listOf(if (creation==CreationMode.STYLE_CORRECTION) .5f else 1f,
 211 |             .45f,.32f,.22f,.13f)
 212 |         val passes=(1..5).map { index ->
 213 |             GenerationPass(index,base+" "+modifiers[index-1],"",denoise[index-1])
 214 |         }
 215 |         return UniversalCreativePlanner.enrich(
 216 |             GenerationRecipe(mode,passes,sources.map { it.sheetId },settings),
 217 |             creation,concepts,sources,resolver)
 218 |     }
 219 |
 220 |
 221 |     /**
 222 |      * Character Creation v3 and original Creature Creation start without sheets.
 223 |      * The selected, frozen user concepts are the authoritative task input.
 224 |      */
 225 |     fun compileConcepts(concepts: List<String>, creation: CreationMode,
 226 |                         mode: RenderMode, settings: GeneratorSettings,
 227 |                         resolver:ConceptResolver?=null): GenerationRecipe {
 228 |         require(creation==CreationMode.CHARACTER || creation==CreationMode.CREATURE)
 229 |         enforceRoles(emptyList(),creation,concepts,false)
 230 |         val specification=MethodologyRegistry.modes.getValue(creation)
 231 |         val frozen=concepts.distinct().joinToString(" Ã— ")
 232 |         val base=specification.directive+"\nPrimary concepts: "+frozen+
 233 |             "\nProduce a faithful 2D visual output. Follow exact framing and candidate count."+
 234 |             " No unrelated environmental scenery or textual labels except the specified candidate numbers."
 235 |         val details=listOf("Establish every major structure and all characters in this pass.",
 236 |             "Stabilize body architecture, silhouette, face, head and visual identity.",
 237 |             "Resolve anatomical and material interaction with consistent lighting.",
 238 |             "Detail intrinsic morphology, hair, surfaces and smaller structure without redesign.",
 239 |             "Finalize faithful visual coherence and clean neutral presentation.")
 240 |         val passes=(1..5).map { i ->
 241 |             GenerationPass(i,base+"\n"+details[i-1],"",listOf(1f,.46f,.32f,.21f,.13f)[i-1])
 242 |         }
 243 |         return UniversalCreativePlanner.enrich(
 244 |             GenerationRecipe(mode,passes,emptyList(),settings),
 245 |             creation,concepts,emptyList(),resolver)
 246 |     }
 247 |
 248 |     fun selectModelId(models: List<Pair<String, String>>, mode: RenderMode): String? {
 249 |         // One automatic route shown to the user; the ranking operates internally.
 250 |         // This is NOT a claim of checkpoint weight fusion.
 251 |         val rank = when (mode) {
 252 |             RenderMode.ANIME, RenderMode.ANIMATION ->
 253 |                 listOf("rin_flanime", "miaomiao", "hassaku", "noobai")
 254 |             RenderMode.REALISTIC, RenderMode.COSPLAY ->
 255 |                 listOf("cyberrealistic", "realism", "sam_anima_realistic", "rin_flanime")
 256 |             RenderMode.OTHER -> emptyList()
 257 |         }
 258 |         // Imported QNN IDs are randomized. Match their actual labels even
 259 |         // when upstream uses spaces, hyphens or underscores for one family.
 260 |         // This preserves the owner's existing model-quality preference order.
 261 |         fun normalized(value:String):String =
 262 |             value.lowercase().replace(Regex("[^a-z0-9]+"),"_").trim('_')
 263 |         return rank.firstNotNullOfOrNull { needle ->
 264 |             models.firstOrNull { (id, name) ->
 265 |                 normalized(id).contains(needle) ||
 266 |                     normalized(name).contains(needle)
 267 |             }?.first
 268 |         }
 269 |     }
 270 | }
~~~

## Single-image sheet reconstruction prompts

Original: https://github.com/Azrazieliz/AsterionCompiler/blob/20cc9900c6f42dc6b61bb5455ca9cf1abe45bf05/app/src/main/java/com/asterion/art/work/SheetMasterGenerationWorker.kt#L1-L210

~~~kotlin
   1 | package com.asterion.art.work
   2 |
   3 | import android.app.Notification
   4 | import android.app.NotificationChannel
   5 | import android.app.NotificationManager
   6 | import android.content.Context
   7 | import android.content.pm.ServiceInfo
   8 | import android.graphics.Bitmap
   9 | import android.graphics.BitmapFactory
  10 | import androidx.work.*
  11 | import com.asterion.art.data.ArtRepository
  12 | import com.asterion.art.data.GeneratedImagePublisher
  13 | import com.asterion.art.engine.GenerationPass
  14 | import com.asterion.art.render.OneShotSheetConditioning
  15 | import com.asterion.art.render.NeuralSheetPanelRenderer
  16 | import com.asterion.art.engine.SourceReferenceEncoder
  17 | import com.asterion.art.model.SheetRole
  18 | import com.asterion.art.model.TaskKind
  19 | import com.asterion.art.model.TaskStatus
  20 | import com.asterion.art.nativeengine.EmbeddedEngineFactory
  21 | import com.asterion.art.render.SheetMasterPrompts
  22 | import com.asterion.art.vision.VisionModelStore
  23 | import com.asterion.art.vision.ReferenceVisualInterpreter
  24 | import com.asterion.art.engine.SheetTextReader
  25 | import kotlinx.coroutines.Dispatchers
  26 | import kotlinx.coroutines.withContext
  27 | import java.io.ByteArrayOutputStream
  28 | import java.util.concurrent.CancellationException
  29 | import kotlin.math.abs
  30 |
  31 | /**
  32 |  * Native, reference-conditioned master-sheet generation from ONE source image by default.
  33 |  * No per-panel crop copying, no multi-photo requirement, no artificial success.
  34 |  */
  35 | class SheetMasterGenerationWorker(ctx:Context,params:WorkerParameters):
  36 |     CoroutineWorker(ctx,params) {
  37 |     companion object {
  38 |         private const val CHANNEL="asterion-sheet-neural"
  39 |         fun enqueue(context:Context,sheetId:String,taskId:String) {
  40 |             val request=OneTimeWorkRequestBuilder<SheetMasterGenerationWorker>()
  41 |                 .setInputData(Data.Builder().putString("sheet",sheetId)
  42 |                     .putString("task",taskId).build())
  43 |                 .addTag("asterion-sheet-neural").build()
  44 |             WorkManager.getInstance(context).enqueueUniqueWork(
  45 |                 "asterion-sheet-"+sheetId,ExistingWorkPolicy.KEEP,request)
  46 |         }
  47 |     }
  48 |     override suspend fun doWork():Result=withContext(Dispatchers.IO) {
  49 |         val sheetId=inputData.getString("sheet")?:return@withContext Result.failure()
  50 |         val task=inputData.getString("task")?:return@withContext Result.failure()
  51 |         val app=applicationContext
  52 |         val repo=ArtRepository(app)
  53 |         val sheet=repo.sheets.firstOrNull{it.id==sheetId}
  54 |             ?:return@withContext Result.failure()
  55 |         val manager=app.getSystemService(NotificationManager::class.java)
  56 |         manager.createNotificationChannel(NotificationChannel(CHANNEL,
  57 |             "Neural reference sheet generation",NotificationManager.IMPORTANCE_LOW))
  58 |         suspend fun foreground(label:String,resetProgress:Boolean=true) {
  59 |             setForeground(ForegroundInfo(7308,Notification.Builder(app,CHANNEL)
  60 |                 .setSmallIcon(android.R.drawable.ic_menu_gallery)
  61 |                 .setContentTitle("Generating "+sheet.role.display+" master sheet")
  62 |                 .setContentText(label).setOngoing(true).build(),
  63 |                 ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
  64 |             if(resetProgress)repo.updateTaskProgress(task,0,0,label)
  65 |         }
  66 |         var engine:com.asterion.art.engine.StillImageEngine?=null
  67 |         try {
  68 |             repo.updateTask(task,TaskStatus.RUNNING)
  69 |             foreground("Reading your original image")
  70 |             val sources=sheet.sourceUris
  71 |             require(sources.isNotEmpty()){"Select at least one reference image"}
  72 |             val encoded=SourceReferenceEncoder.encodeUris(app,sources)
  73 |             val observations=if(VisionModelStore(app).installed()!=null){
  74 |                 foreground("Analyzing actual reference pixels")
  75 |                 val evidence=ReferenceVisualInterpreter(app).enrich(
  76 |                     listOf(sheet),listOf(SheetTextReader.read(app,sheet)),onProgress={
  77 |                         repo.updateTaskProgress(task,0,0,it)
  78 |                     })
  79 |                 evidence.single().all().joinToString(" ")
  80 |             }else ""
  81 |             val basePositive=SheetMasterPrompts.positive(
  82 |                 sheet.role,observations,sheet.originatingOriginal)
  83 |             val baseNegative=SheetMasterPrompts.negative(sheet.role)
  84 |             val reasoner=com.asterion.art.brain.AsterionReasoner(app)
  85 |             val neural=if(reasoner.available()){
  86 |                 foreground("Interpreting source anatomy and sheet construction")
  87 |                 reasoner.compile(
  88 |                     domain=com.asterion.art.engine.UniversalCreativePlanner
  89 |                         .sheetDomain(sheet.role),
  90 |                     canonicalMethod=basePositive,
  91 |                     concepts=emptyList(),basePositive=basePositive,
  92 |                     baseNegative=baseNegative,jobId=sheet.id,
  93 |                     candidateCount=1,
  94 |                     sheetSources=listOf(observations).filter{it.isNotBlank()},
  95 |                     onStage={stage->repo.updateTaskProgress(task,0,0,stage)},
  96 |                     creationMode=sheet.originatingOriginal)
  97 |             }else null
  98 |             val negative=baseNegative+if(neural==null||neural.negative.isBlank())""
  99 |                 else ", "+neural.negative
 100 |             engine=EmbeddedEngineFactory.open(app)
 101 |             val models=engine.installedModels()
 102 |             require(models.isNotEmpty()){"Install a compatible local image-generation model"}
 103 |             val conditioned=models.filter{
 104 |                 it.ditKind in setOf("qwen21","klein")
 105 |             }
 106 |             // Honor the user's explicit image-model output assignment, rather
 107 |             // than loading the first imported checkpoint arbitrarily.
 108 |             val tagged=com.asterion.art.nativeengine.ModelOutputRouting(app)
 109 |                 .preferred(com.asterion.art.model.RenderMode.ANIME,models)
 110 |             val primary=tagged?:conditioned.firstOrNull()?:models.first()
 111 |             val resolution=com.asterion.art.nativeengine.NativeResolutionPolicy
 112 |                 .choose(primary,"9:16")
 113 |             // Anima/SDXL MUST load their fixed 1024-square QNN canvas and
 114 |             // request 9:16 via JSON aspect_ratio. Only dynamic DiT models
 115 |             // may request a genuinely non-square native canvas.
 116 |             if(!primary.isAnima&&!primary.isSdxl)
 117 |                 require(abs(resolution.first.toDouble()/resolution.second-9.0/16.0)<0.16) {
 118 |                     "The selected DiT model does not support a 9:16 canvas"
 119 |                 }
 120 |             foreground("Loading local QNN model Â· "+primary.name)
 121 |             engine.prepare(primary,resolution.first,resolution.second){isStopped}
 122 |             // The authoritative Character Sheet V5 calls for one complete
 123 |             // reference-conditioned image, not multiple partially repainted
 124 |             // panels or token-sliced denoising passes.
 125 |             val oneShot=runCatching {
 126 |                 OneShotSheetConditioning.select(
 127 |                     basePositive,neural?.positive,negative,engine::inspectText)
 128 |             }
 129 |             if(oneShot.isFailure) {
 130 |                 val cause=requireNotNull(oneShot.exceptionOrNull())
 131 |                 val message=cause.message.orEmpty()
 132 |                 if(!message.contains("context",ignoreCase=true) &&
 133 |                     !message.contains("exceeds",ignoreCase=true))
 134 |                     throw cause
 135 |                 repo.updateTaskProgress(task,0,9,
 136 |                     "Model context is too short for one-shot sheet; "+
 137 |                     "generating nine distinct source-conditioned views instead")
 138 |             }
 139 |             val positive=oneShot.getOrNull()
 140 |             // Preserve the user's checkpoint-specific sampler settings.
 141 |             // Raising every model to 25 steps overrides distilled QNN defaults
 142 |             // and can degrade quality as well as generation time.
 143 |             val selected=com.asterion.art.nativeengine.NativeTurboDefaults
 144 |                 .apply(repo.settings,primary).copy(batch=1,ratio="9:16",
 145 |                     automatic=false)
 146 |             val referencePixels=if(primary.ditKind in setOf("qwen21","klein"))
 147 |                 encoded else emptyList()
 148 |             val previous=if(referencePixels.isEmpty()) {
 149 |                 // On Anima/SDXL use the source pixels through true img2img;
 150 |                 // never discard the only reference.
 151 |                 val decoded=BitmapFactory.decodeByteArray(encoded[0],0,encoded[0].size)
 152 |                     ?:error("Cannot decode canonical reference")
 153 |                 try{
 154 |                     ByteArrayOutputStream().use{out->
 155 |                         check(decoded.compress(Bitmap.CompressFormat.PNG,100,out))
 156 |                         out.toByteArray()
 157 |                     }
 158 |                 }finally{decoded.recycle()}
 159 |             }else null
 160 |             if(isStopped)throw CancellationException()
 161 |             val output=if(positive!=null) {
 162 |                 engine.verifyText(positive)
 163 |                 if(negative.isNotBlank())engine.verifyText(negative)
 164 |                 foreground("Reconstructing complete 9:16 sheet Â· one image",false)
 165 |                 engine.generate(
 166 |                     GenerationPass(1,positive,negative,
 167 |                         if(previous==null)1.0f else 0.78f),
 168 |                     selected,resolution.first,resolution.second,
 169 |                     previous,selected.seed?:System.currentTimeMillis(),{isStopped},
 170 |                     {done,total->
 171 |                         repo.updateTaskProgress(task,done.coerceAtLeast(0),
 172 |                             total.coerceAtLeast(0),
 173 |                             "One complete image Â· native diffusion "+
 174 |                             done+"/"+total.coerceAtLeast(1))
 175 |                     },referencePngs=referencePixels).png
 176 |             } else {
 177 |                 // The user supplies ONE image. Every panel below is a NEW
 178 |                 // generated view; no source-image crop is substituted for a
 179 |                 // missing view or passed off as a completed master sheet.
 180 |                 foreground("Generating nine actual anatomical and detail views",false)
 181 |                 NeuralSheetPanelRenderer.build(app,sheet.id,sheet.role,
 182 |                     sheet.originatingOriginal,engine,selected,
 183 |                     resolution.first,resolution.second,encoded.first(),
 184 |                     referencePixels,{isStopped},{completed,total,label->
 185 |                         repo.updateTaskProgress(task,completed,total,label)
 186 |                     })
 187 |             }
 188 |             val info=BitmapFactory.Options().apply{inJustDecodeBounds=true}
 189 |             BitmapFactory.decodeByteArray(output,0,output.size,info)
 190 |             require(info.outWidth>0&&info.outHeight>0){"Native model returned invalid image"}
 191 |             require(abs(info.outWidth.toDouble()/info.outHeight-9.0/16.0)<0.06) {
 192 |                 "The QNN pipeline returned "+info.outWidth+"Ã—"+info.outHeight+
 193 |                     " instead of the required 9:16 sheet. A model with an "+
 194 |                     "operational aspect-ratio VAE encoder is needed; the "+
 195 |                     "square output will not be incorrectly saved as a sheet."
 196 |             }
 197 |             val saved=GeneratedImagePublisher.publish(app,sheet.id,1,output)
 198 |             repo.finishSheet(sheet.id,saved.toString())
 199 |             repo.updateTask(task,TaskStatus.COMPLETED)
 200 |             Result.success()
 201 |         }catch(e:CancellationException){
 202 |             repo.updateTask(task,TaskStatus.CANCELLED,"Neural sheet generation cancelled")
 203 |             Result.failure()
 204 |         }catch(e:Throwable) {
 205 |             repo.updateTask(task,TaskStatus.FAILED,
 206 |                 "Neural sheet creation: "+(e.message?:e.javaClass.simpleName))
 207 |             Result.failure()
 208 |         }finally{runCatching{engine?.release()}}
 209 |     }
 210 | }
~~~

## Model-agnostic architecture note

Character Creation is only one workflow. Artwork creation, style correction, outfits, weapons, transformation, mutation, creatures, visual reference reconstruction, grading and video retain separate prompt/methodology contracts. The source audit does not imply that a Qwen-specific design system is appropriate for all workflows.

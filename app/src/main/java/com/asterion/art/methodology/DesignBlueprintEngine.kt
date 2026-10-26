package com.asterion.art.methodology

/** Structured design decisions precede any image-model call. Not a trained reasoning model. */
data class DesignBlueprint(
    val number: Int,
    val concepts: List<String>,
    val morphology: String,
    val faceAndHair: String,
    val internalMechanics: String,
    val conceptFusion: String
) {
    val id: String get() = "D" + number.toString().padStart(2, '0')
    fun render(): String = id + ": " + morphology + "; " + faceAndHair +
        "; " + internalMechanics + "; " + conceptFusion + "."
}

object DesignBlueprintEngine {
    private val bodyPlans = listOf(
        "tall elegant body, long legs and narrow ribcage",
        "compact powerful physique, broad shoulders and muscular thighs",
        "soft curvy balanced body with elongated neck",
        "petite sharp anatomy with extended forearms",
        "monumental broad-hipped body with long limbs",
        "athletic inverted-triangle silhouette with defined waist",
        "lithe short torso, long legs, angular joints",
        "strong lower-body proportions with distinctive load-bearing calves",
        "elongated soft pear-shaped silhouette",
        "medium athletic body with a compact torso",
        "broad powerful physique with strong rib and pelvic supports",
        "slender long-limbed asymmetrical silhouette",
        "compact rounded body with short structural arms",
        "very tall muscular female physique with heavy support joints",
        "medium statuesque body with broad upper back"
    )
    private val faces = listOf(
        "heart-shaped face, aqua almond eyes, long blue-black layered hair",
        "strong square jaw, amber eyes, short ivory bob",
        "oval face, violet upturned eyes, high dark-violet hair",
        "soft triangular face, green eyes, chestnut wavy locks",
        "elongated face, pale-gold eyes, thick black hair",
        "diamond face, copper eyes, dark-green wedge hair",
        "sharp tapered face, indigo eyes, pearly-white hair",
        "strong cheekbones, crimson eyes, silver braids",
        "small round face, silver eyes, charcoal hair with layered locks",
        "high cheekbones, magenta eyes, copper-red hair",
        "compact squared face, teal eyes, deep-plum hair",
        "slender angular face, gray-blue eyes, black-red hair",
        "soft oval face, golden-brown eyes, fluffy taupe bob",
        "long structured face, lavender eyes, pale-gray flowing hair",
        "round forehead, dark amber eyes, ash-blue short hair"
    )
    private val mechanics = listOf(
        "dorsal support struts and symmetrical anatomical wing-root options",
        "wingless articulated spinal and pelvic reinforcement",
        "long articulated tail with coherent vertebral transitions",
        "asymmetric rooted appendages supported by ribs and pelvis",
        "paired bounded openings with continuous structural paths",
        "tensile fiber networks connected through joints",
        "overlapping body-built segmented forearm and rib structures",
        "radial branching supports integrated into skull and back",
        "intrinsic transparent cavities with thick load-bearing rims",
        "continuous long tail and compact supported wing architecture",
        "mechanical interlocking rings with aligned joint axes",
        "layered membranes and differentiated scale structures",
        "rooted nonhuman appendage system with readable balance",
        "rib-to-tail support structure with continuous tendons",
        "paired horn roots and intrinsic channel networks"
    )
    private fun interpretation(name: String): String {
        val word=name.lowercase()
        return when {
            "antimatter" in word -> "contained matter-discontinuities, load-bearing boundary rings and inverted internal density"
            "cloud" in word || "mist" in word -> "condensed volumetric cloud tissue, layered vapor boundaries and rooted soft-hard transitions"
            "oni" in word -> "cranial horn origins, jaw architecture, body strength and reinforced bone attachments"
            "dragon" in word -> "integrated scales, horns, structural wing roots, claws and a long axial tail"
            "galaxy" in word || "cosmos" in word -> "stellar particulate cavities and orbit-like intrinsic body structures"
            "void" in word -> "literal bounded missing volumes with visibly supporting rims"
            "chaos" in word -> "deliberate asymmetry that preserves the major anatomical load paths"
            "flower" in word || "rose" in word -> "layered petal masses, rooted stems and soft-to-hard transitions"
            "spider" in word -> "radial tendon organization, articulated additional limb roots and silk structures"
            "crystal" in word -> "facet-bearing rigid elements and optically distinct translucent mineral"
            "ocean" in word || "water" in word -> "pressure-supported clear fluid channels and contained liquid interfaces"
            "machine" in word || "robot" in word -> "internal joint frames, load-bearing bearings and cable-based tendons"
            "fire" in word || "flame" in word -> "thermal channels and refractory body-owned structures"
            "moon" in word -> "rocky pale highlands, dark basins and crescent-curve anatomy"
            "moth" in word || "butterfly" in word -> "rooted membranous wing planes with vein and scale structures"
            else -> "recognizable anatomical and material features of " + name +
                " constructed as actual body systems rather than motifs"
        }
    }
    fun population(concepts: List<String>, count: Int = 15): List<DesignBlueprint> {
        val names=concepts.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        require(names.size >= 2) { "Select two or more different concepts" }
        require(count in 1..60)
        return (0 until count).map { i ->
            val fusion=names.indices.joinToString("; ") { j ->
                val index=(j+i)%names.size
                val region=when((i+j)%5) {
                    0->"head and horn supports"
                    1->"torso and ribcage"
                    2->"arms and legs"
                    3->"tail and back appendages"
                    else->"skin and internal body material"
                }
                names[index] + " reshapes " + region + " through " + interpretation(names[index])
            }
            DesignBlueprint(i+1,names,bodyPlans[i%bodyPlans.size],
                faces[(i*7)%faces.size],mechanics[(i*3)%mechanics.size],fusion)
        }
    }
    fun sheetPrompt(concepts:List<String>, sheet:Int):String {
        require(sheet in 0..2)
        return "Premium 2D anime/manhwa orthographic five-character board, continuous clean white 16:9. " +
            "Five unique complete uncropped female identities from left to right, each with dominant " +
            "standing front and smaller true 90-degree side and direct rear views of the exact same body; " +
            "all feet visible; body-owned concept materialization, not outfits, icons or backgrounds.\n" +
            population(concepts).drop(sheet*5).take(5).joinToString("\n") { it.render() } +
            "\nCoherent illustrated materials, differentiated characters, consistent anatomy in every view."
    }
    fun negative():String = "cropped body, missing legs, duplicated design, identical figures, " +
        "upper and lower halves misplaced, broken anatomy, mismatched profiles, costume-only concept, " +
        "repeated uniform, CGI, 3d render, scenery, unrelated props, unreadable labels"
}

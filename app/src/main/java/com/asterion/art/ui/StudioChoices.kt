package com.asterion.art.ui

import com.asterion.art.model.CreationMode

/** Expandable selectable design vocabularies. Stable IDs survive catalogue growth. */
object StudioChoices {
    private val au = listOf(
        "Cyberpunk","Biopunk","Steampunk","Dieselpunk","Solarpunk","Atompunk",
        "Clockpunk","Post-cyberpunk","Space opera","Hard science fiction",
        "Mecha civilization","Post-human future","Virtual reality","Synthetic biology",
        "Alien occupation","Interstellar frontier","Post-apocalyptic",
        "Nuclear wasteland","Flooded Earth","Frozen world","Desert world",
        "Dark fantasy","High fantasy","Low fantasy","Sword and sorcery",
        "Magitech","Arcane industrial","Urban occult","Gothic horror",
        "Cosmic horror","Mythological world","Divine celestial realm",
        "Demonic underworld","Spirit realm","Eastern cultivation","Wuxia","Xianxia",
        "Samurai era","Viking age","Classical antiquity","Medieval Europe",
        "Renaissance","Industrial revolution","Victorian","Art deco",
        "Roaring twenties","World-war era","Cold-war espionage","Contemporary urban",
        "Near-future military","Dystopian dictatorship","Utopian society",
        "Nomadic society","Pirate age","Underwater civilization",
        "Floating-island society","Deep jungle world","Ancient lost civilization",
        "Court aristocracy","High-fashion world","Sports academy",
        "Dark academia","Traditional folklore","Monster society",
        "Ecological symbiosis","Inverted gravity","Parallel history",
        "Shattered time","Dream logic","Reincarnation setting","Otherworld isekai"
    )
    private val outfits = listOf(
        "Heavy plate armor","Light armor","Lamellar armor","Flexible combat suit",
        "Bodysuit","Armored bodysuit","Ceremonial dress","Formal gown",
        "Battle dress","Royal regalia","Layered robe","Kimono","Hanfu",
        "Qipao","Sarong","Tunic","Corset ensemble","Uniform","Military uniform",
        "Officer dress","Streetwear","Techwear","Urban sportswear","Tactical gear",
        "Gothic lolita","Maid costume","Idol costume","Dancer costume",
        "Swimwear","Beachwear","Lingerie","Eveningwear","Winter outfit",
        "Desert outfit","Flight suit","Space suit","Exosuit","Magical vestments",
        "Divine raiment","Demonic regalia","Organic-grown clothing",
        "Living armor","Crystal garment","Liquid fabric","Transparent-layer garment",
        "Asymmetric wrap","Harness architecture","Cape and mantle","Cloak",
        "Utility outfit","Explorer outfit","Fantasy adventurer",
        "Mechanized dress","Futuristic ceremonial","Hybrid garment"
    )
    private val weapons = listOf(
        "Longsword","Greatsword","Katana","Rapier","Dagger","Twin blades",
        "Spear","Lance","Halberd","Glaive","Trident","Scythe",
        "Battle axe","Warhammer","Mace","Flail","Polearm",
        "Longbow","Recurve bow","Crossbow","Throwing blade",
        "Staff","Wand","Scepter","Cane sword","Shield weapon",
        "Chakram","Whip","Chain weapon","Clawed gauntlet",
        "Gunblade","Fantasy rifle","Fantasy pistol","Energy cannon",
        "Mechanical launcher","Transforming weapon","Segmented blade",
        "Organic weapon","Crystal weapon","Living weapon","Liquid weapon",
        "Magnetic weapon","Runic weapon","Anti-matter weapon",
        "Celestial weapon","Demonic weapon","Ceremonial weapon",
        "Folding architecture","Telescoping architecture","Articulated frame",
        "Asymmetric geometry","Symmetric geometry","Ring architecture",
        "Spiral architecture","Exposed mechanical skeleton",
        "Contained energy core","Modular assembly"
    )
    fun options(mode: CreationMode, canonical: List<SelectableConcept>):List<SelectableConcept> {
        val names=when(mode) {
            CreationMode.ALTERNATE_UNIVERSE -> au
            CreationMode.OUTFIT -> outfits
            CreationMode.WEAPON -> weapons
            else -> return canonical
        }
        val prefix=when(mode) {
            CreationMode.ALTERNATE_UNIVERSE -> "AU"
            CreationMode.OUTFIT -> "OF"
            else -> "WP"
        }
        return names.mapIndexed { index,name ->
            SelectableConcept(prefix+(index+1).toString().padStart(4,'0'),name)
        }
    }
}

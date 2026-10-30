package com.asterion.art.engine

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Random

/**
 * Namespaced, expandable creative taxonomies, separate from the canonical
 * Character Creation SC*** concept records. No existing IDs are reallocated.
 */
class CreativeTaxonomyRegistry(context:Context) {
    companion object {
        const val THEME_ATOMS="theme_atoms"
        const val EXTENDED_CONCEPTS="extended_concepts"
        const val MATERIAL_SYSTEMS="material_systems"
        const val ENVIRONMENTS="environments"
        const val PHYSICAL_MECHANISMS="physical_mechanisms"
        const val COMPOSITION_SYSTEMS="composition_systems"
        const val MOTION_SYSTEMS="motion_systems"
        const val INTERACTION_SYSTEMS="interaction_systems"
        private val mutex=Any()
        private val initialAtoms=linkedMapOf(
            "architectural" to listOf("vault","atrium","obelisk","viaduct","buttress","pavilion","arcade","colonnade","labyrinth","rotunda"),
            "natural" to listOf("geyser","canopy","lagoon","grotto","aurora","canyon","monsoon","tundra","delta","dune"),
            "material" to listOf("basalt","amber","mercury","marble","ceramic","crystal","silk","glass","obsidian","copper"),
            "mechanical" to listOf("turbine","piston","lattice","gyro","valve","engine","tether","hinge","conduit","gear"),
            "celestial" to listOf("nebula","comet","eclipse","orbit","pulsar","quasar","meteor","zenith","horizon","asteroid"),
            "biological" to listOf("fungus","coral","mycelium","antler","frond","spore","membrane","gill","shell","tendon"),
            "historical" to listOf("citadel","galleon","aqueduct","bastion","reliquary","sundial","bazaar","palisade","caravan","forge"),
            "surreal" to listOf("mirror","paradox","echo","ribbon","fracture","inversion","spiral","rift","shadow","prism"),
            "optical" to listOf("caustic","diffraction","refraction","glare","parallax","bokeh","halo","chromatic","reflection","silhouette"),
            "technical" to listOf("circuit","antenna","reactor","module","sensor","archive","array","transducer","diode","console"),
            "weather" to listOf("mist","hail","squall","blizzard","thunder","rainfall","frost","cyclone","cloud","lightning"),
            "terrain" to listOf("escarpment","archipelago","moraine","ravine","crater","plateau","isthmus","basin","fjord","mesa")
        )
        private fun validName(value:String)=
            value.isNotBlank() && !value.contains(Regex("\\s"))
        private fun selection(available:List<String>,rand:Random,n:Int):List<String>{
            val deck=available.toMutableList()
            for(i in deck.indices.reversed()){
                val j=rand.nextInt(i+1)
                val a=deck[j];deck[j]=deck[i];deck[i]=a
            }
            return deck.take(n)
        }
        /** Independent of user keywords and B/E/T: these are NOT parameters. */
        fun roulette(terms:List<String>,seed:Long):List<List<String>>{
            val unique=terms.map{it.trim()}.filter(::validName).distinct()
            require(unique.size>=30){"Theme atom taxonomy needs >=30 separate atoms"}
            val rand=Random(seed)
            val seen=mutableSetOf<String>()
            return (0 until 5).map {
                var result:List<String>
                var trials=0
                do {
                    val size=3+rand.nextInt(4)
                    result=selection(unique,rand,size)
                    trials++
                    require(trials<=10000){"Independent roulette cannot form five distinct atom sets"}
                } while(!seen.add(result.sorted().joinToString("|")))
                result
            }
        }
    }
    private val root=File(context.applicationContext.filesDir,"creative-taxonomies/v1")
        .also{it.mkdirs()}
    private fun file(space:String):File{
        require(space.matches(Regex("[a-z][a-z0-9_]{2,64}")))
        return File(root,"$space.json")
    }
    @Synchronized fun entries(space:String):Map<String,List<String>> =
        synchronized(mutex) {
            val file=file(space)
            if(!file.exists()){
                if(space!=THEME_ATOMS)return@synchronized emptyMap()
                return@synchronized initialAtoms
            }
            val json=JSONObject(file.readText())
            val groups=json.getJSONObject("categories")
            groups.keys().asSequence().associateWith{group->
                val a=groups.getJSONArray(group)
                (0 until a.length()).map{a.getString(it)}
            }
        }
    @Synchronized fun extend(space:String,category:String,words:List<String>){
        synchronized(mutex){
            require(category.isNotBlank()&&category.length<=100)
            require(words.isNotEmpty()&&words.all{validName(it)})
            val prior=entries(space)
            val updated=prior+(category to
                (prior[category].orEmpty()+words).distinct())
            val obj=JSONObject().apply{
                put("version",1);put("taxonomy",space)
                put("categories",JSONObject().apply{
                    updated.forEach{(key,list)->put(key,JSONArray(list))}
                })
            }
            GeneratedImagePublisher.atomicWrite(file(space),obj.toString(2).toByteArray())
        }
    }
    fun atoms():List<String>=entries(THEME_ATOMS).values.flatten().distinct()
    fun themeRoulette(seed:Long):List<List<String>> =roulette(atoms(),seed)
}

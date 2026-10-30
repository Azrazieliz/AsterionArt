package com.asterion.art.methodology

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A Theme is a versioned TEXT asset; no image or sheet is created here. */
data class ThemeText(
    val id:String,
    val name:String,
    val atoms:List<String>,
    val visualSystem:String,
    val revisedFrom:List<String> = emptyList()
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(atoms.isNotEmpty() && atoms.all {
            it.isNotBlank() && !it.trim().contains(Regex("\\s"))
        })
        require(visualSystem.isNotBlank())
    }
}

data class ThemeTextPlan(
    val id:String,
    val createdAt:Long,
    val badassness:Int?,
    val eroticity:Int?,
    val tension:Int?,
    val keywords:List<String>,
    val extraParameters:Map<String,String>,
    val characterConfiguration:String?,
    val liquidDirection:String?,
    val seed:Long,
    val status:String="DRAFT",
    val stage:String="",
    val frozenAtomSets:List<List<String>> = emptyList(),
    val themes:List<ThemeText> = emptyList(),
    val approvedThemeId:String? = null,
    val renderJobId:String? = null
) {
    val approved:ThemeText? get()=themes.firstOrNull{it.id==approvedThemeId}
    fun requireFive() {
        require(themes.size==5) { "Exactly five independent text themes are required" }
        require(themes.map{it.id}.distinct().size==5)
        require(themes.map{it.name.trim().lowercase()}.distinct().size==5)
        require(themes.map{it.visualSystem.trim().lowercase()}.distinct().size==5)
    }
    fun renderAuthority():String {
        val chosen=requireNotNull(approved){"Approve one text theme first"}
        return buildString {
            append("APPROVED THEME TEXT AS VISUAL-CONTENT AUTHORITY. ")
            append("Generate ONE reusable 9:16 professional Theme Master Sheet ")
            append("with one dominant canonical scene and analytical sections ")
            append("describing invariant, adaptable, optional and forbidden ")
            append("visual systems; scene hierarchy, geometry, composition, ")
            append("camera, figures as identity-neutral hosts, interactions, ")
            append("materials, lighting, color and reuse constraints. ")
            append("Do not invent canonical character face, anatomy or outfit. ")
            append("No unrelated themes or collage. ")
            append("THEME NAME: ").append(chosen.name)
            append("\\nFROZEN ATOMS: ").append(chosen.atoms.joinToString(", "))
            append("\\nFULL THEME VISUAL SYSTEM: ").append(chosen.visualSystem)
            append("\\nSCORE B/E/T: ")
            append(listOf(badassness,eroticity,tension).joinToString("/") {
                it?.toString() ?: "UNSPECIFIED"
            })
            append("\\nKEYWORD CONSTRAINTS: ").append(keywords.joinToString("; "))
            if(characterConfiguration!=null)
                append("\\nCHARACTER CONFIGURATION: ").append(characterConfiguration)
            if(liquidDirection!=null)
                append("\\nLIQUID: ").append(liquidDirection)
            extraParameters.forEach{(name,value)->
                append("\\nOWNER PARAMETER ").append(name).append(": ").append(value)
            }
        }
    }
}

/** Isolated text assets and exact user-approved variants, never prompt-only ghosts. */
class ThemeTextStore(context:Context) {
    private val directory=File(context.applicationContext.filesDir,"themes/text").also{it.mkdirs()}
    private fun file(id:String):File {
        require(id.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        return File(directory,id+".json")
    }
    @Synchronized fun create(
        b:Int?,e:Int?,t:Int?,
        keywords:List<String>,
        extra:Map<String,String>,
        characterConfiguration:String?,
        liquidDirection:String?,
        seed:Long
    ):ThemeTextPlan {
        listOfNotNull(b,e,t).forEach{require(it in 0..200)}
        require(extra.keys.all{it.isNotBlank()})
        return ThemeTextPlan(UUID.randomUUID().toString(),
            System.currentTimeMillis(),b,e,t,
            keywords.map{it.trim()}.filter{it.isNotBlank()},
            extra.mapKeys{it.key.trim()},characterConfiguration,liquidDirection,seed)
            .also(::persist)
    }
    @Synchronized fun persist(plan:ThemeTextPlan) {
        val data=JSONObject().apply {
            put("version",1)
            put("id",plan.id);put("created",plan.createdAt)
            put("b",plan.badassness?:JSONObject.NULL)
            put("e",plan.eroticity?:JSONObject.NULL)
            put("t",plan.tension?:JSONObject.NULL)
            put("keywords",JSONArray(plan.keywords))
            put("extra",JSONObject(plan.extraParameters))
            put("config",plan.characterConfiguration?:JSONObject.NULL)
            put("liquid",plan.liquidDirection?:JSONObject.NULL)
            put("seed",plan.seed);put("status",plan.status);put("stage",plan.stage)
            put("frozen_atoms",JSONArray().apply {
                plan.frozenAtomSets.forEach{put(JSONArray(it))}
            })
            put("themes",JSONArray().apply {
                plan.themes.forEach { theme ->
                    put(JSONObject().apply {
                        put("id",theme.id);put("name",theme.name)
                        put("atoms",JSONArray(theme.atoms))
                        put("visual",theme.visualSystem)
                        put("parents",JSONArray(theme.revisedFrom))
                    })
                }
            })
            put("approved",plan.approvedThemeId?:JSONObject.NULL)
            put("render_job",plan.renderJobId?:JSONObject.NULL)
        }
        GeneratedImagePublisher.atomicWrite(file(plan.id),data.toString().toByteArray())
    }
    @Synchronized fun load(id:String):ThemeTextPlan {
        val j=JSONObject(file(id).readText())
        require(j.getInt("version")==1)
        fun optional(key:String)=j.optString(key,"").takeIf{it.isNotBlank()&&it!="null"}
        fun strings(array:JSONArray)=(0 until array.length()).map{array.getString(it)}
        val extras=j.optJSONObject("extra")?:JSONObject()
        val items=j.getJSONArray("themes")
        return ThemeTextPlan(
            j.getString("id"),j.getLong("created"),
            if(j.isNull("b"))null else j.getInt("b"),
            if(j.isNull("e"))null else j.getInt("e"),
            if(j.isNull("t"))null else j.getInt("t"),
            strings(j.getJSONArray("keywords")),
            extras.keys().asSequence().associateWith{extras.getString(it)},
            optional("config"),optional("liquid"),j.getLong("seed"),
            j.getString("status"),j.optString("stage"),
            j.optJSONArray("frozen_atoms")?.let{outer->
                (0 until outer.length()).map{strings(outer.getJSONArray(it))}
            }.orEmpty(),
            (0 until items.length()).map{index->
                val c=items.getJSONObject(index)
                ThemeText(c.getString("id"),c.getString("name"),
                    strings(c.getJSONArray("atoms")),c.getString("visual"),
                    strings(c.optJSONArray("parents")?:JSONArray()))
            },optional("approved"),optional("render_job")
        )
    }
    @Synchronized fun recent():List<ThemeTextPlan> = directory.listFiles()
        .orEmpty().filter{it.extension=="json"}
        .mapNotNull{runCatching{load(it.nameWithoutExtension)}.getOrNull()}
        .sortedByDescending{it.createdAt}

    @Synchronized fun begin(id:String) {
        val p=load(id)
        require(p.status in setOf("DRAFT","FAILED")) {
            "Theme generation already in progress or completed"
        }
        persist(p.copy(status="GENERATING",stage="Selecting five independent atomic sets"))
    }
    @Synchronized fun stage(id:String,message:String) {
        val p=load(id)
        if(p.status=="GENERATING")persist(p.copy(stage=message))
    }
    @Synchronized fun freezeAtoms(id:String,sets:List<List<String>>) {
        val p=load(id)
        require(p.status=="GENERATING" && p.frozenAtomSets.isEmpty())
        require(sets.size==5 && sets.map{it.joinToString("|")}.distinct().size==5)
        require(sets.all{set->set.size in 3..6 && set.all{atom->
            atom.isNotBlank() && !atom.contains(Regex("\\s"))
        }})
        persist(p.copy(frozenAtomSets=sets,
            stage="Frozen five independent roulette results"))
    }
    @Synchronized fun finish(id:String,proposals:List<ThemeText>) {
        val p=load(id)
        require(p.status=="GENERATING")
        require(p.frozenAtomSets.size==5)
        require(proposals.size==5 && proposals.indices.all{
            proposals[it].atoms==p.frozenAtomSets[it]
        })
        persist(p.copy(status="READY",themes=proposals,
            approvedThemeId=null,stage="Five distinct text themes ready"))
    }
    @Synchronized fun fail(id:String,message:String) {
        val p=load(id)
        if(p.status=="GENERATING")persist(p.copy(status="FAILED",stage=message))
    }
    @Synchronized fun revise(id:String,theme:ThemeText) {
        val p=load(id)
        require(p.status in setOf("READY","APPROVED"))
        require(p.renderJobId==null) {
            "This revision has already been sent to rendering; create another text plan"
        }
        require(p.themes.any{it.id==theme.id})
        val changed=p.themes.map{if(it.id==theme.id)theme else it}
        val revised=p.copy(status="READY",themes=changed,
            approvedThemeId=null,stage="Theme text modified; approval required")
        revised.requireFive()
        persist(revised)
    }
    @Synchronized fun approve(id:String,themeId:String) {
        val p=load(id)
        p.requireFive()
        require(p.themes.any{it.id==themeId})
        require(p.renderJobId==null)
        persist(p.copy(status="APPROVED",approvedThemeId=themeId,
            stage="Text theme approved; ready for optional Theme Sheet bridge"))
    }
    @Synchronized fun recordRender(id:String,jobId:String) {
        val p=load(id)
        require(p.status=="APPROVED" && p.approved!=null)
        require(p.renderJobId==null)
        persist(p.copy(renderJobId=jobId,stage="Theme Sheet image generation submitted"))
    }
}

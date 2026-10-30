package com.asterion.art.methodology

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import com.asterion.art.model.SheetRole
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Route B is an executable, mode-specific design contract. The output-image
 * group is authoritative and contains all independent entries. These fields
 * control generation and are not decorative explanatory text.
 */
data class ModeTextField(val key:String,val title:String)
data class ModeTextSchema(
    val key:String,
    val title:String,
    val fields:List<ModeTextField>,
    val designsPerImage:Int,
    val imageRatio:String,
    val directive:String,
    val requiredRoles:Set<SheetRole> = emptySet()
) {
    fun validate(designs:List<ModeTextDesign>) {
        require(designs.size==designsPerImage) {
            "One "+title+" image requires "+designsPerImage+" complete designs"
        }
        require(designs.map{it.ordinal}.toSet()==(1..designsPerImage).toSet())
        require(designs.map{it.name.trim().lowercase()}.distinct().size==designsPerImage)
        for(design in designs) {
            require(design.name.isNotBlank())
            require(design.fields.keys.containsAll(fields.map{it.key}))
            fields.forEach { field ->
                require(!design.fields[field.key].isNullOrBlank()) {
                    "Design "+design.ordinal+" missing "+field.title
                }
            }
        }
    }
}
data class ModeTextDesign(
    val ordinal:Int,
    val name:String,
    val fields:Map<String,String>
)

/** Field vocabulary and output layout are unique to the selected mode. */
object ModeTextSchemas {
    private fun fields(vararg data:Pair<String,String>)=
        data.map{ModeTextField(it.first,it.second)}
    fun forMode(mode:CreationMode?):ModeTextSchema {
        if(mode==null) return ModeTextSchema("ARTWORK","Artwork",
            fields(
                "identity" to "Character identity and preserved details",
                "theme" to "Theme composition and physical interactions",
                "style" to "Art-style rendering grammar",
                "scene" to "Pose, viewpoint, scene and camera",
                "materials" to "Materials, lighting and effects",
                "details" to "Complete composition-level render specification"
            ),1,"CONFIGURED",
            "Artwork only. Character, Theme and Art Style Sheets are required; "+
                "Element Sheet is optional. Preserve the existing five Artwork "+
                "quality phases, and never apply them to any Original.",
            setOf(SheetRole.CHARACTER,SheetRole.THEME,SheetRole.ART_STYLE))
        val original=DetailedMethodologies.all.getValue(mode)
        val layouts=original.catalogue
        val parts=when(mode) {
            CreationMode.OUTFIT -> fields(
                "identity" to "Frozen source body, identity and fit",
                "silhouette" to "Independent garment silhouette and construction",
                "coverage" to "Coverage and clothing architecture",
                "materials" to "Textile, layers, seams and material response",
                "detail" to "Distinct accessories and construction details",
                "render" to "Complete outfit design for its catalogue panel")
            CreationMode.WEAPON -> fields(
                "category" to "Weapon category",
                "geometry" to "Complete isolated functional geometry",
                "grip" to "Grip, balance, assembly and mechanical joins",
                "materials" to "Materials and fabrication",
                "identity" to "Affinity with preserved source character",
                "render" to "Independent complete weapon panel specification")
            CreationMode.TRANSFORMATION -> fields(
                "identity" to "Unchanged original identity",
                "state" to "Alternate body state",
                "anatomy" to "Structural transformation and appendage logic",
                "materials" to "Material state and change",
                "outfit" to "Outfit adaptation where applicable",
                "render" to "Complete coherent alternate form")
            CreationMode.MUTATION -> fields(
                "identity" to "Preserved source recognition",
                "mutation" to "Structural mutation mechanism",
                "anatomy" to "Changed body plan and asymmetry",
                "materials" to "Mutated materials, surfaces and attachments",
                "difference" to "Distinction from all other mutants",
                "render" to "Complete mutation catalogue panel")
            CreationMode.CREATURE -> fields(
                "body" to "Distinct complete coherent body plan",
                "locomotion" to "Locomotion and load paths",
                "senses" to "Sensory anatomy and physiology",
                "fusion" to "Structural concept symbiosis",
                "materials" to "Surface, material and palette architecture",
                "render" to "Complete independently authored creature")
            CreationMode.ALTERNATE_UNIVERSE -> fields(
                "identity" to "Frozen source-character identity",
                "universe" to "Distinct alternate-world operating rules",
                "body" to "Body identity and morphology continuity",
                "clothing" to "World-native outfit and equipment",
                "materials" to "Technology and material adaptation",
                "render" to "Complete alternate-universe character")
            CreationMode.FEMINISATION -> fields(
                "identity" to "Source recognizable non-gender identity anchors",
                "body" to "Independently reconstructed female anatomy",
                "face" to "Rebuilt female face, eyes and hair",
                "outfit" to "Reconstructed clothing fitted to the new body",
                "materials" to "Preserved source features and materials",
                "render" to "Complete independent feminine character")
            CreationMode.STYLE_CORRECTION -> fields(
                "content" to "Source identity, pose, framing and scene lock",
                "style" to "Target Art Style Sheet grammar",
                "line" to "Complete linework and edge treatment",
                "shading" to "Shading and light treatment",
                "materials" to "Source-material rendering in the target style",
                "render" to "One complete source-faithful style correction")
            CreationMode.CHARACTER -> error(
                "Character Creation has a separate five-identity Phase-1 design board")
        }
        return ModeTextSchema(mode.name,original.name,
            parts,layouts.designsPerImage,layouts.ratio,
            original.rendererDirective,original.requiredSheets)
    }
}

data class ModeTextPlan(
    val id:String,
    val created:Long,
    val mode:CreationMode?,
    val renderMode:RenderMode,
    val sheetIds:List<String>,
    val sourceImageUri:String?,
    val concepts:List<String>,
    val userDirection:String,
    val exclusions:String,
    val status:String="DRAFT",
    val stage:String="",
    val designs:List<ModeTextDesign> = emptyList(),
    val renderJobId:String?=null
) {
    val schema get()=ModeTextSchemas.forMode(mode)
    fun rendererAuthority():String {
        schema.validate(designs)
        return buildString{
            append("EXECUTABLE MODE: ").append(schema.key).append("\n")
            append("FULL SPECIFIC METHODOLOGY: ").append(schema.directive)
                .append("\n")
            append("ONE COMPLETE ").append(schema.imageRatio)
                .append(" IMAGE WITH EXACTLY ")
                .append(schema.designsPerImage)
                .append(" INDEPENDENT DESIGNS.\n")
            append("USER DIRECTION: ").append(userDirection).append("\n")
            append("BINDING SOURCE CONCEPTS: ").append(concepts.joinToString(" × "))
                .append("\n")
            append("SOURCE IMAGE: ").append(sourceImageUri?:"none").append("\n")
            append("SOURCE SHEET IDS: ").append(sheetIds.joinToString(", "))
                .append("\n")
            designs.forEach{design->
                append("DESIGN ").append(design.ordinal)
                    .append(" · ").append(design.name).append("\n")
                schema.fields.forEach{field->
                    append(field.title).append(": ")
                        .append(design.fields.getValue(field.key)).append("\n")
                }
            }
            append("Preserve each independent design's information and all ")
            append("selected source-reference pixels during native conditioning.")
        }
    }
}

class ModeTextPlanStore(context:Context) {
    private val directory=File(context.applicationContext.filesDir,
        "methodology-text/plans").also{it.mkdirs()}
    private fun file(id:String):File{
        require(id.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        return File(directory,id+".json")
    }
    @Synchronized fun create(mode:CreationMode?,renderMode:RenderMode,
        sheets:List<String>,sourceUri:String?,concepts:List<String>,
        direction:String,exclusions:String):ModeTextPlan {
        val plan=ModeTextPlan(UUID.randomUUID().toString(),
            System.currentTimeMillis(),mode,renderMode,sheets.distinct(),
            sourceUri,concepts,direction,exclusions)
        persist(plan)
        return plan
    }
    @Synchronized fun persist(plan:ModeTextPlan){
        if(plan.status in setOf("READY","AUTHORIZED"))
            plan.schema.validate(plan.designs)
        val j=JSONObject().apply{
            put("version",1);put("id",plan.id);put("created",plan.created)
            put("mode",plan.mode?.name?:JSONObject.NULL)
            put("render",plan.renderMode.name)
            put("sheets",JSONArray(plan.sheetIds))
            put("source",plan.sourceImageUri?:JSONObject.NULL)
            put("concepts",JSONArray(plan.concepts))
            put("direction",plan.userDirection);put("exclusions",plan.exclusions)
            put("status",plan.status);put("stage",plan.stage)
            put("render_job",plan.renderJobId?:JSONObject.NULL)
            put("designs",JSONArray().apply{
                plan.designs.forEach{d->
                    put(JSONObject().put("ordinal",d.ordinal)
                        .put("name",d.name).put("fields",JSONObject(d.fields)))
                }
            })
        }
        GeneratedImagePublisher.atomicWrite(file(plan.id),j.toString().toByteArray())
    }
    @Synchronized fun load(id:String):ModeTextPlan{
        val j=JSONObject(file(id).readText())
        require(j.getInt("version")==1)
        fun str(key:String)=j.optString(key).takeIf{it.isNotBlank()&&it!="null"}
        fun arr(key:String)=j.getJSONArray(key).let{a->
            (0 until a.length()).map{a.getString(it)}
        }
        val records=j.getJSONArray("designs")
        return ModeTextPlan(j.getString("id"),j.getLong("created"),
            str("mode")?.let(CreationMode::valueOf),
            RenderMode.valueOf(j.getString("render")),
            arr("sheets"),str("source"),arr("concepts"),
            j.getString("direction"),j.getString("exclusions"),
            j.getString("status"),j.optString("stage"),
            (0 until records.length()).map{idx->
                val d=records.getJSONObject(idx)
                val f=d.getJSONObject("fields")
                ModeTextDesign(d.getInt("ordinal"),d.getString("name"),
                    f.keys().asSequence().associateWith{f.getString(it)})
            },str("render_job"))
    }
    @Synchronized fun recent()=directory.listFiles().orEmpty()
        .filter{it.extension=="json"}
        .mapNotNull{runCatching{load(it.nameWithoutExtension)}.getOrNull()}
        .sortedByDescending{it.created}
    @Synchronized fun begin(id:String){
        val p=load(id)
        require(p.status in setOf("DRAFT","FAILED","GENERATING"))
        persist(p.copy(status="GENERATING",stage="Writing mode-specific text designs"))
    }
    @Synchronized fun finish(id:String,designs:List<ModeTextDesign>){
        val p=load(id)
        require(p.status=="GENERATING")
        p.schema.validate(designs)
        persist(p.copy(status="READY",designs=designs,
            stage="Complete design records ready for owner review"))
    }
    @Synchronized fun fail(id:String,reason:String){
        val p=load(id)
        if(p.status=="GENERATING")persist(p.copy(status="FAILED",stage=reason))
    }
    @Synchronized fun revise(id:String,design:ModeTextDesign){
        val p=load(id)
        require(p.status=="READY"&&p.renderJobId==null)
        require(p.designs.any{it.ordinal==design.ordinal})
        val updated=p.designs.map{if(it.ordinal==design.ordinal)design else it}
        p.schema.validate(updated)
        // Original unedited design population is retained independently.
        val backup=File(directory,id+"-original.json")
        if(!backup.exists())
            GeneratedImagePublisher.atomicWrite(backup,file(id).readBytes())
        persist(p.copy(designs=updated,stage="Design "+design.ordinal+" revised"))
    }
    @Synchronized fun authorize(id:String,jobId:String){
        val p=load(id)
        require(p.status=="READY"&&p.renderJobId==null)
        p.schema.validate(p.designs)
        persist(p.copy(status="AUTHORIZED",renderJobId=jobId))
    }
}

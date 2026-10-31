package com.asterion.art.nativeengine

import android.content.Context
import com.asterion.art.engine.DreamModel
import com.asterion.art.model.RenderMode

/** Explicit user tags override filename guesses; tags persist across app upgrades. */
class ModelOutputRouting(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "asterion_model_output_tags_v1", Context.MODE_PRIVATE)

    fun tags(id: String): Set<RenderMode> {
        val stored=prefs.getStringSet(id, null) ?: return emptySet()
        return stored.mapNotNull { runCatching { RenderMode.valueOf(it) }.getOrNull() }.toSet()
    }

    fun set(id: String, mode: RenderMode, enabled: Boolean) {
        require(id.isNotBlank())
        val current=tags(id).toMutableSet()
        if (enabled) current+=mode else current-=mode
        check(prefs.edit().putStringSet(id,current.map { it.name }.toSet()).commit())
    }


    /** Route a trained, converted per-mode QNN checkpoint when installed.
     * This only selects actual weights; binding an arbitrary checkpoint
     * does not train or condition that model.
     */
    fun forMode(modeKey:String,models:List<DreamModel>):DreamModel? {
        require(modeKey.matches(Regex("[A-Z_]{3,48}")))
        val id=prefs.getString("native_mode_"+modeKey,null)?:return null
        return models.firstOrNull { it.id==id }
    }
    fun setMode(modeKey:String,modelId:String?) {
        require(modeKey.matches(Regex("[A-Z_]{3,48}")))
        require(modelId==null || modelId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        val edit=prefs.edit()
        if(modelId==null)edit.remove("native_mode_"+modeKey)
        else edit.putString("native_mode_"+modeKey,modelId)
        check(edit.commit())
    }

    fun preferred(mode: RenderMode, models: List<DreamModel>): DreamModel? {
        if(models.isEmpty()) return null
        val candidates=models.filter { mode in tags(it.id) }
        val pool=if(candidates.isNotEmpty())candidates else models.filter {
            when(mode) {
                RenderMode.ANIME -> it.isAnima || it.isSdxl
                RenderMode.REALISTIC,RenderMode.COSPLAY -> it.isSdxl || it.isAnima
                RenderMode.ANIMATION,RenderMode.OTHER -> false
            }
        }
        val ranked=when(mode) {
            RenderMode.ANIME -> listOf("rin", "miaomiao", "hassaku", "noobai")
            RenderMode.REALISTIC,RenderMode.COSPLAY ->
                listOf("realistic","realism","cyberrealistic","cosplay")
            RenderMode.ANIMATION,RenderMode.OTHER -> emptyList()
        }
        return pool.minByOrNull { item ->
            val label=(item.name+" "+item.id).lowercase()
            ranked.indexOfFirst { it in label }.let { if(it<0)100 else it }
        }
    }
}

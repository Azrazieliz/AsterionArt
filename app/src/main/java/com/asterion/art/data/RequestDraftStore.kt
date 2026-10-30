package com.asterion.art.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable, private request state. A DRAFT is never submitted for inference by
 * saving it. Only an explicit user Start creates a GenerationJob/VideoPlan.
 *
 * Persisted in the existing application data; app upgrades retain it.
 * No model directories, generated files, signing keys or cached inference are
 * deleted when a draft is saved.
 */
class RequestDraftStore(context: Context) {
    private val prefs=context.applicationContext.getSharedPreferences(
        "asterion_request_drafts_v1",Context.MODE_PRIVATE)

    @Synchronized fun load(key:String):JSONObject {
        checkKey(key)
        return runCatching {
            val saved=prefs.getString(key,null)
            if(saved.isNullOrBlank())JSONObject() else JSONObject(saved)
        }.getOrDefault(JSONObject())
    }

    @Synchronized fun save(key:String,document:JSONObject) {
        checkKey(key)
        val data=JSONObject(document.toString()).apply {
            put("draft_schema",1)
        }.toString()
        if(prefs.getString(key,null)==data)return
        check(prefs.edit().putString(key,data).commit()) {
            "The unfinished request could not be saved to durable storage"
        }
    }

    /** Clear only by an explicit user action; launching a job never clears it. */
    @Synchronized fun clear(key:String) {
        checkKey(key)
        check(prefs.edit().remove(key).commit())
    }

    private fun checkKey(key:String) {
        require(key.matches(Regex("[a-z0-9_-]{3,70}"))) {
            "Invalid request draft key"
        }
    }

    companion object {
        fun strings(items:Iterable<String>):JSONArray =
            JSONArray().apply { items.forEach { put(it) } }

        fun readStrings(root:JSONObject,key:String):List<String> =
            root.optJSONArray(key)?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            }.orEmpty()

        fun readRoleSources(root:JSONObject,key:String):
            Map<String,List<String>> {
            val data=root.optJSONObject(key)?:return emptyMap()
            return data.keys().asSequence().associateWith { role ->
                readStrings(data,role)
            }
        }

        fun roleSources(values:Map<String,List<String>>):JSONObject =
            JSONObject().apply {
                values.forEach { (role,uris) ->put(role,strings(uris)) }
            }
    }
}

package com.asterion.art.work

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class QueuedNativeRender(val jobId:String,val taskId:String)

/**
 * Durable FIFO for native image renders executed in the foreground service.
 * Foreground-service intent redelivery is insufficient when two jobs are
 * queued: every request must survive Activity teardown and service restart.
 */
class ForegroundRenderQueueStore(context:Context) {
    companion object { private val LOCK=Any() }
    private val prefs=context.applicationContext.getSharedPreferences(
        "asterion_native_render_queue_v1",Context.MODE_PRIVATE)

    private fun read():List<QueuedNativeRender> {
        val array=runCatching { JSONArray(prefs.getString("pending","[]")) }
            .getOrElse { throw IllegalStateException(
                "Pending rendering requests cannot be recovered",it) }
        return (0 until array.length()).map { index ->
            val item=array.getJSONObject(index)
            QueuedNativeRender(item.getString("job"),item.getString("task"))
        }
    }

    private fun write(items:List<QueuedNativeRender>) {
        val array=JSONArray().apply {
            items.forEach { put(JSONObject().put("job",it.jobId).put("task",it.taskId)) }
        }
        check(prefs.edit().putString("pending",array.toString()).commit()) {
            "Could not save the native render queue"
        }
    }

    fun enqueue(jobId:String,taskId:String) = synchronized(LOCK) {
        require(jobId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        require(taskId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        val current=read()
        if(current.none{it.jobId==jobId && it.taskId==taskId})
            write(current+QueuedNativeRender(jobId,taskId))
    }

    fun next():QueuedNativeRender?=synchronized(LOCK){read().firstOrNull()}

    fun remove(jobId:String):Unit=synchronized(LOCK) {
        val items=read()
        val updated=items.filterNot { it.jobId==jobId }
        if(updated.size!=items.size)write(updated)
    }

    fun pending():List<QueuedNativeRender>=synchronized(LOCK){read()}
}

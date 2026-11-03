package com.asterion.art.integrations

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/** Optional source-asset retrieval by Asterion Downloader.
 * The Downloader validates OS caller UID/certificate; the user approves Art once
 * in Downloader Settings because the two products have independent signing keys.
 * No account cookies or private credentials ever leave either application's vault.
 */
object AsterionDownloaderBridge {
    private const val ACTION="com.azrael.pixivdumpsync.action.DOWNLOAD_REQUEST"
    private const val RESULT="com.azrael.pixivdumpsync.action.DOWNLOAD_RESULT"
    private const val PKG_PREVIEW="com.azrael.pixivdumpsync.nocturne"
    private const val PKG_PRODUCTION="com.azrael.pixivdumpsync"
    private const val RECEIVER="com.azrael.pixivdumpsync.AsterionDownloadRequestReceiver"
    private const val PREF="asterion_art_downloader_bridge"
    private fun target(c:Context):String?{
        val pm=c.packageManager
        for(pkg in listOf(PKG_PREVIEW,PKG_PRODUCTION)){
            if(runCatching{
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg,0)
            }.isSuccess)return pkg
        }
        return null
    }
    fun requestAsset(c:Context,url:String,label:String,category:String="ASSETS"):Boolean{
        if(!url.startsWith("https://") || !android.webkit.URLUtil.isHttpsUrl(url) ||
            category !in setOf("ASSETS","ARTWORK","VIDEOS","BOOKS",
                "COMICS","GAMES","KOIKATSU"))return false
        val pkg=target(c)?:return false
        return runCatching{
            c.sendBroadcast(Intent(ACTION).apply{
                component=ComponentName(pkg,RECEIVER)
                putExtra("url",url)
                putExtra("category",category)
                putExtra("name",label.take(140))
            })
            c.getSharedPreferences(PREF,0).edit()
                .putString("last_request_name",label.take(140))
                .putString("last_state","SUBMITTED").apply()
            true
        }.getOrDefault(false)
    }
    fun status(c:Context):String=c.getSharedPreferences(PREF,0)
        .getString("last_state","NO_REQUEST").orEmpty()
    internal fun record(c:Context,id:String,state:String){
        c.getSharedPreferences(PREF,0).edit()
            .putString("last_request_id",id)
            .putString("last_state",state.take(40)).apply()
    }
}
class AsterionDownloaderResultReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        if(intent.action!="com.azrael.pixivdumpsync.action.DOWNLOAD_RESULT")return
        // A status broadcast carries no URL or credentials. Ignore spoofed senders.
        val uid=if(Build.VERSION.SDK_INT>=34)sentFromUid else -1
        val packages=context.packageManager.getPackagesForUid(uid).orEmpty()
        if(packages.none{it=="com.azrael.pixivdumpsync.nocturne" ||
                        it=="com.azrael.pixivdumpsync"})return
        val id=intent.getStringExtra("request_id").orEmpty()
        val state=intent.getStringExtra("state").orEmpty()
        if(id.matches(Regex("[0-9a-f]{64}")) &&
            state in setOf("QUEUED","COMPLETED","NEEDS_AUTH","ERROR"))
            AsterionDownloaderBridge.record(context,id,state)
    }
}

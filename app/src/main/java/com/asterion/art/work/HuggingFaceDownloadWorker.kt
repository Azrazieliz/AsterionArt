package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.work.*
import com.asterion.art.brain.BrainCatalog
import com.asterion.art.brain.BrainModelStore
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskStatus
import com.asterion.art.models.HubPath
import com.asterion.art.nativeengine.NativeModelLibrary
import com.asterion.art.nativeengine.OriginalCheckpointLibrary
import com.asterion.art.vision.VisionCatalog
import com.asterion.art.vision.VisionModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CancellationException

/**
 * User-selected file from a commit-pinned Hugging Face repository.
 * Model weights never enter the APK. Runtime support is checked independently.
 */
class HuggingFaceDownloadWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        private const val CHANNEL="asterion-hugging-face"
        fun enqueue(context:Context,repoId:String,revision:String,path:String,
            bytes:Long,sha256:String?,family:String="auto"):String {
            HubPath.repository(repoId);HubPath.revision(revision);HubPath.file(path)
            val repo=ArtRepository(context)
            val task=repo.recordTask("hf:"+repoId+"/"+path,TaskKind.MODEL_IMPORT)
            val input=Data.Builder()
                .putString("repo",repoId).putString("revision",revision)
                .putString("path",path).putLong("bytes",bytes)
                .putString("sha",sha256).putString("family",family)
                .putString("task",task.id).build()
            val request=OneTimeWorkRequestBuilder<HuggingFaceDownloadWorker>()
                .setInputData(input).addTag("asterion-hf-model")
                .addTag("asterion-model-import-"+task.id).build()
            WorkManager.getInstance(context).enqueue(request)
            return task.id
        }
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO){
        val task=inputData.getString("task")?:return@withContext Result.failure()
        val repo=ArtRepository(applicationContext)
        try {
            repo.updateTask(task,TaskStatus.RUNNING)
            val id=HubPath.repository(inputData.getString("repo").orEmpty())
            val rev=HubPath.revision(inputData.getString("revision").orEmpty())
            val path=HubPath.file(inputData.getString("path").orEmpty())
            val expected=inputData.getString("sha")
                ?.takeIf{it.matches(Regex("[a-fA-F0-9]{64}"))}
            val fileName=path.substringAfterLast('/')
            val size=inputData.getLong("bytes",0L)
            val brain=BrainCatalog.models.firstOrNull {
                it.repository==id&&it.filename==fileName&&
                    (expected==null||it.sha256.equals(expected,true))
            }
            val vision=VisionCatalog.all.firstOrNull {
                id=="Qwen/Qwen3-VL-2B-Instruct-GGUF"&&it.name==fileName&&
                    (expected==null||it.sha256.equals(expected,true))
            }
            val app=applicationContext
            // Choose an app-owned volume with enough space before downloading
            // multi-gigabyte original checkpoints; never duplicate existing weights.
            val candidateRoots=(app.getExternalFilesDirs(null).filterNotNull()
                .map{File(it,"hf-library")}+
                File(app.filesDir,"hf-library"))
            val required=if(size>0)size+128L*1024*1024 else 512L*1024*1024
            val root=candidateRoots.firstOrNull {
                (it.parentFile?.usableSpace?:0L)>required
            }?:error("No app-owned volume has enough space for this model")
            check(root.mkdirs()||root.isDirectory)
            val folder=File(root,rev+"/"+id).also{
                check(it.mkdirs()||it.isDirectory)
            }
            // Only the base filename is stored; repository/version context
            // is retained in the manifest. Never accept repository traversal.
            val target=when {
                brain!=null->BrainModelStore(applicationContext).target(brain)
                vision!=null->VisionModelStore(applicationContext).target(vision)
                else->File(folder,UUID.nameUUIDFromBytes(
                    path.toByteArray()).toString()+"-"+fileName)
            }
            val partial=File(target.path+".partial")
            check(target.parentFile?.mkdirs()==true||target.parentFile?.isDirectory==true)
            val manager=applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL,
                "Hugging Face model downloads",NotificationManager.IMPORTANCE_LOW))
            fun stage(message:String,received:Long=0L) {
                val progress=if(size>0) (received*100/size).toInt().coerceIn(0,99) else 0
                repo.updateTaskProgress(task,progress,if(size>0)100 else 0,
                    message,received,size)
                val notification=Notification.Builder(applicationContext,CHANNEL)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(fileName.take(60)).setContentText(message)
                    .setOngoing(true).build()
                manager.notify(7246,notification)
            }
            setForeground(ForegroundInfo(7246,Notification.Builder(applicationContext,CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading model from Hugging Face")
                .setContentText(fileName.take(60)).build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            var previous=if(partial.isFile)partial.length() else 0L
            if(size>0&&previous>size){partial.delete();previous=0L}
            val url=HubPath.downloadUrl(id,rev,path)
            val conn=URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout=30_000;conn.readTimeout=120_000
            conn.instanceFollowRedirects=true
            conn.setRequestProperty("Accept-Encoding","identity")
            if(previous>0)conn.setRequestProperty("Range","bytes="+previous+"-")
            try {
                val status=conn.responseCode
                require(status==200||status==206){
                    "Hugging Face HTTP "+status+
                    if(status in listOf(401,403))": access restricted; use an accessible model" else ""
                }
                if(status==206)require(conn.getHeaderField("Content-Range")
                    .orEmpty().startsWith("bytes "+previous+"-")){
                    "Server returned an inconsistent resumed download"
                }
                val append=status==206&&previous>0L
                var received=if(append)previous else 0L
                val checkedSize=when{
                    size>0->size
                    conn.contentLengthLong>0->received+conn.contentLengthLong
                    else->0L
                }
                require(checkedSize==0L||
                    target.parentFile!!.usableSpace>checkedSize-received+64L*1024*1024){
                    "Insufficient storage for selected Hugging Face model file"
                }
                FileOutputStream(partial,append).use{output->
                    conn.inputStream.use{input->
                        val buffer=ByteArray(256*1024)
                        var last=0L
                        while(true) {
                            if(isStopped)throw CancellationException()
                            val n=input.read(buffer)
                            if(n<0)break
                            if(n==0)continue
                            received+=n
                            require(size<=0||received<=size){"File exceeds Hub metadata"}
                            output.write(buffer,0,n)
                            if(received-last>=4L*1024*1024){
                                stage("Downloading "+(received/1048576L)+" MiB",received)
                                last=received
                            }
                        }
                        output.fd.sync()
                    }
                }
                require(size<=0L||partial.length()==size){
                    "Incomplete Hugging Face model download"
                }
            }finally{conn.disconnect()}
            stage("Verifying selected model",partial.length())
            val digest=MessageDigest.getInstance("SHA-256")
            partial.inputStream().use{input->
                val buf=ByteArray(256*1024)
                while(true){
                    if(isStopped)throw CancellationException()
                    val n=input.read(buf);if(n<0)break
                    digest.update(buf,0,n)
                }
            }
            val actual=digest.digest().joinToString(""){"%02x".format(it)}
            require(expected==null||expected.equals(actual,true)){
                partial.delete()
                "Hugging Face LFS SHA-256 mismatch"
            }
            val message=when{
                brain!=null -> {
                    BrainModelStore(applicationContext).verifyAndCommit(brain,partial)
                    "Installed verified local reasoning GGUF"
                }
                vision!=null -> {
                    VisionModelStore(applicationContext).verifyAndCommit(vision,partial)
                    "Installed verified multimodal GGUF component"
                }
                fileName.lowercase().endsWith(".zip")-> {
                    stage("Installing downloaded QNN archive",partial.length())
                    NativeModelLibrary(applicationContext).importZip(
                        Uri.fromFile(partial),fileName.removeSuffix(".zip"),
                        progress={c,u->stage("Unpacking "+(u/1048576)+" MiB",c)},
                        stopped={isStopped},family=inputData.getString("family"))
                    partial.delete()
                    "Installed validated QNN image model"
                }
                else -> {
                    check(partial.renameTo(target)){
                        "Could not finalize downloaded model file"
                    }
                    OriginalCheckpointLibrary(applicationContext).registerDownloaded(
                        target,fileName)
                    val meta=File(target.path+".json")
                    GeneratedImagePublisher.atomicWrite(meta,JSONObject().apply {
                        put("schema",1);put("repo",id);put("revision",rev)
                        put("file",path);put("size",target.length())
                        put("sha256",actual);put("runnable",false)
                    }.toString().toByteArray())
                    val indexed=OriginalCheckpointLibrary(applicationContext)
                        .registerDownloaded(target,id+"/"+path)
                    if(indexed!=null)
                        "Indexed downloaded "+indexed.format+
                            " in Model Library; conversion/compatible runtime still required"
                    else
                        "Saved supporting model file; a compatible runtime is required"
                }
            }
            repo.updateTaskProgress(task,100,100,message,size,size)
            repo.updateTask(task,TaskStatus.COMPLETED)
            Result.success()
        }catch(e:CancellationException){
            repo.updateTask(task,TaskStatus.CANCELLED,"Download paused/cancelled; partial bytes retained")
            Result.failure()
        }catch(t:Throwable){
            repo.updateTask(task,TaskStatus.FAILED,t.message?:"Model download failed")
            Result.failure()
        }
    }
}

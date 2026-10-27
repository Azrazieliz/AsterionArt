package com.asterion.art.testing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.asterion.art.brain.BrainModelStore
import com.asterion.art.core.CoreSheetBridge
import com.asterion.art.model.SheetLayouts
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetRole
import com.asterion.art.nativeengine.EmbeddedNativeRunner
import com.asterion.art.nativeengine.NativeModelLibrary
import com.asterion.art.render.SheetRenderer
import com.asterion.art.vision.VisionModelStore
import java.io.File
import java.util.UUID

/**
 * Actual locally observable readiness. "Found" is not a successful model render.
 * No diagnostic sends prompts or user images to the internet.
 */
data class RuntimeProbe(val component:String,val state:String,val detail:String)
object RuntimeProbes {
    fun snapshot(context:Context):List<RuntimeProbe> {
        val app=context.applicationContext
        val native=File(app.applicationInfo.nativeLibraryDir)
        fun library(name:String)=File(native,"lib"+name+".so").isFile
        fun result(name:String,action:()->RuntimeProbe)=try {action()}
            catch(t:Exception){ RuntimeProbe(name,"FAILED",t.message?:"Unexpected failure") }
        return listOf(
            result("Native image engine") {
                val binary=EmbeddedNativeRunner.isEmbedded(app)
                val runtime=app.assets.list("qnnlibs").orEmpty()
                RuntimeProbe("Native image engine",
                    if(binary&&"libQnnHtp.so" in runtime)"PRESENT" else "MISSING",
                    "Embedded inference executable: "+binary+
                        "; Qualcomm QNN libraries: "+("libQnnHtp.so" in runtime)+
                        ". Model execution requires a separate actual smoke test.")
            },
            result("Image model installation") {
                val models=NativeModelLibrary(app).list()
                RuntimeProbe("Image model installation",
                    if(models.isNotEmpty())"PRESENT" else "NEEDS MODEL",
                    if(models.isEmpty())"Install a compatible converted QNN model in Models."
                    else models.joinToString("; ") { it.label+" ["+it.backend+"]" }+
                        ". Presence does not establish native compatibility.")
            },
            result("Local reasoning") {
                val brain=BrainModelStore(app)
                val selected=brain.selected()
                RuntimeProbe("Local reasoning",
                    if(selected!=null&&library("ai-chat"))"PRESENT" else "NEEDS MODEL",
                    "Qwen3 weights selected: "+(selected?.label?:"none")+
                        "; native reasoning library: "+library("ai-chat"))
            },
            result("Visual intelligence") {
                val pair=VisionModelStore(app).installed()
                RuntimeProbe("Visual intelligence",
                    if(pair!=null&&library("asterion-vision"))"PRESENT" else "NEEDS MODEL",
                    "Vision GGUF + projector: "+(pair!=null)+
                        "; native multimodal engine: "+library("asterion-vision"))
            },
            result("Local image enhancement") {
                RuntimeProbe("Local image enhancement",
                    if(library("asterion_realsr_cli"))"PRESENT" else "MISSING",
                    "RealSR native runtime packaged: "+library("asterion_realsr_cli")+
                        ". Upscaling output still requires device testing.")
            },
            result("Asterion Core provider") {
                val p=app.packageManager.resolveContentProvider(
                    CoreSheetBridge.AUTHORITY,0)
                RuntimeProbe("Asterion Core provider",
                    if(p!=null)"PRESENT" else "NOT CONNECTED",
                    if(p!=null)"Matching sheet provider found; imported bytes are verified before use."
                    else "Asterion Core provider is not installed/visible on this phone.")
            },
            RuntimeProbe("AI video engine","NOT INTEGRATED",
                "The current video workflow needs a separate phone-local Nightmare " +
                    "video API. Native Wan inference is not embedded; no artificial " +
                    "video is presented as generated."),
            result("Gallery and cache storage") {
                val available=app.cacheDir.usableSpace
                RuntimeProbe("Gallery and cache storage",
                    if(available>300_000_000L)"PRESENT" else "LOW STORAGE",
                    "Free cache space: "+(available/1_048_576L)+" MiB. " +
                        "Run the sheet round-trip to verify actual MediaStore output.")
            }
        )
    }

    /**
     * Execute the actual sheet renderer for all four sheet roles with on-device
     * synthetic fixtures. This tests rendering and MediaStore writing, not
     * neural understanding. No user libraries or training labels are changed.
     */
    fun sheetRoundTrip(context:Context):String {
        val app=context.applicationContext
        val folder=File(app.cacheDir,"sheet-integration-"+UUID.randomUUID())
        check(folder.mkdirs())
        val published=mutableListOf<Uri>()
        try {
            SheetRole.entries.forEach{role->
                val slots=SheetLayouts.forRole(role).take(2)
                val inputs=slots.mapIndexed{n,slot->
                    val source=File(folder,role.name+"-"+slot.id+".png")
                    val bitmap=Bitmap.createBitmap(440,660,Bitmap.Config.ARGB_8888)
                    try {
                        val canvas=Canvas(bitmap)
                        canvas.drawColor(if(n==0)Color.rgb(221,229,244)
                            else Color.rgb(206,218,230))
                        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color=Color.rgb(42,65,113);textSize=27f
                        }
                        canvas.drawText(role.name,22f,88f,paint)
                        canvas.drawText(slot.label,22f,140f,paint)
                        source.outputStream().use {
                            check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))
                        }
                    } finally {bitmap.recycle()}
                    Uri.fromFile(source).toString()
                }
                val record=SheetRecord(
                    role=role,label="Runtime composition test",
                    sourceUris=inputs,referenceSlots=slots.map{it.id})
                val generated=SheetRenderer.render(app,record)
                published+=generated
                val options=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                requireNotNull(app.contentResolver.openInputStream(generated)).use {
                    BitmapFactory.decodeStream(it,null,options)
                }
                check(options.outWidth==1440&&options.outHeight==2560) {
                    "Incorrect "+role.name+" output sheet dimensions"
                }
            }
            return "PASS: four role-specific sheets rendered, published, reopened, " +
                "and dimension-checked; temporary results cleaned."
        } finally {
            published.forEach {runCatching{app.contentResolver.delete(it,null,null)}}
            folder.deleteRecursively()
        }
    }
}

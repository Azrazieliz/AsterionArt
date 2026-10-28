package com.asterion.art.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.engine.GenerationPass
import com.asterion.art.engine.StillImageEngine
import com.asterion.art.model.CreationMode
import com.asterion.art.model.GeneratorSettings
import com.asterion.art.model.SheetRole
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/**
 * Fallback for native diffusion models whose tokenizer cannot hold a complete
 * nine-panel sheet request in one conditioning tensor.
 *
 * Each illustration is generated independently FROM the actual single-image
 * neural reference, at the selected native model resolution; none is a crop
 * of the user source. The final 9:16 analytical composition labels real new
 * views, not pasted copies of the original reference.
 */
object NeuralSheetPanelRenderer {
    private const val W=1024
    private const val H=1820
    private const val NEGATIVE="wrong identity, bad anatomy, duplicate subject, "+
        "broken joints, blurry, watermark, unrelated accessories"
    private data class Cell(val x:Int,val y:Int,val width:Int,val height:Int)
    private val cells=listOf(
        Cell(22,138,537,1044),
        Cell(580,138,204,331),Cell(798,138,204,331),
        Cell(580,491,204,331),Cell(798,491,204,331),
        Cell(580,844,204,331),Cell(798,844,204,331),
        Cell(22,1246,477,470),Cell(517,1246,485,470)
    )
    private fun sha(bytes:ByteArray):String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString(""){"%02x".format(it)}

    fun build(context:Context,sheetId:String,role:SheetRole,
        original:CreationMode?,engine:StillImageEngine,
        settings:GeneratorSettings,width:Int,height:Int,
        source:ByteArray,referencePngs:List<ByteArray>,
        cancelled:()->Boolean,progress:(Int,Int,String)->Unit):ByteArray {
        require(sheetId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        require(source.size>100)
        val panels=NeuralSheetStoryboard.panels(role,original)
        check(panels.size==cells.size)
        val root=File(context.filesDir,"neural-sheet-views/"+sheetId)
            .also{check(it.mkdirs()||it.isDirectory)}
        val baseSeed=settings.seed?:System.currentTimeMillis()
        val sameSource=if(referencePngs.isNotEmpty())null else source
        val paths=mutableListOf<File>()
        val hashes=mutableSetOf<String>()
        val sourceHash=sha(source)
        for((index,panel) in panels.withIndex()) {
            if(cancelled())throw CancellationException("Sheet generation cancelled")
            val path=File(root,(index+1).toString().padStart(2,'0')+
                "-"+panel.key+".png")
            val prior=if(path.isFile&&path.length()>1024L) {
                val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeFile(path.absolutePath,opts)
                path.takeIf{opts.outWidth>0&&opts.outHeight>0}
            } else null
            val bytes=prior?.readBytes()?:run {
                progress(index,panels.size,"Generating new "+panel.title.lowercase()+" view")
                val prompt=panel.instruction
                val capacity=engine.inspectText(prompt)
                require(capacity.limit>0&&capacity.fits) {
                    "Selected native model cannot fit the mandatory "+
                    panel.title+" view instructions. Install a model with a "+
                    "compatible token context; no source crop is substituted."
                }
                engine.verifyText(prompt)
                engine.verifyText(NEGATIVE)
                val generated=engine.generate(
                    GenerationPass(index+1,prompt,NEGATIVE,
                        if(sameSource==null)1f else 0.78f),
                    settings,width,height,sameSource,baseSeed+index*9473L,
                    cancelled,{done,total->
                        if(cancelled())throw CancellationException()
                        progress(index,panels.size,
                            panel.title+" diffusion "+done+"/"+total.coerceAtLeast(1))
                    },referencePngs=referencePngs).png
                require(generated.size>1024){"Native model returned empty view"}
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeByteArray(generated,0,generated.size,bounds)
                require(bounds.outWidth>=256&&bounds.outHeight>=256) {
                    "Native model produced an invalid newly generated view"
                }
                GeneratedImagePublisher.atomicWrite(path,generated)
                generated
            }
            val digest=sha(bytes)
            require(digest!=sourceHash) {
                "Neural sheet panel exactly duplicated the source image"
            }
            require(hashes.add(digest)) {
                "Neural sheet model repeated the same pixels in multiple views"
            }
            paths+=path
            progress(index+1,panels.size,"Generated "+panel.title)
        }
        return compose(paths,panels,role,original,
            if(role==SheetRole.ART_STYLE)source else null)
    }

    private fun fit(canvas:Canvas,image:Bitmap,rect:RectF,paint:Paint) {
        val scale=minOf(rect.width()/image.width,rect.height()/image.height)
        val w=image.width*scale
        val h=image.height*scale
        val x=rect.centerX()
        val y=rect.centerY()
        canvas.drawBitmap(image,null,
            RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint)
    }
    private fun compose(paths:List<File>,panels:List<NeuralSheetPanel>,
        role:SheetRole,original:CreationMode?,styleEvidence:ByteArray?):ByteArray {
        val page=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(page)
            canvas.drawColor(Color.rgb(247,249,253))
            val ink=Paint(Paint.ANTI_ALIAS_FLAG).apply{
                color=Color.rgb(27,37,53);textSize=43f;isFakeBoldText=true
            }
            val secondary=Paint(Paint.ANTI_ALIAS_FLAG).apply{
                color=Color.rgb(82,105,128);textSize=22f
            }
            val stroke=Paint(Paint.ANTI_ALIAS_FLAG).apply{
                color=Color.rgb(176,194,212);style=Paint.Style.STROKE
                strokeWidth=2f
            }
            canvas.drawText((original?.display?:role.display).uppercase()+
                "  ·  MASTER SHEET",24f,64f,ink)
            canvas.drawText("Source-conditioned neural reconstruction · "+
                "nine distinct generated analytical views",25f,104f,secondary)
            val media=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            for(i in panels.indices){
                val cell=cells[i]
                val rect=RectF(cell.x.toFloat(),cell.y.toFloat(),
                    (cell.x+cell.width).toFloat(),
                    (cell.y+cell.height).toFloat())
                canvas.drawRect(rect,stroke)
                val name=panels[i].title
                val labelPaint=secondary.apply{
                    textSize=if(name.length>20)15f else 19f
                    isFakeBoldText=true
                }
                canvas.drawText(name,rect.left+8,rect.top+27,labelPaint)
                val art=BitmapFactory.decodeFile(paths[i].absolutePath)
                    ?:error("Saved generated view cannot be decoded: "+name)
                try {
                    fit(canvas,art,RectF(rect.left+7,rect.top+38,
                        rect.right-7,rect.bottom-8),media)
                }finally{art.recycle()}
            }
            if(styleEvidence!=null) {
                val originalImage=BitmapFactory.decodeByteArray(
                    styleEvidence,0,styleEvidence.size)
                    ?:error("Original style evidence cannot be decoded")
                try {
                    val frame=RectF(48f,926f,257f,1131f)
                    canvas.drawRect(frame,stroke)
                    fit(canvas,originalImage,frame,media)
                    val small=secondary.apply{textSize=14f}
                    canvas.drawText("SOURCE STYLE EVIDENCE",50f,1154f,small)
                }finally{originalImage.recycle()}
            }
            val footer=secondary.apply{textSize=19f;isFakeBoldText=false}
            canvas.drawText("ANALYTICAL VIEWS · ALL PANEL ARTWORK NEURALLY GENERATED",
                24f,1781f,footer)
            canvas.drawText("Profile and rear accuracy require visual review",
                24f,1806f,footer)
            return ByteArrayOutputStream().use{out->
                check(page.compress(Bitmap.CompressFormat.PNG,100,out))
                out.toByteArray()
            }
        }finally{page.recycle()}
    }
}

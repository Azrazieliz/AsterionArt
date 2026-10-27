package com.asterion.art.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.asterion.art.data.GeneratedImagePublisher
import java.io.ByteArrayOutputStream
import java.io.File

data class DesignCrop(val x:Int,val y:Int,val width:Int,val height:Int)

object DesignPreviewCrops {
    /** Exactly partition the actual raster; do not invent identities. */
    fun rectangle(width:Int,height:Int,columns:Int,rows:Int,index:Int):DesignCrop {
        require(width>0 && height>0)
        require(columns>0 && rows>0 && columns<=width && rows<=height)
        require(index in 0 until columns*rows)
        val c=index%columns
        val r=index/columns
        val x1=c*width/columns
        val x2=(c+1)*width/columns
        val y1=r*height/rows
        val y2=(r+1)*height/rows
        return DesignCrop(x1,y1,x2-x1,y2-y1)
    }
    fun generate(context:Context,png:ByteArray,jobId:String,
                 ordinal:Int,index:Int,columns:Int,rows:Int):String? {
        if(columns*rows<2) return null
        require(jobId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        val bitmap=BitmapFactory.decodeByteArray(png,0,png.size)
            ?: error("Cannot decode rendered result")
        try {
            val area=rectangle(bitmap.width,bitmap.height,columns,rows,index)
            val fragment=Bitmap.createBitmap(
                bitmap,area.x,area.y,area.width,area.height)
            val encoded=ByteArrayOutputStream().use { out ->
                try {
                    check(fragment.compress(Bitmap.CompressFormat.PNG,100,out))
                    out.toByteArray()
                } finally { fragment.recycle() }
            }
            val folder=File(context.filesDir,"design-previews").also { it.mkdirs() }
            val result=File(folder,jobId+"-"+ordinal.toString().padStart(3,'0')+".png")
            GeneratedImagePublisher.atomicWrite(result,encoded)
            return Uri.fromFile(result).toString()
        } finally { bitmap.recycle() }
    }
}

package com.asterion.art.methodology

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Compose REAL separately rendered front/profile/rear source images.
 * Never crop a generated multicharacter picture into artificial views.
 * The exact 640x1800 cell fits the existing continuous 3200x1800 sheet.
 */
object CharacterViewComposer {
    enum class View(val label:String,val direction:String) {
        FRONT("front","true-front"),
        PROFILE("profile","90-degree side profile"),
        REAR("rear","direct rear")
    }
    data class Placement(val view:View,val left:Int,val top:Int,val right:Int,val bottom:Int)
    fun placements(width:Int=640,height:Int=1800):List<Placement> {
        require(width>=320 && height>=720)
        val first=width*52/100
        val second=width*76/100
        return listOf(
            Placement(View.FRONT,0,0,first,height),
            Placement(View.PROFILE,first,0,second,height),
            Placement(View.REAR,second,0,width,height)
        )
    }
    fun compose(viewPngs:Map<View,ByteArray>,width:Int=640,height:Int=1800):ByteArray {
        require(viewPngs.keys==View.entries.toSet()) {
            "The full triptych requires genuine front, profile and rear renders"
        }
        val tile=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(tile)
            canvas.drawColor(Color.WHITE)
            val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            for(placement in placements(width,height)) {
                val image=BitmapFactory.decodeByteArray(
                    viewPngs.getValue(placement.view),0,
                    viewPngs.getValue(placement.view).size)
                    ?:error("Native "+placement.view.label+" result is not a decodable image")
                try {
                    require(image.width>=256 && image.height>=256)
                    val slotWidth=placement.right-placement.left-8
                    val slotHeight=height-26
                    val scale=min(slotWidth.toDouble()/image.width,
                        slotHeight.toDouble()/image.height)
                    val destWidth=(image.width*scale).toInt().coerceAtLeast(1)
                    val destHeight=(image.height*scale).toInt().coerceAtLeast(1)
                    val destLeft=placement.left+
                        (placement.right-placement.left-destWidth)/2
                    val destTop=height-16-destHeight
                    canvas.drawBitmap(image,null,Rect(destLeft,destTop,
                        destLeft+destWidth,destTop+destHeight),paint)
                } finally { image.recycle() }
            }
            return ByteArrayOutputStream().use { bytes ->
                check(tile.compress(Bitmap.CompressFormat.PNG,100,bytes))
                bytes.toByteArray()
            }
        } finally { tile.recycle() }
    }
}

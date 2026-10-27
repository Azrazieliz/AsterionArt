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
 * One continuous 16:9 clinical white canvas, five character regions.
 * Each constituent image independently represents one frozen identity's
 * front/90-degree-profile/rear triptych. No dividers, borders or captions.
 */
data class CharacterSheetCell(val index:Int,val left:Int,val top:Int,
                              val right:Int,val bottom:Int)
object CharacterSheetGeometry {
    fun cells(width:Int=3200,height:Int=1800):List<CharacterSheetCell> {
        require(width>=1280&&height>=720&&width*9L==height*16L)
        return (0 until 5).map {i->
            CharacterSheetCell(i,width*i/5,0,width*(i+1)/5,height)
        }
    }
}
object CharacterSheetComposer {
    private fun whiteEnough(value:Int):Boolean {
        val a=value ushr 24
        if(a<60)return true
        val r=value shr 16 and 255
        val g=value shr 8 and 255
        val b=value and 255
        return r>=238&&g>=238&&b>=238
    }
    fun assemble(tiles:List<ByteArray>,width:Int=3200,height:Int=1800):ByteArray {
        require(tiles.size==5){"One complete frozen population sheet needs five renderings"}
        val cells=CharacterSheetGeometry.cells(width,height)
        val sheet=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(sheet)
            canvas.drawColor(Color.WHITE)
            val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                isDither=true
            }
            tiles.forEachIndexed { index,png ->
                val image=BitmapFactory.decodeByteArray(png,0,png.size)
                    ?:error("Character "+(index+1)+" produced undecodable PNG")
                try {
                    require(image.width>=256&&image.height>=256) {
                        "Character reference image resolution is too low"
                    }
                    val corners=listOf(
                        image.getPixel(0,0),
                        image.getPixel(image.width-1,0),
                        image.getPixel(0,image.height-1),
                        image.getPixel(image.width-1,image.height-1))
                    require(corners.count(::whiteEnough)>=3) {
                        "Character "+(index+1)+
                            " has a nonwhite image background. "+
                            "A continuous clinical sheet cannot be assembled without "+
                            "introducing background panels."
                    }
                    val cell=cells[index]
                    val availableW=(cell.right-cell.left)-8
                    val availableH=height-16
                    val ratio=min(
                        availableW.toDouble()/image.width,
                        availableH.toDouble()/image.height)
                    val drawW=(image.width*ratio).toInt().coerceAtLeast(1)
                    val drawH=(image.height*ratio).toInt().coerceAtLeast(1)
                    val left=cell.left+((cell.right-cell.left)-drawW)/2
                    val top=(height-drawH)/2
                    canvas.drawBitmap(image,null,Rect(left,top,left+drawW,top+drawH),paint)
                } finally {image.recycle()}
            }
            return ByteArrayOutputStream().use{bytes->
                check(sheet.compress(Bitmap.CompressFormat.PNG,100,bytes))
                bytes.toByteArray()
            }
        } finally {sheet.recycle()}
    }
}

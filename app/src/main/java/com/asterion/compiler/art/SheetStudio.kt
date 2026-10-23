package com.asterion.compiler.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.CRC32

data class SheetField(val name: String, val label: String, val description: String)

data class AuthoredSheet(
    val id: String,
    val kind: SheetKind,
    val title: String,
    val fields: List<SheetField>,
    val referenceUris: List<String>,
) {
    fun problems(): List<String> = buildList {
        if (title.isBlank()) add("Title required")
        if (fields.none { it.description.isNotBlank() }) add("Specify at least one visual property")
        if (referenceUris.isEmpty()) add("Choose at least one reference image")
        if (referenceUris.size != referenceUris.distinct().size) add("Duplicate image references")
    }
    fun toJson(): JSONObject = JSONObject().apply {
        put("schema", "asterion.sheet.v1")
        put("id", id)
        put("kind", kind.name)
        put("title", title)
        put("fields", JSONArray().apply { fields.forEach {
            put(JSONObject().put("name", it.name).put("label", it.label).put("description", it.description))
        } })
        put("referenceUris", JSONArray(referenceUris))
    }
    fun promptFacts(): String = (listOf(kind.name.replace('_', ' ') + ": " + title) +
        fields.filter { it.description.isNotBlank() }.map { it.label + ": " + it.description.trim() })
        .joinToString("; ")
    companion object {
        fun fromJson(obj: JSONObject): AuthoredSheet {
            require(obj.getString("schema") == "asterion.sheet.v1")
            val fields = obj.getJSONArray("fields")
            val refs = obj.getJSONArray("referenceUris")
            return AuthoredSheet(obj.getString("id"), SheetKind.valueOf(obj.getString("kind")),
                obj.getString("title"),
                (0 until fields.length()).map { val v = fields.getJSONObject(it)
                    SheetField(v.getString("name"), v.getString("label"), v.getString("description"))
                },
                (0 until refs.length()).map { refs.getString(it) })
        }
    }
}

object SheetTemplates {
    fun fields(kind: SheetKind): List<SheetField> {
        val names = when(kind) {
            SheetKind.CHARACTER -> listOf(
                "identity" to "Identity and distinguishing traits",
                "face" to "Face, eyes and expressions",
                "hair" to "Hair: color, shape and construction",
                "anatomy" to "Anatomy, silhouette and nonhuman features",
                "wardrobe" to "Clothing and material construction",
                "angles" to "Front, side and back consistency",
                "variants" to "Forms, variations and identity invariants",
                "avoid" to "Incorrect substitutions")
            SheetKind.ART_STYLE -> listOf(
                "medium" to "Medium, visual genre and linework",
                "render" to "Rendering density, shading and materials",
                "palette" to "Palette and color theory",
                "lighting" to "Lighting and shadow grammar",
                "anatomy" to "Stylization of forms and features",
                "composition" to "Perspective, framing and depth",
                "detail" to "Texture and detail hierarchy",
                "avoid" to "Style exclusions")
            SheetKind.THEME -> listOf(
                "narrative" to "Scene narrative and intended feeling",
                "setting" to "Environment, props and architecture",
                "interaction" to "Character interactions and contact",
                "composition" to "Staging and focal hierarchy",
                "pose" to "Pose, movement and body relationships",
                "lighting" to "Scene lighting and mood",
                "required" to "Mandatory scene properties",
                "avoid" to "Incorrect interpretations")
            SheetKind.ELEMENT -> listOf(
                "identity" to "Element identity and purpose",
                "geometry" to "Shape, geometry and proportions",
                "placement" to "Location, attachment and scale",
                "materials" to "Surface, colors and visual effects",
                "states" to "States, variants and transitions",
                "interaction" to "Interactions with characters or environment",
                "required" to "Non-negotiable element details",
                "avoid" to "Forbidden modifications")
        }
        return names.map { SheetField(it.first, it.second, "") }
    }
}

/** Writes a reproducible board. The user decides its content; no topic filtering. */
class SheetStudio(private val context: Context) {
    fun save(sheet: AuthoredSheet): ReferenceSheet {
        check(sheet.problems().isEmpty()) { sheet.problems().joinToString("; ") }
        val dir = File(context.filesDir, "asterion/sheets").apply { mkdirs() }
        val safe = sheet.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val imageFile = File(dir, safe + ".png")
        val jsonFile = File(dir, safe + ".json")
        val bitmap = draw(sheet)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        val data = SheetMetadata.embed(stream.toByteArray(), sheet.toJson().toString())
        val tmp = File(dir, safe + ".png.part")
        tmp.writeBytes(data)
        check(tmp.renameTo(imageFile)) { "Failed to save sheet image" }
        jsonFile.writeText(sheet.toJson().toString(2))
        return ReferenceSheet(sheet.id, sheet.kind, Uri.fromFile(imageFile).toString(), sheet.title)
    }

    fun load(uri: Uri): AuthoredSheet? = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val json = SheetMetadata.extract(bytes) ?: return null
        AuthoredSheet.fromJson(JSONObject(json))
    }.getOrNull()

    private fun draw(sheet: AuthoredSheet): Bitmap {
        val bitmap = Bitmap.createBitmap(1500, 2100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(16, 21, 29))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(204, 170, 119)
        paint.isFakeBoldText = true
        paint.textSize = 49f
        canvas.drawText(sheet.kind.name.replace('_', ' '), 56f, 82f, paint)
        paint.color = Color.WHITE
        paint.textSize = 37f
        canvas.drawText(sheet.title.take(70), 56f, 146f, paint)
        sheet.referenceUris.take(6).forEachIndexed { index, name ->
            val columns = 3
            val boxWidth = 445f
            val boxHeight = 305f
            val x = 56f + (index % columns) * 468f
            val y = 195f + (index / columns) * 322f
            paint.color = Color.rgb(36, 42, 54)
            canvas.drawRoundRect(RectF(x,y,x+boxWidth,y+boxHeight), 12f,12f,paint)
            val image = runCatching {
                context.contentResolver.openInputStream(Uri.parse(name))?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })
                }
            }.getOrNull()
            if (image != null) {
                val scale = minOf(boxWidth/image.width,boxHeight/image.height)
                val w = image.width * scale
                val h = image.height * scale
                paint.color = Color.WHITE
                canvas.drawBitmap(image, null, RectF(x+(boxWidth-w)/2,y+(boxHeight-h)/2,
                    x+(boxWidth+w)/2,y+(boxHeight+h)/2), paint)
                image.recycle()
            }
        }
        var y = 875f
        for (field in sheet.fields.filter { it.description.isNotBlank() }) {
            if (y > 1960f) break
            paint.color = Color.rgb(204,170,119)
            paint.isFakeBoldText = true
            paint.textSize = 28f
            canvas.drawText(field.label.take(85), 56f, y, paint)
            paint.isFakeBoldText = false
            paint.color = Color.rgb(233,236,240)
            paint.textSize = 24f
            y += 38f
            for (part in field.description.chunked(108).take(3)) {
                canvas.drawText(part, 66f, y, paint)
                y += 31f
            }
            y += 27f
        }
        return bitmap
    }
}

/** Embeds UTF-8 structured fields in an ancillary PNG iTXt chunk. */
object SheetMetadata {
    private val signature = byteArrayOf(137.toByte(),80,78,71,13,10,26,10)
    private val key = "asterion.sheet.v1".toByteArray(StandardCharsets.US_ASCII)
    private val kind = "iTXt".toByteArray(StandardCharsets.US_ASCII)
    fun embed(png: ByteArray, json: String): ByteArray {
        require(png.size >= 20 && png.copyOfRange(0,8).contentEquals(signature))
        require(String(png, png.size-8,4,Charsets.US_ASCII) == "IEND")
        val payload = key + byteArrayOf(0,0,0,0,0) + json.toByteArray(Charsets.UTF_8)
        val crc = CRC32().apply { update(kind); update(payload) }.value.toInt()
        val chunk = ByteBuffer.allocate(payload.size + 12).order(ByteOrder.BIG_ENDIAN)
            .putInt(payload.size).put(kind).put(payload).putInt(crc).array()
        return png.copyOfRange(0,png.size-12) + chunk + png.copyOfRange(png.size-12,png.size)
    }
    fun extract(png: ByteArray): String? {
        if (png.size < 20 || !png.copyOfRange(0,8).contentEquals(signature)) return null
        var offset = 8
        while (offset + 12 <= png.size) {
            val length = ByteBuffer.wrap(png,offset,4).order(ByteOrder.BIG_ENDIAN).int
            if (length < 0 || length > 8*1024*1024 || offset+length+12 > png.size) return null
            val type = String(png,offset+4,4,Charsets.US_ASCII)
            if (type == "iTXt") {
                val bytes = png.copyOfRange(offset+8,offset+8+length)
                if (bytes.size > key.size+5 &&
                    bytes.copyOfRange(0,key.size).contentEquals(key) &&
                    bytes.copyOfRange(key.size,key.size+5).contentEquals(byteArrayOf(0,0,0,0,0))) {
                    val crc = CRC32().apply { update(kind); update(bytes) }.value.toInt()
                    val actual = ByteBuffer.wrap(png,offset+8+length,4).order(ByteOrder.BIG_ENDIAN).int
                    if (crc != actual) return null
                    return String(bytes,key.size+5,bytes.size-key.size-5,Charsets.UTF_8)
                }
            }
            if (type == "IEND") return null
            offset += 12+length
        }
        return null
    }
}

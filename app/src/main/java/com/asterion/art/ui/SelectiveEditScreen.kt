package com.asterion.art.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.EditJobStore
import com.asterion.art.engine.SelectiveImageEditor
import com.asterion.art.model.GradedAsset
import com.asterion.art.model.TaskKind
import com.asterion.art.work.SelectiveEditWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun normalize(pos: Offset, frame: IntSize, bitmap: Bitmap): Pair<Float, Float>? {
    val ratio = min(frame.width.toFloat() / bitmap.width,
        frame.height.toFloat() / bitmap.height)
    val visibleW = bitmap.width * ratio
    val visibleH = bitmap.height * ratio
    val left = (frame.width - visibleW) / 2
    val top = (frame.height - visibleH) / 2
    val x = (pos.x - left) / visibleW
    val y = (pos.y - top) / visibleH
    if (x !in 0f..1f || y !in 0f..1f) return null
    return x to y
}

private fun screenPoint(pos: Pair<Float, Float>, frame: androidx.compose.ui.geometry.Size,
                        bitmap: Bitmap): Offset {
    val ratio = min(frame.width / bitmap.width,frame.height / bitmap.height)
    val visibleW = bitmap.width * ratio
    val visibleH = bitmap.height * ratio
    return Offset((frame.width-visibleW)/2 + pos.first * visibleW,
        (frame.height-visibleH)/2 + pos.second * visibleH)
}

@Composable
fun SelectiveEditScreen(asset: GradedAsset?, repo: ArtRepository, onQueued: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(asset?.uri) { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var radius by remember { mutableStateOf(12f) }
    var strength by remember { mutableStateOf(0.35f) }
    val strokes = remember(asset?.uri) { mutableStateListOf<List<Pair<Float,Float>>>() }

    LaunchedEffect(asset?.uri) {
        if (asset == null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(Uri.parse(asset.uri))?.use {
                BitmapFactory.decodeStream(it)
            }
        }
        if (bitmap == null) error = "Original artwork unavailable"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SELECTIVE EDITING", color = Gold)
                Text("Mask-only correction", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text("Paint only the pixels you want to change. Everything outside your mask is restored exactly from the original image.",color = Mist)
            }
        }
        item {
            val source = bitmap
            if (source == null) {
                Text("Loading artwork…", Modifier.padding(20.dp),color = Mist)
            } else {
                Box(Modifier.fillMaxWidth().height(440.dp)) {
                    Image(bitmap = source.asImageBitmap(),contentDescription = "Editable artwork",
                        modifier = Modifier.fillMaxSize(),contentScale = ContentScale.Fit)
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(source) {
                                detectDragGestures(
                                    onDragStart = { p ->
                                        val point = normalize(p,size,source)
                                        if (point != null) strokes.add(listOf(point))
                                    },
                                    onDrag = { change, _ ->
                                        val point = normalize(change.position,size,source)
                                        if (point != null && strokes.isNotEmpty()) {
                                            val index = strokes.lastIndex
                                            strokes[index] = strokes[index] + point
                                        }
                                        change.consume()
                                    }
                                )
                            }
                            .pointerInput(source) {
                                detectTapGestures { p ->
                                    normalize(p,size,source)?.let { strokes.add(listOf(it)) }
                                }
                            }
                    ) {
                        val scale = min(size.width / source.width,size.height / source.height)
                        for (stroke in strokes) {
                            val visible = stroke.map { screenPoint(it,size,source) }
                            if (visible.size == 1) drawCircle(Color(0xCCF3A6B4),
                                radius * scale,visible.first())
                            for (i in 1 until visible.size) drawLine(
                                Color(0xCCF3A6B4),visible[i-1],visible[i],
                                strokeWidth = radius * scale * 2,
                                cap = StrokeCap.Round)
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Brush radius: " + radius.roundToInt() + " image pixels",color = Gold)
                Slider(value = radius,onValueChange = { radius = it },valueRange = 1f..80f)
                Text("Edit denoise: " + "%.2f".format(strength),color = Gold)
                Slider(value = strength,onValueChange = { strength = it },
                    valueRange = 0.05f..0.9f)
            }
        }
        item {
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                    enabled = strokes.isNotEmpty(),
                    modifier = Modifier.weight(1f)) { Text("Undo") }
                OutlinedButton(onClick = { strokes.clear() },
                    enabled = strokes.isNotEmpty(),
                    modifier = Modifier.weight(1f)) { Text("Clear mask") }
            }
        }
        item {
            val source = bitmap
            val canEdit = source != null && asset?.sourceJobId != null && strokes.isNotEmpty()
            Button(
                enabled = canEdit,
                onClick = {
                    if (source != null && asset?.sourceJobId != null) {
                        runCatching {
                            val mask = SelectiveImageEditor.paintMask(
                                source.width,source.height,strokes.toList(),radius.roundToInt())
                            val output = ByteArrayOutputStream()
                            try {
                                require(mask.compress(Bitmap.CompressFormat.PNG,100,output))
                            } finally { mask.recycle() }
                            val plan = EditJobStore(context).create(
                                asset.uri,asset.sourceJobId,output.toByteArray(),strength)
                            val task = repo.recordTask(plan.id,TaskKind.IMAGE_EDIT)
                            SelectiveEditWorker.enqueue(context,plan.id,task.id)
                        }.onSuccess { onQueued() }
                            .onFailure { error = it.message }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            ) { Text("Apply masked inpainting in background") }
        }
        if (asset?.sourceJobId == null) item {
            Text("This imported artwork has no recorded generation specification. " +
                "Selective AI editing requires a source generation job.",
                modifier = Modifier.padding(horizontal = 18.dp),color = Mist)
        }
        if (error != null) item {
            Text(error.orEmpty(),Modifier.padding(horizontal = 18.dp),color = Red)
        }
    }
}

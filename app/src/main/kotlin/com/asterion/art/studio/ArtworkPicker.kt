package com.asterion.art.studio

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asterion.art.runtime.ArtworkDocuments
import com.asterion.art.runtime.ArtworkPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ArtworkLocation(val id: String, val name: String, val offset: Int = 0)

@Composable
internal fun ArtworkPicker(onDismiss: () -> Unit, onSelect: (Uri) -> Unit) {
    val context = LocalContext.current
    val documents = remember { ArtworkDocuments(context) }
    val scope = rememberCoroutineScope()
    var tree by remember { mutableStateOf(documents.selectedTree()) }
    var path by remember(tree) { mutableStateOf(emptyList<ArtworkLocation>()) }
    var rootOffset by remember(tree) { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableStateOf(0) }
    val directory = tree?.let { root ->
        path.lastOrNull() ?: ArtworkLocation(DocumentsContract.getTreeDocumentId(root), "Artwork library", rootOffset)
    }
    var page by remember(tree, directory, revision) { mutableStateOf<ArtworkPage?>(null) }
    val chooseTree = rememberLauncherForActivityResult(OpenDocumentTree()) { selected ->
        if (selected != null) scope.launch {
            try {
                withContext(Dispatchers.IO) { documents.select(selected) }
                tree = selected
                revision++
            } catch (stop: CancellationException) {
                throw stop
            } catch (failure: Exception) {
                error = failure.message ?: "Could not open the artwork folder"
            }
        }
    }
    LaunchedEffect(tree, directory, revision) {
        error = null
        val root = tree ?: return@LaunchedEffect
        val folder = directory ?: return@LaunchedEffect
        try {
            do {
                val current = withContext(Dispatchers.IO) { documents.page(root, folder.id, folder.offset) }
                page = current
                if (current.loading) delay(750)
            } while (current.loading)
        } catch (stop: CancellationException) {
            throw stop
        } catch (failure: Exception) {
            error = failure.message ?: "The artwork folder could not be listed"
        }
    }
    fun loadOffset(offset: Int) {
        if (path.isEmpty()) rootOffset = offset
        else path = path.dropLast(1) + path.last().copy(offset = offset)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(directory?.name ?: "Artwork library", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (path.isNotEmpty()) TextButton(onClick = { path = path.dropLast(1) }) { Text("Parent folder") }
                when {
                    error != null -> {
                        Text(checkNotNull(error))
                        if (tree != null) TextButton(onClick = { revision++ }) { Text("Try again") }
                    }
                    tree == null -> Text("Choose Art/Artworks or another artwork folder in Rounder or Nexus.")
                    page == null || page?.loading == true -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    else -> {
                        val entries = checkNotNull(page).entries
                        if (entries.isEmpty()) Text("No artwork on this page")
                        LazyColumn(Modifier.heightIn(max = 330.dp)) {
                            items(entries, key = { it.id }) { entry ->
                                TextButton(onClick = {
                                    if (entry.directory) path = path + ArtworkLocation(entry.id, entry.name)
                                    else onSelect(documents.uri(checkNotNull(tree), entry))
                                }) {
                                    Text(
                                        if (entry.directory) "${entry.name}  ›" else entry.name,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        Row {
                            val offset = directory?.offset ?: 0
                            if (offset > 0) TextButton(onClick = { loadOffset(offset - ArtworkDocuments.PAGE_SIZE) }) {
                                Text("Previous")
                            }
                            page?.nextOffset?.let { next ->
                                TextButton(onClick = { loadOffset(next) }) { Text("Next page") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { chooseTree.launch(tree) }) { Text(if (tree == null) "Choose folder" else "Change folder") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

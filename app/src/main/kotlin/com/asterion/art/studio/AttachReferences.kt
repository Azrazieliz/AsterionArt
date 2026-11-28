package com.asterion.art.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.asterion.art.creation.AssetRef
import com.asterion.art.creation.AssetSource
import com.asterion.art.creation.ContentArtifact
import com.asterion.art.creation.CreationRun
import com.asterion.art.creation.Mode
import com.asterion.art.creation.RefRole
import com.asterion.art.creation.RunStatus
import com.asterion.art.runtime.ArtWorkspace
import java.io.File
import java.text.DateFormat
import java.util.Date

internal fun outputReference(run: CreationRun, artifact: ContentArtifact) = AssetRef(
    artifact.sha256, AssetSource.ART_OUTPUT, "${run.id}/${artifact.sha256}",
    run.request.mode.title, artifact.sha256,
)

internal fun referenceOutputs(role: RefRole, runs: List<CreationRun>): List<Pair<CreationRun, ContentArtifact>> {
    val modes = when (role) {
        RefRole.CHARACTER -> listOf(Mode.CHARACTER_SHEET)
        RefRole.ART_STYLE -> listOf(Mode.STYLE_SHEET)
        RefRole.THEME -> listOf(Mode.THEME_SHEET)
        RefRole.SOURCE, RefRole.KEYFRAME -> Mode.entries.filterNot { it in setOf(Mode.THEME, Mode.CLIP, Mode.ANIMATION) }
        RefRole.OUTFIT -> listOf(Mode.OUTFIT)
        else -> emptyList()
    }
    return runs.filter { it.status == RunStatus.COMPLETE && it.request.mode in modes }.flatMap { run ->
        ((run.finalImage?.let(::listOf) ?: run.independentImages) + run.savedImages)
            .distinctBy { it.artifact.sha256 }.map { run to it.artifact }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachReferences(
    role: RefRole,
    runs: List<CreationRun>,
    workspace: ArtWorkspace,
    onDismiss: () -> Unit,
    onFiles: () -> Unit,
    onLibrary: (() -> Unit)?,
    onShared: (() -> Unit)?,
    onProject: (CreationRun, ContentArtifact) -> Unit,
) {
    val outputs = remember(role, runs) { referenceOutputs(role, runs) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("Add ${roleLabel(role)}", style = MaterialTheme.typography.headlineSmall) }
            if (onShared != null) item {
                Button(onShared, Modifier.fillMaxWidth()) { Text("Use shared file") }
            }
            item {
                Button(onFiles, Modifier.fillMaxWidth()) { Text("Browse files") }
            }
            if (onLibrary != null) item {
                OutlinedButton(onLibrary, Modifier.fillMaxWidth()) { Text("Browse artwork library") }
            }
            if (role == RefRole.GEOMETRY) item {
                Text("GLB or Foundry editable package", style = MaterialTheme.typography.bodySmall)
            }
            if (outputs.isNotEmpty()) item {
                Text("From Projects", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
            }
            items(outputs, key = { (run, artifact) -> "${run.id}/${artifact.sha256}" }) { (run, artifact) ->
                val date = remember(run.createdAt) { DateFormat.getDateTimeInstance().format(Date(run.createdAt)) }
                Surface(
                    onClick = { onProject(run, artifact) },
                    shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            File(workspace.directory, "outputs/${run.id}/${artifact.sha256}"),
                            null, Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(run.request.mode.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(date, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

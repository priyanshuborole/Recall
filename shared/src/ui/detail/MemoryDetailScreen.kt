package com.zerotwodev.recall.ui.detail

import androidx.compose.foundation.BorderStroke
import com.zerotwodev.recall.util.toDisplayDate
import com.zerotwodev.recall.domain.Reflection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerotwodev.recall.domain.MemoryDetails
import com.zerotwodev.recall.presentation.MemoryDetailUiState
import com.zerotwodev.recall.presentation.MemoryDetailViewModel
import com.zerotwodev.recall.ui.ImageStore
import com.zerotwodev.recall.ui.PhotoThumbnail
import com.zerotwodev.recall.ui.RecallIcons
import com.zerotwodev.recall.ui.capture.PersonInitial
import com.zerotwodev.recall.ui.components.MoodChip
import com.zerotwodev.recall.util.toFullDateTime

/** Board 7: photo header when there is one, then date, title, chips, more photos and the words. */
@Composable
fun MemoryDetailScreen(
    viewModel: MemoryDetailViewModel,
    imageStore: ImageStore,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    when (val s = state) {
        MemoryDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        MemoryDetailUiState.NotFound -> Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(16.dp)) { RoundButton(RecallIcons.Back, "Back", onBack) }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This memory is no longer here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        is MemoryDetailUiState.Loaded -> MemoryContent(
            details = s.details,
            imageStore = imageStore,
            onBack = onBack,
            onEdit = onEdit,
            onToggleHidden = { viewModel.setHidden(!s.memory.isHidden) },
            onAddReflection = viewModel::addReflection,
            onDelete = { confirmDelete = true },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this memory?") },
            text = { Text("It will be removed from your timeline.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MemoryContent(
    details: MemoryDetails,
    imageStore: ImageStore,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleHidden: () -> Unit,
    onAddReflection: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val memory = details.memory
    val header = details.photos.firstOrNull()
    val buttons: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            RoundButton(RecallIcons.Back, "Back", onBack)
            Box(Modifier.weight(1f))
            MoreMenu(isHidden = memory.isHidden, onEdit = onEdit, onToggleHidden = onToggleHidden, onDelete = onDelete)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (header != null) {
            Box(Modifier.fillMaxWidth().height(236.dp)) {
                PhotoThumbnail(
                    header.fileName,
                    imageStore,
                    Modifier.fillMaxSize(),
                    maxSidePx = 1280,
                    shape = RectangleShape,
                )
                buttons()
            }
        } else {
            buttons()
        }

        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = if (header != null) 20.dp else 0.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    memory.occurredAt.toFullDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    memory.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 27.sp, lineHeight = 32.sp),
                )
            }
            if (memory.mood != null || details.place != null || details.people.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    memory.mood?.let { MoodChip(it) }
                    details.place?.let { place ->
                        DetailChip(place.name) {
                            Icon(RecallIcons.Pin, contentDescription = null, modifier = Modifier.size(13.dp))
                        }
                    }
                    details.people.forEach { person -> DetailChip(person.name) { PersonInitial(person.name) } }
                }
            }
            val morePhotos = details.photos.drop(1)
            if (morePhotos.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    morePhotos.forEach { photo -> PhotoThumbnail(photo.fileName, imageStore, Modifier.size(84.dp)) }
                }
            }
            if (memory.body.isNotBlank()) {
                Text(
                    memory.body,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 24.sp),
                )
            }
            memory.transcript?.takeIf { it.isNotBlank() && memory.body.isBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LookingBack(details.reflections, onAddReflection)
        }
    }
}

/** 44 dp circle that stays readable over a photo. */
@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun MoreMenu(isHidden: Boolean, onEdit: () -> Unit, onToggleHidden: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundButton(RecallIcons.More, "More options") { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    open = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(if (isHidden) "Show in recaps" else "Hide from recaps") },
                onClick = {
                    open = false
                    onToggleHidden()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun DetailChip(label: String, leading: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(
            Modifier.height(28.dp).padding(start = 5.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            leading()
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Thoughts added later. Saved when the person presses Return or Add. */
@Composable
private fun LookingBack(reflections: List<Reflection>, onAdd: (String) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val serifItalic = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, fontSize = 14.5.sp)
    fun submit() {
        if (draft.isNotBlank()) {
            onAdd(draft)
            draft = ""
            focus.clearFocus()
        }
    }

    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = outline)
        Text("Looking back", style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp))
        reflections.forEach { reflection ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(reflection.text, style = serifItalic.copy(fontSize = 15.sp, lineHeight = 22.sp))
                Text(reflection.createdAt.toDisplayDate(), style = MaterialTheme.typography.labelMedium, color = muted)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .drawBehind {
                    drawRoundRect(
                        color = outline,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                    )
                }
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(RecallIcons.Plus, contentDescription = null, tint = muted, modifier = Modifier.size(16.dp))
            Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
                if (draft.isEmpty()) Text("Add a thought, looking back…", style = serifItalic, color = muted)
                BasicTextField(
                    value = draft,
                    onValueChange = { input ->
                        if (input.endsWith("\n")) {
                            draft = input.trimEnd('\n')
                            submit()
                        } else {
                            draft = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Add a reflection" },
                    textStyle = serifItalic.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
            if (draft.isNotBlank()) TextButton(onClick = ::submit) { Text("Add") }
        }
    }
}

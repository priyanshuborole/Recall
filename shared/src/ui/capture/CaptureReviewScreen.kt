package com.zerotwodev.recall.ui.capture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerotwodev.recall.domain.MemoryDetails
import com.zerotwodev.recall.domain.MemoryType
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.domain.Person
import com.zerotwodev.recall.presentation.CaptureReviewViewModel
import com.zerotwodev.recall.ui.ImageStore
import com.zerotwodev.recall.ui.PhotoThumbnail
import com.zerotwodev.recall.ui.RecallIcons
import com.zerotwodev.recall.ui.components.MoodChip
import com.zerotwodev.recall.ui.theme.colors

private enum class ReviewSheet { MOOD, PEOPLE, PLACE }

@Composable
fun CaptureReviewScreen(
    viewModel: CaptureReviewViewModel,
    imageStore: ImageStore,
    onClose: () -> Unit,
    onMessage: (String) -> Unit,
    editing: Boolean = false,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<ReviewSheet?>(null) }

    LaunchedEffect(viewModel) { viewModel.messages.collect { onMessage(it) } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveNow() }
    DisposableEffect(viewModel) { onDispose { viewModel.saveNow() } }

    val details = state.details
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        details == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This memory is no longer here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> ReviewContent(
            details = details,
            imageStore = imageStore,
            onTitleChange = viewModel::onTitleChange,
            onBodyChange = viewModel::onBodyChange,
            editing = editing,
            onOpenSheet = { sheet = it },
            onRemoveMood = { viewModel.setMood(null) },
            onRemovePerson = viewModel::removePerson,
            onRemovePlace = { viewModel.setPlace(null) },
            onAddPhotos = viewModel::addPhotos,
            onClose = onClose,
        )
    }

    if (details != null) {
        when (sheet) {
            ReviewSheet.MOOD -> MoodSheet(
                selected = details.memory.mood,
                onSelect = {
                    viewModel.setMood(it)
                    sheet = null
                },
                onDismiss = { sheet = null },
            )
            ReviewSheet.PEOPLE -> PeopleSheet(
                suggestions = state.allPeople.filter { person -> details.people.none { it.id == person.id } },
                onAdd = {
                    viewModel.addPerson(it)
                    sheet = null
                },
                onDismiss = { sheet = null },
            )
            ReviewSheet.PLACE -> PlaceSheet(
                initial = details.place?.name.orEmpty(),
                onSave = {
                    viewModel.setPlace(it)
                    sheet = null
                },
                onDismiss = { sheet = null },
            )
            null -> Unit
        }
    }
}

@Composable
private fun ReviewContent(
    details: MemoryDetails,
    imageStore: ImageStore,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    editing: Boolean,
    onOpenSheet: (ReviewSheet) -> Unit,
    onRemoveMood: () -> Unit,
    onRemovePerson: (String) -> Unit,
    onRemovePlace: () -> Unit,
    onAddPhotos: () -> Unit,
    onClose: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart).padding(end = 8.dp)) {
                Icon(RecallIcons.Close, contentDescription = "Close")
            }
            if (editing) {
                Text("Edit memory", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
            } else {
                SavedPill(Modifier.align(Alignment.Center))
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(14.dp))
            Text(if (editing) "Title" else "Suggested title · tap to edit", style = MaterialTheme.typography.bodySmall, color = muted)
            TitleField(details.memory.id, details.memory.title, onTitleChange)
            if (editing) {
                Spacer(Modifier.height(18.dp))
                Text("What happened", style = MaterialTheme.typography.bodySmall, color = muted)
                BodyField(details.memory.id, details.memory.body, onBodyChange)
            }

            if (!editing && details.memory.type == MemoryType.VOICE) {
                val audio = details.audio
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(RecallIcons.Mic, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp))
                    Text(if (audio != null) "Voice note · ${formatDuration(audio.durationMs ?: 0)}" else "Voice note · transcribed on device", style = MaterialTheme.typography.bodySmall, color = muted)
                }
                Spacer(Modifier.height(8.dp))
                val transcript = details.memory.transcript?.takeIf { it.isNotBlank() }
                Text(
                    transcript ?: "No transcript for this note",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (transcript == null) muted else MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipRow("Mood") {
                    details.memory.mood?.let { mood ->
                        val colors = mood.colors()
                        RemovableChip(mood.label, "Remove ${mood.label}", onRemoveMood, container = colors.container, content = colors.content) {
                            Box(Modifier.size(6.dp).background(colors.content, CircleShape))
                        }
                    }
                    AddChip("Add mood") { onOpenSheet(ReviewSheet.MOOD) }
                }
                ChipRow("People") {
                    details.people.forEach { person ->
                        RemovableChip(person.name, "Remove ${person.name}", { onRemovePerson(person.id) }) {
                            PersonInitial(person.name)
                        }
                    }
                    AddChip("Add person") { onOpenSheet(ReviewSheet.PEOPLE) }
                }
                ChipRow("Place") {
                    val place = details.place
                    if (place != null) {
                        RemovableChip(place.name, "Remove place", onRemovePlace) {
                            Icon(RecallIcons.Pin, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        AddChip("Add place") { onOpenSheet(ReviewSheet.PLACE) }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Photos", style = MaterialTheme.typography.bodySmall, color = muted)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                details.photos.forEach { photo -> PhotoThumbnail(photo.fileName, imageStore, Modifier.size(84.dp)) }
                Surface(
                    onClick = onAddPhotos,
                    modifier = Modifier.size(84.dp),
                    shape = MaterialTheme.shapes.small,
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(RecallIcons.Plus, contentDescription = null, tint = muted, modifier = Modifier.size(20.dp))
                        Text("Add photo", style = MaterialTheme.typography.labelMedium, color = muted)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(),
        ) { Text("Done", fontSize = 16.sp) }
        Text(
            if (editing) "Changes save as you go" else "Already saved — editing is optional",
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SavedPill(modifier: Modifier = Modifier) {
    val colors = Mood.CALM.colors()
    Surface(modifier = modifier, shape = CircleShape, color = colors.container, contentColor = colors.content) {
        Row(
            Modifier.height(32.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(RecallIcons.Check, contentDescription = null, modifier = Modifier.size(15.dp))
            Text("Saved", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TitleField(memoryId: String, saved: String, onChange: (String) -> Unit) {
    var title by rememberSaveable(memoryId) { mutableStateOf(saved) }
    val ink = MaterialTheme.colorScheme.onBackground
    val focus = LocalFocusManager.current
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = title,
            onValueChange = { input ->
                // A title is one line, so Return finishes editing and closes the keyboard.
                val cleaned = input.replace("\n", "")
                title = cleaned
                onChange(cleaned)
                if (cleaned != input) focus.clearFocus()
            },
            modifier = Modifier.weight(1f).semantics { contentDescription = "Memory title" },
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = ink, fontSize = 25.sp, lineHeight = 30.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
        Icon(
            RecallIcons.Pen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp).size(18.dp),
        )
    }
    HorizontalDivider(Modifier.padding(top = 8.dp), thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun BodyField(memoryId: String, saved: String, onChange: (String) -> Unit) {
    var body by rememberSaveable(memoryId) { mutableStateOf(saved) }
    Box(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .padding(12.dp),
    ) {
        if (body.isEmpty()) {
            Text("A sentence is enough", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BasicTextField(
            value = body,
            onValueChange = {
                body = it
                onChange(it)
            },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "What happened" },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ChipRow(label: String, chips: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            modifier = Modifier.width(52.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            chips()
        }
    }
}

@Composable
private fun RemovableChip(
    label: String,
    removeDescription: String,
    onRemove: () -> Unit,
    container: Color = Color.Transparent,
    content: Color = MaterialTheme.colorScheme.onBackground,
    leading: @Composable () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (container == Color.Transparent) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Row(
            Modifier.height(32.dp).padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading()
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(RecallIcons.Close, contentDescription = removeDescription, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun AddChip(description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Box(Modifier.height(32.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text("+ Add", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val initialColors = listOf(
    Color(0xFFEBDCC8), Color(0xFFDDE3D2), Color(0xFFD9E2EA), Color(0xFFEBD6D0), Color(0xFFE4DCEB),
)

/** A soft circle with the person's first letter. The colour depends only on the name. */
@Composable
fun PersonInitial(name: String) {
    val key = name.trim().lowercase()
    val color = initialColors[key.fold(0) { sum, c -> (sum * 31 + c.code) and 0xFFFF } % initialColors.size]
    Box(Modifier.size(22.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            key.firstOrNull()?.uppercase().orEmpty(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = Color(0xFF2B211A),
        )
    }
}

@Composable
private fun ReviewBottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) { content() }
    }
}

@Composable
private fun MoodSheet(selected: Mood?, onSelect: (Mood?) -> Unit, onDismiss: () -> Unit) {
    ReviewBottomSheet(onDismiss) {
        Text("How did it feel?", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Mood.entries.forEach { option ->
                MoodChip(option, selected = selected == option, onClick = { onSelect(if (selected == option) null else option) })
            }
        }
    }
}

@Composable
private fun PeopleSheet(suggestions: List<Person>, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val matches = remember(name, suggestions) {
        suggestions.filter { it.name.contains(name.trim(), ignoreCase = true) }.take(8)
    }
    ReviewBottomSheet(onDismiss) {
        Text("Who was there?", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("A name") },
        )
        if (matches.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                matches.forEach { person ->
                    Surface(
                        onClick = { onAdd(person.name) },
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Row(
                            Modifier.height(36.dp).padding(start = 6.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PersonInitial(person.name)
                            Text(person.name, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        Button(onClick = { onAdd(name) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Add") }
    }
}

@Composable
private fun PlaceSheet(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    ReviewBottomSheet(onDismiss) {
        Text("Where was this?", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("A place") },
        )
        Button(onClick = { onSave(name) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save") }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms + 500) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

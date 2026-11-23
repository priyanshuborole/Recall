package com.zerotwodev.recall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.domain.MemoryDraft
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.domain.MemoryType
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.domain.PhotoPickResult
import com.zerotwodev.recall.domain.PhotoPicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

data class HomeUiState(
    val isLoading: Boolean = true,
    val now: Instant? = null,
    val onThisDay: Memory? = null,
    val recent: List<Memory> = emptyList(),
)

class HomeViewModel(
    private val repository: MemoryRepository,
    private val photoPicker: PhotoPicker,
    private val clock: Clock,
) : ViewModel() {

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()
    private val _savedMemories = Channel<String>(Channel.BUFFERED)

    /** Ids of photo memories just saved, so Home can open their review. */
    val savedMemories: Flow<String> = _savedMemories.receiveAsFlow()
    private var photoJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeRecent(limit = 3),
        repository.observeOnThisDay(),
    ) { recent, onThisDay ->
        HomeUiState(
            isLoading = false,
            now = clock.now(),
            onThisDay = onThisDay.firstOrNull(),
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun saveTextMemory(text: String, mood: Mood?) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.create(MemoryDraft(body = text, type = MemoryType.TEXT, mood = mood))
        }
    }

    fun addPhoto() {
        if (photoJob?.isActive == true) return
        photoJob = viewModelScope.launch {
            when (val result = photoPicker.pick()) {
                PhotoPickResult.Cancelled -> Unit
                PhotoPickResult.Failed -> _messages.send("That photo couldn't be added. Please try another.")
                is PhotoPickResult.Saved -> withContext(NonCancellable) { savePhotos(result.fileNames) }
            }
        }
    }

    private suspend fun savePhotos(fileNames: List<String>) {
        try {
            val title = if (fileNames.size == 1) "Photo" else "${fileNames.size} photos"
            val id = repository.createPhotos(MemoryDraft(body = "", type = MemoryType.PHOTO, title = title), fileNames)
            _savedMemories.trySend(id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fileNames.forEach { photoPicker.delete(it) }
            _messages.trySend("Those photos couldn't be added. Please try again.")
        }
    }
}

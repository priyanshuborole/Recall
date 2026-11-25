package com.zerotwodev.recall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwodev.recall.domain.MemoryDetails
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.domain.Person
import com.zerotwodev.recall.domain.PhotoPickResult
import com.zerotwodev.recall.domain.PhotoPicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TITLE_SAVE_DELAY_MS = 500L

data class CaptureReviewUiState(
    val isLoading: Boolean = true,
    val details: MemoryDetails? = null,
    val allPeople: List<Person> = emptyList(),
)

/** The memory is already saved. Every change here is saved as it happens. */
class CaptureReviewViewModel(
    private val memoryId: String,
    private val repository: MemoryRepository,
    private val photoPicker: PhotoPicker,
) : ViewModel() {

    val uiState: StateFlow<CaptureReviewUiState> = combine(
        repository.observeDetails(memoryId),
        repository.observePeople(),
    ) { details, people ->
        CaptureReviewUiState(isLoading = false, details = details, allPeople = people)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureReviewUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var pendingTitle: String? = null
    private var pendingBody: String? = null
    private var typingJob: Job? = null
    private var photoJob: Job? = null

    fun onTitleChange(title: String) {
        pendingTitle = title
        saveAfterPause()
    }

    fun onBodyChange(body: String) {
        pendingBody = body
        saveAfterPause()
    }

    private fun saveAfterPause() {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(TITLE_SAVE_DELAY_MS)
            flushTyping()
        }
    }

    /** Saves typing still waiting on the pause, for when the screen closes. */
    fun saveNow() {
        typingJob?.cancel()
        viewModelScope.launch { flushTyping() }
    }

    private suspend fun flushTyping() {
        val title = pendingTitle
        val body = pendingBody
        pendingTitle = null
        pendingBody = null
        withContext(NonCancellable) {
            title?.let { repository.setTitle(memoryId, it) }
            body?.let { repository.setBody(memoryId, it) }
        }
    }

    fun setMood(mood: Mood?) {
        viewModelScope.launch { repository.setMood(memoryId, mood) }
    }

    fun addPerson(name: String) {
        viewModelScope.launch { repository.addPerson(memoryId, name) }
    }

    fun removePerson(personId: String) {
        viewModelScope.launch { repository.removePerson(memoryId, personId) }
    }

    fun setPlace(name: String?) {
        viewModelScope.launch { repository.setPlace(memoryId, name) }
    }

    fun addPhotos() {
        if (photoJob?.isActive == true) return
        photoJob = viewModelScope.launch {
            when (val result = photoPicker.pick()) {
                PhotoPickResult.Cancelled -> Unit
                PhotoPickResult.Failed -> _messages.send("Those photos couldn't be added. Please try again.")
                is PhotoPickResult.Saved -> withContext(NonCancellable) {
                    try {
                        repository.addPhotos(memoryId, result.fileNames)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        result.fileNames.forEach { photoPicker.delete(it) }
                        _messages.trySend("Those photos couldn't be added. Please try again.")
                    }
                }
            }
        }
    }
}

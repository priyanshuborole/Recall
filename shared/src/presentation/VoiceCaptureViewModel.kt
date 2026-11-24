package com.zerotwodev.recall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwodev.recall.domain.MemoryDraft
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.domain.MemoryType
import com.zerotwodev.recall.domain.MicPermission
import com.zerotwodev.recall.domain.SpeechEvent
import com.zerotwodev.recall.domain.SpeechTranscriber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

const val WAVEFORM_BARS = 34
const val MAX_RECORDING_MS = 60_000L
private const val MIN_RECORDING_MS = 1_000L
private const val FINAL_WORDS_TIMEOUT_MS = 4_000L
private const val TICK_MS = 50L

data class VoiceCaptureUiState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0,
    val levels: List<Float> = List(WAVEFORM_BARS) { 0f },
    val transcript: String = "",
    val willCancel: Boolean = false,
)

/** Hold to speak, release to save the words, slide left to cancel. No audio is kept. */
class VoiceCaptureViewModel(
    private val transcriber: SpeechTranscriber,
    private val micPermission: MicPermission,
    private val repository: MemoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceCaptureUiState())
    val state: StateFlow<VoiceCaptureUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private val _savedMemories = Channel<String>(Channel.BUFFERED)

    /** Ids of voice memories just saved, so Home can open their review. */
    val savedMemories: Flow<String> = _savedMemories.receiveAsFlow()

    private var held = false
    private var session: Job? = null

    fun onPress() {
        if (session?.isActive == true) return
        held = true
        session = viewModelScope.launch { listen() }
    }

    fun onSlide(willCancel: Boolean) {
        _state.update { if (it.isRecording) it.copy(willCancel = willCancel) else it }
    }

    /** Also called when the app goes to the background, so what was said is kept. */
    fun onRelease() {
        held = false
    }

    private suspend fun listen() {
        if (!micPermission.isGranted()) {
            held = false
            val granted = micPermission.request()
            _messages.send(
                if (granted) "Your microphone is ready. Hold to speak."
                else "Recall needs the microphone to hear you. You can allow it in your phone's settings."
            )
            return
        }
        if (!transcriber.isAvailable()) {
            held = false
            _messages.send("Voice notes need on-device speech recognition, which this phone doesn't have. You can write this memory instead.")
            return
        }

        var problem: SpeechEvent? = null
        _state.value = VoiceCaptureUiState(isRecording = true)
        val listener = viewModelScope.launch {
            transcriber.listen().collect { event ->
                when (event) {
                    is SpeechEvent.Level -> _state.update { it.copy(levels = it.levels.drop(1) + event.value) }
                    is SpeechEvent.Transcript -> _state.update { it.copy(transcript = event.text) }
                    SpeechEvent.LanguageUnavailable, SpeechEvent.Failed -> problem = event
                }
            }
        }

        try {
            val startedAt = TimeSource.Monotonic.markNow()
            var elapsed = 0L
            while (held && elapsed < MAX_RECORDING_MS && listener.isActive) {
                elapsed = startedAt.elapsedNow().inWholeMilliseconds
                _state.update { it.copy(elapsedMs = elapsed) }
                delay(TICK_MS)
            }
            val cancelled = _state.value.willCancel
            if (cancelled || elapsed < MIN_RECORDING_MS) {
                listener.cancelAndJoin()
            } else {
                transcriber.finish()
                withTimeoutOrNull(FINAL_WORDS_TIMEOUT_MS) { listener.join() }
                listener.cancelAndJoin()
            }
            val text = _state.value.transcript.trim()
            _state.value = VoiceCaptureUiState()
            withContext(NonCancellable) { finish(cancelled, elapsed, text, problem, elapsed >= MAX_RECORDING_MS) }
        } finally {
            listener.cancel()
            _state.value = VoiceCaptureUiState()
        }
    }

    private suspend fun finish(cancelled: Boolean, elapsedMs: Long, text: String, problem: SpeechEvent?, reachedLimit: Boolean) {
        when {
            cancelled -> Unit
            text.isNotEmpty() -> save(text, reachedLimit)
            problem == SpeechEvent.LanguageUnavailable ->
                _messages.trySend("Your phone is getting its speech pack for this language. Try again in a few minutes.")
            problem == SpeechEvent.Failed -> _messages.trySend("Listening stopped. Please try again.")
            elapsedMs < MIN_RECORDING_MS -> _messages.trySend("Hold the mic while you speak.")
            else -> _messages.trySend("I didn't catch any words. Please try again.")
        }
    }

    private suspend fun save(text: String, reachedLimit: Boolean) {
        try {
            val id = repository.create(MemoryDraft(body = text, type = MemoryType.VOICE, transcript = text))
            if (reachedLimit) _messages.trySend("Saved your note at the 1 minute limit.")
            _savedMemories.trySend(id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _messages.trySend("That note couldn't be saved. Please try again.")
        }
    }
}

package com.zerotwodev.recall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwodev.recall.domain.AudioRecorder
import com.zerotwodev.recall.domain.MemoryDraft
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.domain.MemoryType
import com.zerotwodev.recall.domain.MicPermission
import com.zerotwodev.recall.domain.RecordedAudio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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
import kotlin.time.TimeSource

const val WAVEFORM_BARS = 34
const val MAX_RECORDING_MS = 60_000L
private const val MIN_RECORDING_MS = 1_000L
private const val TICK_MS = 50L

data class VoiceCaptureUiState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0,
    val levels: List<Float> = List(WAVEFORM_BARS) { 0f },
    val willCancel: Boolean = false,
)

/** Hold to record, release to save, slide left to cancel. */
class VoiceCaptureViewModel(
    private val recorder: AudioRecorder,
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
        session = viewModelScope.launch { record() }
    }

    fun onSlide(willCancel: Boolean) {
        _state.update { if (it.isRecording) it.copy(willCancel = willCancel) else it }
    }

    /** Also called when the app goes to the background, so what was said is kept. */
    fun onRelease() {
        held = false
    }

    private suspend fun record() {
        if (!micPermission.isGranted()) {
            held = false
            val granted = micPermission.request()
            _messages.send(
                if (granted) "Your microphone is ready. Hold to record."
                else "Recall needs the microphone to record. You can allow it in your phone's settings."
            )
            return
        }
        if (!recorder.start()) {
            held = false
            _messages.send("Recording couldn't start. Please try again.")
            return
        }

        var finished = false
        try {
            val startedAt = TimeSource.Monotonic.markNow()
            var elapsed = 0L
            _state.value = VoiceCaptureUiState(isRecording = true)
            while (held && elapsed < MAX_RECORDING_MS) {
                val level = recorder.level()
                elapsed = startedAt.elapsedNow().inWholeMilliseconds
                _state.update { it.copy(elapsedMs = elapsed, levels = it.levels.drop(1) + level) }
                delay(TICK_MS)
            }
            val cancelled = _state.value.willCancel
            _state.value = VoiceCaptureUiState()
            finished = true
            withContext(NonCancellable) { finish(cancelled, elapsed) }
        } finally {
            if (!finished) {
                _state.value = VoiceCaptureUiState()
                withContext(NonCancellable) { recorder.cancel() }
            }
        }
    }

    private suspend fun finish(cancelled: Boolean, elapsedMs: Long) {
        when {
            cancelled -> recorder.cancel()
            elapsedMs < MIN_RECORDING_MS -> {
                recorder.cancel()
                _messages.trySend("Hold the mic a little longer to record.")
            }
            else -> {
                val audio = recorder.stop()
                if (audio == null) {
                    _messages.trySend("That recording couldn't be saved. Please try again.")
                } else {
                    save(audio, reachedLimit = elapsedMs >= MAX_RECORDING_MS)
                }
            }
        }
    }

    private suspend fun save(audio: RecordedAudio, reachedLimit: Boolean) {
        try {
            val id = repository.createVoice(MemoryDraft(body = "", type = MemoryType.VOICE, title = "Voice note"), audio)
            if (reachedLimit) _messages.trySend("Saved your voice note at the 1 minute limit.")
            _savedMemories.trySend(id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            recorder.delete(audio.fileName)
            _messages.trySend("That recording couldn't be saved. Please try again.")
        }
    }
}

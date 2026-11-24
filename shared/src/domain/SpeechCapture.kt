package com.zerotwodev.recall.domain

import kotlinx.coroutines.flow.Flow

sealed interface SpeechEvent {
    /** Input level from 0 to 1, for the waveform. */
    data class Level(val value: Float) : SpeechEvent

    /** Everything heard so far in this session. */
    data class Transcript(val text: String) : SpeechEvent

    /** The phone's speech pack for this language isn't installed yet. */
    data object LanguageUnavailable : SpeechEvent
    data object Failed : SpeechEvent
}

/** Turns speech into text on the device. No audio is stored or sent anywhere. */
interface SpeechTranscriber {
    fun isAvailable(): Boolean

    /** Listens until [finish] is called or collection is cancelled. */
    fun listen(): Flow<SpeechEvent>

    /** Stops listening and lets the last words arrive before the flow completes. */
    fun finish()
}

interface MicPermission {
    fun isGranted(): Boolean

    /** Shows the system prompt if needed and returns whether the microphone is allowed. */
    suspend fun request(): Boolean
}

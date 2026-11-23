package com.zerotwodev.recall.domain

/** A finished recording, stored by file name inside the app's media folder. */
data class RecordedAudio(val fileName: String, val durationMs: Long)

/** Records AAC `.m4a`, mono, 64 kbps. One recording at a time. */
interface AudioRecorder {
    /** Returns false if the microphone could not be started. */
    suspend fun start(): Boolean

    /** Current input level from 0 to 1, for the waveform. */
    fun level(): Float

    /** Finishes the recording. Returns null, and leaves no file, if nothing usable was captured. */
    suspend fun stop(): RecordedAudio?

    /** Stops and throws the recording away. Safe to call when not recording. */
    suspend fun cancel()

    /** Removes a finished recording that could not be saved. */
    suspend fun delete(fileName: String)
}

interface MicPermission {
    fun isGranted(): Boolean

    /** Shows the system prompt if needed and returns whether the microphone is allowed. */
    suspend fun request(): Boolean
}

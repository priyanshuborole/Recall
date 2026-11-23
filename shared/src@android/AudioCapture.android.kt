package com.zerotwodev.recall

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import com.zerotwodev.recall.domain.AudioRecorder
import com.zerotwodev.recall.domain.MicPermission
import com.zerotwodev.recall.domain.RecordedAudio
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

internal class AndroidAudioRecorder(private val context: Context) : AudioRecorder {
    private val mediaDir = File(context.filesDir, "media")
    private val lock = Mutex()

    @Volatile
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt: TimeSource.Monotonic.ValueTimeMark? = null

    override suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            if (recorder != null) return@withLock false
            mediaDir.mkdirs()
            val output = File(mediaDir, "${Uuid.random()}.m4a")
            val created = newRecorder()
            try {
                created.setAudioSource(MediaRecorder.AudioSource.MIC)
                created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                created.setAudioChannels(1)
                created.setAudioSamplingRate(44_100)
                created.setAudioEncodingBitRate(64_000)
                created.setOutputFile(output.absolutePath)
                created.prepare()
                created.start()
                recorder = created
                file = output
                startedAt = TimeSource.Monotonic.markNow()
                true
            } catch (_: Exception) {
                created.release()
                output.delete()
                false
            }
        }
    }

    override fun level(): Float = try {
        // A square root makes quiet speech visible on the waveform.
        sqrt((recorder?.maxAmplitude ?: 0) / 32_767f).coerceIn(0f, 1f)
    } catch (_: IllegalStateException) {
        0f
    }

    override suspend fun stop(): RecordedAudio? = withContext(Dispatchers.IO) {
        lock.withLock {
            val active = recorder ?: return@withLock null
            val output = file
            val durationMs = startedAt?.elapsedNow()?.inWholeMilliseconds ?: 0
            clear()
            val stopped = try {
                active.stop()
                true
            } catch (_: RuntimeException) {
                false // Android throws here when no audio was captured.
            } finally {
                active.release()
            }
            if (stopped && output != null && output.length() > 0) {
                RecordedAudio(output.name, fileDurationMs(output) ?: durationMs)
            } else {
                output?.delete()
                null
            }
        }
    }

    override suspend fun cancel() = withContext(Dispatchers.IO) {
        lock.withLock {
            val active = recorder ?: return@withLock
            val output = file
            clear()
            try {
                active.stop()
            } catch (_: RuntimeException) {
                // Nothing was captured; the file is deleted below either way.
            } finally {
                active.release()
                output?.delete()
            }
        }
    }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) { File(mediaDir, File(fileName).name).delete() }
    }

    /** The length of the audio actually written, which can be shorter than the time the mic was held. */
    private fun fileDurationMs(output: File): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(output.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: RuntimeException) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun clear() {
        recorder = null
        file = null
        startedAt = null
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}

/** The activity connects its permission launcher here, because only an activity can show the prompt. */
class AndroidMicPermission(private val context: Context) : MicPermission {
    var launcher: (() -> Unit)? = null
    private var pending: CompletableDeferred<Boolean>? = null

    override fun isGranted(): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override suspend fun request(): Boolean {
        if (isGranted()) return true
        val launch = launcher ?: return false
        val result = CompletableDeferred<Boolean>()
        pending = result
        launch()
        return result.await()
    }

    fun onResult(granted: Boolean) {
        pending?.complete(granted)
        pending = null
    }
}

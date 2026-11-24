package com.zerotwodev.recall

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.zerotwodev.recall.domain.MicPermission
import com.zerotwodev.recall.domain.SpeechEvent
import com.zerotwodev.recall.domain.SpeechTranscriber
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.Locale

private const val RESTART_DELAY_MS = 120L

/** Uses only Android's on-device recognizer, so speech never leaves the phone. Needs Android 12+. */
internal class AndroidSpeechTranscriber(private val context: Context) : SpeechTranscriber {
    private var active: SpeechRecognizer? = null
    private var downloader: SpeechRecognizer? = null
    private var finishing = false

    override fun isAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    override fun finish() {
        finishing = true
        active?.stopListening()
    }

    override fun listen(): Flow<SpeechEvent> = callbackFlow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            trySend(SpeechEvent.Failed)
            close()
            return@callbackFlow
        }
        finishing = false
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        active = recognizer
        val intent = listenIntent()
        var committed = ""

        fun joined(next: String) = listOf(committed, next.trim()).filter { it.isNotEmpty() }.joinToString(" ")
        fun restart() {
            launch {
                delay(RESTART_DELAY_MS)
                if (!finishing) recognizer.startListening(intent)
            }
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onRmsChanged(rmsdB: Float) {
                trySend(SpeechEvent.Level(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                bestResult(partialResults)?.let { trySend(SpeechEvent.Transcript(joined(it))) }
            }

            override fun onResults(results: Bundle?) {
                bestResult(results)?.let {
                    committed = joined(it)
                    trySend(SpeechEvent.Transcript(committed))
                }
                // The recognizer stops at each pause, so keep going while the mic is held.
                if (finishing) close() else restart()
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> if (finishing) close() else restart()
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                        downloadLanguagePack(intent)
                        trySend(SpeechEvent.LanguageUnavailable)
                        close()
                    }
                    else -> {
                        trySend(SpeechEvent.Failed)
                        close()
                    }
                }
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        recognizer.startListening(intent)

        awaitClose {
            active = null
            recognizer.destroy()
        }
    }.flowOn(Dispatchers.Main.immediate)

    /** Uses its own recognizer, because destroying the listening one can cancel the download. */
    private fun downloadLanguagePack(intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        // If the person dismissed the system's download prompt, no callback arrives, so start fresh.
        downloader?.destroy()
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        downloader = recognizer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val done = {
                recognizer.destroy()
                if (downloader === recognizer) downloader = null
            }
            recognizer.triggerModelDownload(
                intent,
                context.mainExecutor,
                object : ModelDownloadListener {
                    override fun onProgress(completedPercent: Int) = Unit
                    override fun onSuccess() = done()
                    override fun onScheduled() = done()
                    override fun onError(error: Int) = done()
                },
            )
        } else {
            recognizer.triggerModelDownload(intent)
        }
    }

    private fun listenIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun bestResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }
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

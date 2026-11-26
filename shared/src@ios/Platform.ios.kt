package com.zerotwodev.recall

import androidx.compose.ui.graphics.ImageBitmap
import androidx.room3.Room
import com.zerotwodev.recall.data.KeyValueStore
import com.zerotwodev.recall.data.SettingsRepositoryImpl
import com.zerotwodev.recall.data.local.AppDatabase
import com.zerotwodev.recall.data.local.RoomMemoryRepository
import com.zerotwodev.recall.data.local.buildDatabase
import com.zerotwodev.recall.domain.MicPermission
import com.zerotwodev.recall.domain.NotificationPermission
import com.zerotwodev.recall.domain.ReminderScheduler
import com.zerotwodev.recall.domain.PhotoPickResult
import com.zerotwodev.recall.domain.PhotoPicker
import com.zerotwodev.recall.domain.SpeechEvent
import com.zerotwodev.recall.domain.SpeechTranscriber
import com.zerotwodev.recall.ui.ImageStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import kotlin.time.Clock

/** Called once from the iOS app's ViewController. */
fun createAppContainer(): AppContainer {
    val database = buildDatabase(
        Room.databaseBuilder<AppDatabase>(name = documentDirectory() + "/" + AppDatabase.FILE_NAME)
    )
    val clock = Clock.System
    return AppContainer(
        memoryRepository = RoomMemoryRepository(database, clock),
        settingsRepository = SettingsRepositoryImpl(UserDefaultsStore(NSUserDefaults.standardUserDefaults)),
        speechTranscriber = UnavailableSpeechTranscriber,
        micPermission = UnavailableMicPermission,
        photoPicker = UnavailablePhotoPicker,
        imageStore = UnavailableImageStore,
        reminderScheduler = UnavailableReminderScheduler,
        notificationPermission = UnavailableNotificationPermission,
        clock = clock,
    )
}

private object UnavailablePhotoPicker : PhotoPicker {
    override suspend fun pick(): PhotoPickResult = PhotoPickResult.Failed
    override suspend fun delete(fileName: String) = Unit
}

// UNUserNotificationCenter goes here when iOS work resumes.
private object UnavailableReminderScheduler : ReminderScheduler {
    override fun schedule(minuteOfDay: Int) = Unit
    override fun cancel() = Unit
}

private object UnavailableNotificationPermission : NotificationPermission {
    override fun isGranted() = false
    override suspend fun request() = false
}

private object UnavailableImageStore : ImageStore {
    override suspend fun load(fileName: String, maxSidePx: Int): ImageBitmap? = null
}

// Android comes first. SFSpeechRecognizer goes here when iOS work resumes.
private object UnavailableSpeechTranscriber : SpeechTranscriber {
    override fun isAvailable() = false
    override fun listen(): Flow<SpeechEvent> = emptyFlow()
    override fun finish() = Unit
}

private object UnavailableMicPermission : MicPermission {
    override fun isGranted() = true
    override suspend fun request() = true
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path) { "No documents directory" }
}

private class UserDefaultsStore(private val defaults: NSUserDefaults) : KeyValueStore {
    private fun has(key: String) = defaults.objectForKey(key) != null

    override fun getBoolean(key: String, default: Boolean) =
        if (has(key)) defaults.boolForKey(key) else default

    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, forKey = key)

    override fun getInt(key: String, default: Int) =
        if (has(key)) defaults.integerForKey(key).toInt() else default

    override fun putInt(key: String, value: Int) = defaults.setInteger(value.toLong(), forKey = key)

    override fun getString(key: String, default: String) = defaults.stringForKey(key) ?: default

    override fun putString(key: String, value: String) = defaults.setObject(value, forKey = key)
}

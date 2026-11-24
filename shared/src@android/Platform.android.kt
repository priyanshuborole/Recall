package com.zerotwodev.recall

import android.content.Context
import android.content.SharedPreferences
import androidx.room3.Room
import com.zerotwodev.recall.data.KeyValueStore
import com.zerotwodev.recall.data.SettingsRepositoryImpl
import com.zerotwodev.recall.data.local.AppDatabase
import com.zerotwodev.recall.data.local.RoomMemoryRepository
import com.zerotwodev.recall.data.local.buildDatabase
import kotlin.time.Clock

/** Called once from the Android Application. */
fun createAppContainer(context: Context): AppContainer {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(AppDatabase.FILE_NAME)
    val database = buildDatabase(
        Room.databaseBuilder<AppDatabase>(context = appContext, name = dbFile.absolutePath)
    )
    val clock = Clock.System
    return AppContainer(
        memoryRepository = RoomMemoryRepository(database, clock),
        settingsRepository = SettingsRepositoryImpl(
            SharedPreferencesStore(appContext.getSharedPreferences("recall_settings", Context.MODE_PRIVATE))
        ),
        speechTranscriber = AndroidSpeechTranscriber(appContext),
        micPermission = AndroidMicPermission(appContext),
        photoPicker = AndroidPhotoPicker(appContext),
        imageStore = AndroidImageStore(appContext),
        clock = clock,
    )
}

private class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    override fun getString(key: String, default: String) = prefs.getString(key, default) ?: default
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
}

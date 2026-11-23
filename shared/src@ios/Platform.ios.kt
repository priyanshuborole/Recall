package com.zerotwodev.recall

import androidx.room3.Room
import com.zerotwodev.recall.data.KeyValueStore
import com.zerotwodev.recall.data.SettingsRepositoryImpl
import com.zerotwodev.recall.data.local.AppDatabase
import com.zerotwodev.recall.data.local.RoomMemoryRepository
import com.zerotwodev.recall.data.local.buildDatabase
import kotlinx.cinterop.ExperimentalForeignApi
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
        memoryRepository = RoomMemoryRepository(database.memoryDao(), clock),
        settingsRepository = SettingsRepositoryImpl(UserDefaultsStore(NSUserDefaults.standardUserDefaults)),
        clock = clock,
    )
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

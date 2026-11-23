package com.zerotwodev.recall.data

/**
 * Small persistent key-value storage for settings.
 * Each platform provides one: SharedPreferences (Android) and NSUserDefaults (iOS).
 */
interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
}

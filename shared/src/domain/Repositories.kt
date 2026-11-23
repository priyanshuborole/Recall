package com.zerotwodev.recall.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The only way the UI talks to memory data.
 * Backed by Room on Android and iOS. A sync source can be added
 * behind this interface later without touching any screen.
 */
interface MemoryRepository {
    fun observeRecent(limit: Int): Flow<List<Memory>>
    fun observeTimeline(query: String): Flow<List<Memory>>

    /** Memories from earlier years that happened on today's month and day. Hidden ones are excluded. */
    fun observeOnThisDay(): Flow<List<Memory>>

    fun observeMemory(id: String): Flow<Memory?>

    /** Saves a new memory and returns its id. */
    suspend fun create(draft: MemoryDraft): String
    suspend fun update(memory: Memory)
    suspend fun setHidden(id: String, hidden: Boolean)

    /** Soft delete: the record stays with deletedAt set, so the delete can sync later. */
    suspend fun delete(id: String)
}

/** Colour theme from the Appearance screen. Each has a light and a dark version. */
enum class AppTheme { WARM, FRESH }

/** Light/dark choice from the Appearance screen. SYSTEM follows the device setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val reminderEnabled: Boolean = true,
    /** Minutes after midnight, local time. Default 21:30. */
    val reminderMinuteOfDay: Int = 21 * 60 + 30,
    val appLockEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
    val theme: AppTheme = AppTheme.WARM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

interface SettingsRepository {
    /** A StateFlow so the first frame already uses the saved theme (no flash of the default). */
    val settings: StateFlow<UserSettings>
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setReminderMinuteOfDay(minuteOfDay: Int)
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setOnboardingDone()
    suspend fun setTheme(theme: AppTheme)
    suspend fun setThemeMode(mode: ThemeMode)
}

package com.zerotwodev.recall.data

import com.zerotwodev.recall.domain.AppTheme
import com.zerotwodev.recall.domain.SettingsRepository
import com.zerotwodev.recall.domain.ThemeMode
import com.zerotwodev.recall.domain.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsRepositoryImpl(
    private val store: KeyValueStore,
) : SettingsRepository {

    private val state = MutableStateFlow(read())

    override val settings: StateFlow<UserSettings> = state.asStateFlow()

    override suspend fun setReminderEnabled(enabled: Boolean) {
        store.putBoolean(REMINDER_ENABLED, enabled)
        state.update { it.copy(reminderEnabled = enabled) }
    }

    override suspend fun setReminderMinuteOfDay(minuteOfDay: Int) {
        val value = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        store.putInt(REMINDER_MINUTE, value)
        state.update { it.copy(reminderMinuteOfDay = value) }
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        store.putBoolean(APP_LOCK, enabled)
        state.update { it.copy(appLockEnabled = enabled) }
    }

    override suspend fun setOnboardingDone() {
        store.putBoolean(ONBOARDING_DONE, true)
        state.update { it.copy(onboardingDone = true) }
    }

    override suspend fun setTheme(theme: AppTheme) {
        store.putString(THEME, theme.name)
        state.update { it.copy(theme = theme) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.putString(THEME_MODE, mode.name)
        state.update { it.copy(themeMode = mode) }
    }

    private fun read(): UserSettings {
        val defaults = UserSettings()
        val themeName = store.getString(THEME, defaults.theme.name)
        val modeName = store.getString(THEME_MODE, defaults.themeMode.name)
        return UserSettings(
            reminderEnabled = store.getBoolean(REMINDER_ENABLED, defaults.reminderEnabled),
            reminderMinuteOfDay = store.getInt(REMINDER_MINUTE, defaults.reminderMinuteOfDay),
            appLockEnabled = store.getBoolean(APP_LOCK, defaults.appLockEnabled),
            onboardingDone = store.getBoolean(ONBOARDING_DONE, defaults.onboardingDone),
            // Unknown saved values (e.g. a theme removed in a later version) fall back to the default.
            theme = AppTheme.entries.firstOrNull { it.name == themeName } ?: defaults.theme,
            themeMode = ThemeMode.entries.firstOrNull { it.name == modeName } ?: defaults.themeMode,
        )
    }

    private companion object {
        const val REMINDER_ENABLED = "reminder_enabled"
        const val REMINDER_MINUTE = "reminder_minute_of_day"
        const val APP_LOCK = "app_lock_enabled"
        const val ONBOARDING_DONE = "onboarding_done"
        const val THEME = "theme"
        const val THEME_MODE = "theme_mode"
    }
}

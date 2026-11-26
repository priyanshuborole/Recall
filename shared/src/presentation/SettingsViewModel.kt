package com.zerotwodev.recall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerotwodev.recall.domain.AppTheme
import com.zerotwodev.recall.domain.NotificationPermission
import com.zerotwodev.recall.domain.SettingsRepository
import com.zerotwodev.recall.domain.ThemeMode
import com.zerotwodev.recall.domain.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val notificationPermission: NotificationPermission,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReminderEnabled(enabled)
            if (enabled) notificationPermission.request()
        }
    }

    fun setReminderTime(minuteOfDay: Int) {
        viewModelScope.launch { repository.setReminderMinuteOfDay(minuteOfDay) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAppLockEnabled(enabled) }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }
}

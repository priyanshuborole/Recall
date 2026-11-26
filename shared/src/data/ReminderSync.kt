package com.zerotwodev.recall.data

import com.zerotwodev.recall.domain.ReminderScheduler
import com.zerotwodev.recall.domain.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The minute to remind at, or null when there should be no reminder. */
fun UserSettings.reminderMinuteOrNull(): Int? =
    if (onboardingDone && reminderEnabled) reminderMinuteOfDay else null

/** Keeps the scheduled reminder matching the settings for as long as [this] scope lives. */
fun CoroutineScope.keepReminderInSync(settings: StateFlow<UserSettings>, scheduler: ReminderScheduler): Job = launch {
    settings.map { it.reminderMinuteOrNull() }.distinctUntilChanged().collect { minute ->
        if (minute == null) scheduler.cancel() else scheduler.schedule(minute)
    }
}

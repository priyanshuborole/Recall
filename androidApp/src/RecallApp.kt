package com.zerotwodev.recall

import android.app.Application
import com.zerotwodev.recall.data.keepReminderInSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RecallApp : Application() {
    /** Created once per process; the database and settings live as long as the app. */
    val container: AppContainer by lazy { createAppContainer(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        appScope.keepReminderInSync(container.settingsRepository.settings, container.reminderScheduler)
    }
}

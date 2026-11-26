package com.zerotwodev.recall.domain

/** Schedules the nightly question. Scheduling again replaces the previous time. */
interface ReminderScheduler {
    fun schedule(minuteOfDay: Int)
    fun cancel()
}

interface NotificationPermission {
    fun isGranted(): Boolean

    /** Shows the system prompt if needed and returns whether notifications are allowed. */
    suspend fun request(): Boolean
}

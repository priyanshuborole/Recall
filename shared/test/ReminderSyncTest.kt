package com.zerotwodev.recall

import com.zerotwodev.recall.data.reminderMinuteOrNull
import com.zerotwodev.recall.domain.UserSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderSyncTest {
    @Test
    fun noReminderBeforeOnboarding() = assertNull(UserSettings(onboardingDone = false).reminderMinuteOrNull())

    @Test
    fun noReminderWhenTurnedOff() =
        assertNull(UserSettings(onboardingDone = true, reminderEnabled = false).reminderMinuteOrNull())

    @Test
    fun reminderAtChosenMinute() =
        assertEquals(1290, UserSettings(onboardingDone = true, reminderMinuteOfDay = 1290).reminderMinuteOrNull())
}

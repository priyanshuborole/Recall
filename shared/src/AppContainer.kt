package com.zerotwodev.recall

import com.zerotwodev.recall.domain.AudioRecorder
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.domain.MicPermission
import com.zerotwodev.recall.domain.SettingsRepository
import kotlin.time.Clock

/**
 * Everything the screens need, created once per app launch by each platform's
 * `createAppContainer(...)` (see src@android and src@ios).
 * Plain constructor injection keeps it working on every target without a DI framework.
 */
class AppContainer(
    val memoryRepository: MemoryRepository,
    val settingsRepository: SettingsRepository,
    val audioRecorder: AudioRecorder,
    val micPermission: MicPermission,
    val clock: Clock = Clock.System,
)

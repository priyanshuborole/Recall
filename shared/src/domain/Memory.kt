package com.zerotwodev.recall.domain

import kotlin.time.Instant

/** A single remembered moment. Pure Kotlin: no platform or database types. */
data class Memory(
    val id: String,
    val title: String,
    val body: String,
    val transcript: String?,
    val mood: Mood?,
    val type: MemoryType,
    val occurredAt: Instant,
    val isHidden: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** What the user provides when capturing. Everything else is filled in by the repository. */
data class MemoryDraft(
    val body: String,
    val type: MemoryType,
    val title: String? = null,
    val transcript: String? = null,
    val mood: Mood? = null,
    val occurredAt: Instant? = null,
)

enum class MemoryType { TEXT, VOICE, PHOTO }

enum class Mood(val label: String) {
    HAPPY("Happy"),
    CALM("Calm"),
    PROUD("Proud"),
    GRATEFUL("Grateful"),
    SAD("Sad"),
    ANXIOUS("Anxious"),
}

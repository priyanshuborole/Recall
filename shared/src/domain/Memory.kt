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

data class Person(val id: String, val name: String)

data class Place(val id: String, val name: String)

enum class MediaKind { AUDIO, PHOTO }

/** A file in the app's media folder, referenced by name. */
data class MediaItem(val id: String, val kind: MediaKind, val fileName: String, val durationMs: Long?)

/** A memory with everything attached to it. */
data class MemoryDetails(
    val memory: Memory,
    val media: List<MediaItem>,
    val people: List<Person>,
    val place: Place?,
) {
    val audio: MediaItem? get() = media.firstOrNull { it.kind == MediaKind.AUDIO }
    val photos: List<MediaItem> get() = media.filter { it.kind == MediaKind.PHOTO }
}

enum class MemoryType { TEXT, VOICE, PHOTO }

enum class Mood(val label: String) {
    HAPPY("Happy"),
    CALM("Calm"),
    PROUD("Proud"),
    GRATEFUL("Grateful"),
    SAD("Sad"),
    ANXIOUS("Anxious"),
}

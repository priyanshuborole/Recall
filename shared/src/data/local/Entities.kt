package com.zerotwodev.recall.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Sync-ready rules for every table:
// - UUID string primary keys (never auto-increment)
// - createdAt / updatedAt epoch millis on every row
// - soft deletes via deletedAt
// - media referenced by file name, never by absolute path

@Entity(
    tableName = "memory",
    indices = [Index("occurredAt"), Index("monthDay"), Index("updatedAt")],
)
data class MemoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val transcript: String?,
    val mood: String?,
    val type: String,
    val occurredAt: Long,
    /** "MM-dd" of occurredAt in the user's time zone at capture, for On this day. */
    val monthDay: String,
    val placeId: String?,
    val isHidden: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Entity(tableName = "media", indices = [Index("memoryId")])
data class MediaEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    /** "PHOTO" or "AUDIO". */
    val kind: String,
    /** File name inside app-private storage, e.g. a content hash. */
    val fileName: String,
    val durationMs: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Entity(tableName = "person")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Entity(
    tableName = "memory_person",
    primaryKeys = ["memoryId", "personId"],
    indices = [Index("personId")],
)
data class MemoryPersonEntity(
    val memoryId: String,
    val personId: String,
)

@Entity(tableName = "place")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

/** A thought added later, looking back at a memory. Added in database version 2. */
@Entity(tableName = "reflection", indices = [Index("memoryId")])
data class ReflectionEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

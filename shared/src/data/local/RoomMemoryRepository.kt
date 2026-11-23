package com.zerotwodev.recall.data.local

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.zerotwodev.recall.data.monthDayOf
import com.zerotwodev.recall.data.titleFrom
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.domain.MemoryDraft
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.domain.MemoryType
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.domain.RecordedAudio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RoomMemoryRepository(
    private val database: AppDatabase,
    private val clock: Clock,
) : MemoryRepository {

    private val dao: MemoryDao = database.memoryDao()
    private val mediaDao: MediaDao = database.mediaDao()
    private val timeZone: TimeZone get() = TimeZone.currentSystemDefault()

    override fun observeRecent(limit: Int): Flow<List<Memory>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeTimeline(query: String): Flow<List<Memory>> {
        val trimmed = query.trim()
        val source = if (trimmed.isEmpty()) dao.observeAll() else dao.search(trimmed)
        return source.map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeOnThisDay(): Flow<List<Memory>> =
        // Re-evaluate "today" each time the flow is collected.
        flow { emit(clock.now()) }.flatMapLatest { now ->
            val startOfToday = now.toLocalDateTime(timeZone).date.atStartOfDayIn(timeZone).toEpochMilliseconds()
            dao.observeOnThisDay(monthDayOf(now, timeZone), before = startOfToday)
        }.map { rows -> rows.map { it.toDomain() } }

    override fun observeMemory(id: String): Flow<Memory?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun create(draft: MemoryDraft): String {
        val memory = newEntity(draft)
        dao.save(memory)
        return memory.id
    }

    override suspend fun createVoice(draft: MemoryDraft, audio: RecordedAudio): String =
        createWithMedia(draft, kind = "AUDIO", fileName = audio.fileName, durationMs = audio.durationMs)

    override suspend fun createPhoto(draft: MemoryDraft, fileName: String): String =
        createWithMedia(draft, kind = "PHOTO", fileName = fileName, durationMs = null)

    private suspend fun createWithMedia(draft: MemoryDraft, kind: String, fileName: String, durationMs: Long?): String {
        val memory = newEntity(draft)
        val media = MediaEntity(
            id = Uuid.random().toString(),
            memoryId = memory.id,
            kind = kind,
            fileName = fileName,
            durationMs = durationMs,
            createdAt = memory.createdAt,
            updatedAt = memory.updatedAt,
            deletedAt = null,
        )
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                dao.save(memory)
                mediaDao.save(media)
            }
        }
        return memory.id
    }

    private fun newEntity(draft: MemoryDraft): MemoryEntity {
        val now = clock.now()
        val occurredAt = draft.occurredAt ?: now
        return MemoryEntity(
            id = Uuid.random().toString(),
            title = draft.title?.takeIf { it.isNotBlank() } ?: titleFrom(draft.body),
            body = draft.body.trim(),
            transcript = draft.transcript,
            mood = draft.mood?.name,
            type = draft.type.name,
            occurredAt = occurredAt.toEpochMilliseconds(),
            monthDay = monthDayOf(occurredAt, timeZone),
            placeId = null,
            isHidden = false,
            createdAt = now.toEpochMilliseconds(),
            updatedAt = now.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun update(memory: Memory) {
        val existing = dao.getById(memory.id) ?: return
        dao.save(
            existing.copy(
                title = memory.title,
                body = memory.body,
                transcript = memory.transcript,
                mood = memory.mood?.name,
                occurredAt = memory.occurredAt.toEpochMilliseconds(),
                monthDay = monthDayOf(memory.occurredAt, timeZone),
                isHidden = memory.isHidden,
                updatedAt = clock.now().toEpochMilliseconds(),
            )
        )
    }

    override suspend fun setHidden(id: String, hidden: Boolean) {
        dao.setHidden(id, hidden, clock.now().toEpochMilliseconds())
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id, clock.now().toEpochMilliseconds())
    }
}

private fun MemoryEntity.toDomain(): Memory = Memory(
    id = id,
    title = title,
    body = body,
    transcript = transcript,
    mood = mood?.let { name -> Mood.entries.firstOrNull { it.name == name } },
    type = MemoryType.entries.firstOrNull { it.name == type } ?: MemoryType.TEXT,
    occurredAt = Instant.fromEpochMilliseconds(occurredAt),
    isHidden = isHidden,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

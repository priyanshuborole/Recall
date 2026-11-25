package com.zerotwodev.recall.data.local

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.zerotwodev.recall.data.monthDayOf
import com.zerotwodev.recall.data.titleFrom
import com.zerotwodev.recall.domain.MediaItem
import com.zerotwodev.recall.domain.MediaKind
import com.zerotwodev.recall.domain.Memory
import com.zerotwodev.recall.domain.MemoryDetails
import com.zerotwodev.recall.domain.MemoryDraft
import com.zerotwodev.recall.domain.MemoryRepository
import com.zerotwodev.recall.domain.MemoryType
import com.zerotwodev.recall.domain.Mood
import com.zerotwodev.recall.domain.Person
import com.zerotwodev.recall.domain.Place
import com.zerotwodev.recall.domain.Reflection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val personDao: PersonDao = database.personDao()
    private val memoryPersonDao: MemoryPersonDao = database.memoryPersonDao()
    private val placeDao: PlaceDao = database.placeDao()
    private val reflectionDao: ReflectionDao = database.reflectionDao()
    private val timeZone: TimeZone get() = TimeZone.currentSystemDefault()

    override fun observeRecent(limit: Int): Flow<List<Memory>> = withListExtras(dao.observeRecent(limit))

    override fun observeTimeline(query: String): Flow<List<Memory>> {
        val trimmed = query.trim()
        val source = if (trimmed.isEmpty()) dao.observeAll() else dao.search(trimmed)
        return withListExtras(source)
    }

    /** Adds the cover photo and place that list cards show. */
    private fun withListExtras(rows: Flow<List<MemoryEntity>>): Flow<List<Memory>> =
        combine(rows, mediaDao.observeCovers(), placeDao.observeMemoryPlaces()) { memories, covers, places ->
            val coverBy = covers.associate { it.memoryId to it.fileName }
            val placeBy = places.associate { it.memoryId to it.name }
            memories.map { it.toDomain().copy(coverPhoto = coverBy[it.id], placeName = placeBy[it.id]) }
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

    override suspend fun createPhotos(draft: MemoryDraft, fileNames: List<String>): String {
        val memory = newEntity(draft)
        val media = fileNames.map { newMedia(memory.id, MediaKind.PHOTO, it, null, memory.createdAt) }
        write {
            dao.save(memory)
            media.forEach { mediaDao.save(it) }
        }
        return memory.id
    }

    override fun observeDetails(id: String): Flow<MemoryDetails?> = combine(
        dao.observeById(id),
        mediaDao.observeForMemory(id),
        memoryPersonDao.observePeopleFor(id),
        placeDao.observeForMemory(id),
        reflectionDao.observeFor(id),
    ) { memory, media, people, place, reflections ->
        memory?.let {
            MemoryDetails(
                memory = it.toDomain(),
                media = media.mapNotNull { row -> row.toDomain() },
                people = people.map { row -> Person(row.id, row.name) },
                place = place?.let { row -> Place(row.id, row.name) },
                reflections = reflections.map { row -> Reflection(row.id, row.text, Instant.fromEpochMilliseconds(row.createdAt)) },
            )
        }
    }

    override fun observePeople(): Flow<List<Person>> =
        personDao.observeAll().map { rows -> rows.map { Person(it.id, it.name) } }

    override suspend fun setTitle(id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) dao.setTitle(id, trimmed, clock.now().toEpochMilliseconds())
    }

    override suspend fun setBody(id: String, body: String) {
        dao.setBody(id, body.trim(), clock.now().toEpochMilliseconds())
    }

    override suspend fun setMood(id: String, mood: Mood?) {
        dao.setMood(id, mood?.name, clock.now().toEpochMilliseconds())
    }

    override suspend fun addPhotos(id: String, fileNames: List<String>) {
        val now = clock.now()
        val media = fileNames.map { newMedia(id, MediaKind.PHOTO, it, null, now.toEpochMilliseconds()) }
        write {
            media.forEach { mediaDao.save(it) }
            dao.touch(id, now.toEpochMilliseconds())
        }
    }

    override suspend fun addPerson(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val now = clock.now().toEpochMilliseconds()
        write {
            val person = personDao.findByName(trimmed)
                ?: PersonEntity(Uuid.random().toString(), trimmed, now, now, null).also { personDao.insert(it) }
            memoryPersonDao.link(MemoryPersonEntity(id, person.id))
            dao.touch(id, now)
        }
    }

    override suspend fun removePerson(id: String, personId: String) {
        val now = clock.now().toEpochMilliseconds()
        write {
            memoryPersonDao.unlink(id, personId)
            dao.touch(id, now)
        }
    }

    override suspend fun addReflection(id: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val now = clock.now().toEpochMilliseconds()
        write {
            reflectionDao.insert(ReflectionEntity(Uuid.random().toString(), id, trimmed, now, now, null))
            dao.touch(id, now)
        }
    }

    override suspend fun setPlace(id: String, name: String?) {
        val trimmed = name?.trim()?.takeIf { it.isNotEmpty() }
        val now = clock.now().toEpochMilliseconds()
        write {
            val placeId = trimmed?.let {
                val place = placeDao.findByName(it)
                    ?: PlaceEntity(Uuid.random().toString(), it, null, null, now, now, null).also { new -> placeDao.insert(new) }
                place.id
            }
            dao.setPlace(id, placeId, now)
        }
    }

    private suspend fun write(block: suspend () -> Unit) {
        database.useWriterConnection { connection ->
            connection.immediateTransaction { block() }
        }
    }

    private fun newMedia(memoryId: String, kind: MediaKind, fileName: String, durationMs: Long?, now: Long) = MediaEntity(
        id = Uuid.random().toString(),
        memoryId = memoryId,
        kind = kind.name,
        fileName = fileName,
        durationMs = durationMs,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
    )

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

private fun MediaEntity.toDomain(): MediaItem? =
    MediaKind.entries.firstOrNull { it.name == kind }?.let { MediaItem(id, it, fileName, durationMs) }

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

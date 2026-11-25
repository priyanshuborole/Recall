package com.zerotwodev.recall.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memory WHERE deletedAt IS NULL ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory WHERE deletedAt IS NULL ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memory
        WHERE deletedAt IS NULL
          AND (title LIKE '%' || :query || '%'
               OR body LIKE '%' || :query || '%'
               OR transcript LIKE '%' || :query || '%'
               OR EXISTS (
                   SELECT 1 FROM memory_person
                   JOIN person ON person.id = memory_person.personId
                   WHERE memory_person.memoryId = memory.id
                     AND person.deletedAt IS NULL
                     AND person.name LIKE '%' || :query || '%')
               OR EXISTS (
                   SELECT 1 FROM place
                   WHERE place.id = memory.placeId
                     AND place.deletedAt IS NULL
                     AND place.name LIKE '%' || :query || '%')
               OR EXISTS (
                   SELECT 1 FROM reflection
                   WHERE reflection.memoryId = memory.id
                     AND reflection.deletedAt IS NULL
                     AND reflection.text LIKE '%' || :query || '%'))
        ORDER BY occurredAt DESC
        """
    )
    fun search(query: String): Flow<List<MemoryEntity>>

    @Query(
        """
        SELECT * FROM memory
        WHERE deletedAt IS NULL AND isHidden = 0
          AND monthDay = :monthDay AND occurredAt < :before
        ORDER BY occurredAt DESC
        """
    )
    fun observeOnThisDay(monthDay: String, before: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<MemoryEntity?>

    @Query("SELECT * FROM memory WHERE id = :id")
    suspend fun getById(id: String): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(memory: MemoryEntity)

    @Query("UPDATE memory SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun setTitle(id: String, title: String, now: Long)

    @Query("UPDATE memory SET body = :body, updatedAt = :now WHERE id = :id")
    suspend fun setBody(id: String, body: String, now: Long)

    @Query("UPDATE memory SET mood = :mood, updatedAt = :now WHERE id = :id")
    suspend fun setMood(id: String, mood: String?, now: Long)

    @Query("UPDATE memory SET placeId = :placeId, updatedAt = :now WHERE id = :id")
    suspend fun setPlace(id: String, placeId: String?, now: Long)

    @Query("UPDATE memory SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long)

    @Query("UPDATE memory SET isHidden = :hidden, updatedAt = :now WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean, now: Long)

    @Query("UPDATE memory SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

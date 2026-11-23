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
               OR transcript LIKE '%' || :query || '%')
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

    @Query("UPDATE memory SET isHidden = :hidden, updatedAt = :now WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean, now: Long)

    @Query("UPDATE memory SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

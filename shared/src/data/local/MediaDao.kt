package com.zerotwodev.recall.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(media: MediaEntity)

    @Query("SELECT * FROM media WHERE memoryId = :memoryId AND deletedAt IS NULL ORDER BY createdAt")
    fun observeForMemory(memoryId: String): Flow<List<MediaEntity>>

    /** The first photo of each memory. SQLite returns the row that holds MIN(createdAt). */
    @Query("SELECT memoryId, fileName, MIN(createdAt) AS createdAt FROM media WHERE kind = 'PHOTO' AND deletedAt IS NULL GROUP BY memoryId")
    fun observeCovers(): Flow<List<MemoryCover>>
}

data class MemoryCover(val memoryId: String, val fileName: String, val createdAt: Long)

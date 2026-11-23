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
}

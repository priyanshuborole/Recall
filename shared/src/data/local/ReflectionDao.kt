package com.zerotwodev.recall.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReflectionDao {
    @Insert
    suspend fun insert(reflection: ReflectionEntity)

    @Query("SELECT * FROM reflection WHERE memoryId = :memoryId AND deletedAt IS NULL ORDER BY createdAt")
    fun observeFor(memoryId: String): Flow<List<ReflectionEntity>>

    @Query("UPDATE reflection SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

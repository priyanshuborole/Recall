package com.zerotwodev.recall.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Insert
    suspend fun insert(person: PersonEntity)

    @Query("SELECT * FROM person WHERE name = :name COLLATE NOCASE AND deletedAt IS NULL LIMIT 1")
    suspend fun findByName(name: String): PersonEntity?

    @Query("SELECT * FROM person WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PersonEntity>>
}

@Dao
interface MemoryPersonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: MemoryPersonEntity)

    @Query("DELETE FROM memory_person WHERE memoryId = :memoryId AND personId = :personId")
    suspend fun unlink(memoryId: String, personId: String)

    @Query(
        """
        SELECT person.* FROM person
        JOIN memory_person ON memory_person.personId = person.id
        WHERE memory_person.memoryId = :memoryId AND person.deletedAt IS NULL
        ORDER BY person.name COLLATE NOCASE
        """
    )
    fun observePeopleFor(memoryId: String): Flow<List<PersonEntity>>
}

@Dao
interface PlaceDao {
    @Insert
    suspend fun insert(place: PlaceEntity)

    @Query("SELECT * FROM place WHERE name = :name COLLATE NOCASE AND deletedAt IS NULL LIMIT 1")
    suspend fun findByName(name: String): PlaceEntity?

    @Query(
        """
        SELECT place.* FROM place
        JOIN memory ON memory.placeId = place.id
        WHERE memory.id = :memoryId AND place.deletedAt IS NULL
        """
    )
    fun observeForMemory(memoryId: String): Flow<PlaceEntity?>

    @Query("SELECT memory.id AS memoryId, place.name AS name FROM memory JOIN place ON place.id = memory.placeId WHERE place.deletedAt IS NULL")
    fun observeMemoryPlaces(): Flow<List<MemoryPlace>>
}

data class MemoryPlace(val memoryId: String, val name: String)

package com.zerotwodev.recall.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
// Wildcard import, as in the toolchain's Room example: Dispatchers.IO is a member on the JVM
// but an extension on iOS, and this covers both.
import kotlinx.coroutines.*

@Database(
    entities = [
        MemoryEntity::class,
        MediaEntity::class,
        PersonEntity::class,
        MemoryPersonEntity::class,
        PlaceEntity::class,
        ReflectionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun mediaDao(): MediaDao
    abstract fun personDao(): PersonDao
    abstract fun memoryPersonDao(): MemoryPersonDao
    abstract fun placeDao(): PlaceDao
    abstract fun reflectionDao(): ReflectionDao

    companion object {
        const val FILE_NAME = "recall.db"
    }
}

// Room's KSP processor generates the `actual` for each platform.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** Each platform gives a builder pointing at its own file location; the rest is shared. */
internal fun buildDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .addMigrations(*ALL_MIGRATIONS)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

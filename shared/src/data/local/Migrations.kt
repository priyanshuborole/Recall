package com.zerotwodev.recall.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Version 2 adds the reflection table. The SQL matches schemas/<platform>/.../2.json exactly. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `reflection` (`id` TEXT NOT NULL, `memoryId` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`deletedAt` INTEGER, PRIMARY KEY(`id`))"
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_reflection_memoryId` ON `reflection` (`memoryId`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)

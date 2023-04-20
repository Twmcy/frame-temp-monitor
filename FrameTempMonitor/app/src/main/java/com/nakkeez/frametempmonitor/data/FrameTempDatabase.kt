package com.nakkeez.frametempmonitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Defines a Room database and it's entities and version
 */
@Database(entities = [FrameTempData::class], version = 1)
// Convert timestamp objects to and from the database
@TypeConverters(ZonedDateTimeConverter::class)
abstract class FrameTempDatabase : RoomDatabase() {
    abstract fun frameTempDao(): FrameTempDao

    companion object {
        private const val DB_NAME = "frame_temp_database"

        // Make sure that INSTANCE can be changed by multiple threads at the same time
        @Volatile
        private var INSTANCE: FrameTempDatabase? = null

        // Create INSTANCE if not created yet
        fun getInstance(context: Context): FrameTempDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        // Create and return the database
        private fun buildDatabase(context: Context): FrameTempDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FrameTempDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}

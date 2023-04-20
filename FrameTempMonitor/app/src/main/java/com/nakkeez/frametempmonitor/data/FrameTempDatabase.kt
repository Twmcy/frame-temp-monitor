package com.nakkeez.frametempmonitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FrameTempData::class], version = 1)
abstract class FrameTempDatabase : RoomDatabase() {
    abstract fun frameTempDao(): FrameTempDao

    companion object {
        private const val DB_NAME = "frame_temp_database"
        @Volatile
        private var INSTANCE: FrameTempDatabase? = null

        fun getInstance(context: Context): FrameTempDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): FrameTempDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FrameTempDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}

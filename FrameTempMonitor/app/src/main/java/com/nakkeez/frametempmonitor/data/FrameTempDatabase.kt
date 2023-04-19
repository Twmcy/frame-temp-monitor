package com.nakkeez.frametempmonitor.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FrameTemp::class], version = 1)
abstract class FrameTempDatabase : RoomDatabase() {
    abstract fun frameTempDao(): FrameTempDao
}

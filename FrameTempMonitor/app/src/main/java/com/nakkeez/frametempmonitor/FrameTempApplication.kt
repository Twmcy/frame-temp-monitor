package com.nakkeez.frametempmonitor

import android.app.Application
import androidx.room.Room
import com.nakkeez.frametempmonitor.data.FrameTempDatabase

/**
 * Main application for the FrameTemp Monitor that build the database
 */
class FrameTempApplication : Application() {
    companion object {
        lateinit var database: FrameTempDatabase
    }

    override fun onCreate() {
        super.onCreate()

        // Build the database using Room
        database = Room.databaseBuilder(
            applicationContext,
            FrameTempDatabase::class.java,
            "frame_temp_database"
        ).build()
    }
}

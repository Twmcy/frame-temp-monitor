package com.nakkeez.frametempmonitor

import android.app.Application
import androidx.room.Room
import com.nakkeez.frametempmonitor.data.FrameTempDatabase

/**
 * Custom Application class for initializing the database instance.
 */
class FrameTempApplication : Application() {
    companion object {
        lateinit var database: FrameTempDatabase
    }

    /**
     * Initializes the database instance when the application is starting.
     */
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

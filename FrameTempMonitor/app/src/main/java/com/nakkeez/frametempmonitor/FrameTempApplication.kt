package com.nakkeez.frametempmonitor

import android.app.Application
import androidx.room.Room
import com.nakkeez.frametempmonitor.data.FrameTempDatabase

class FrameTempApplication : Application() {
    companion object {
        lateinit var database: FrameTempDatabase
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(applicationContext, FrameTempDatabase::class.java, "frame_temp_database").build()
    }
}

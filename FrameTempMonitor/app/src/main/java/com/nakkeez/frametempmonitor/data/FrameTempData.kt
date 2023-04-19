package com.nakkeez.frametempmonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frame_temp_data")
data class FrameTempData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val frameRate: Float,
    val batteryTemp: Float,
    val timestamp: Long = System.currentTimeMillis()
)

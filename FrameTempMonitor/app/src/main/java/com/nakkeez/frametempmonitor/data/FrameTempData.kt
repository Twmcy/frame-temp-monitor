package com.nakkeez.frametempmonitor.data

import androidx.room.*
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Represents a data model for storing frame rate and battery temperature
 * data to Room database. The data is stored in a table named "frame_temp_data".
 */
@Entity(tableName = "frame_temp_data")
data class FrameTempData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val frameRate: Float,
    val batteryTemp: Float,
    // Timestamp for when the data was captured. Depends on the user's time zone
    val timestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
)

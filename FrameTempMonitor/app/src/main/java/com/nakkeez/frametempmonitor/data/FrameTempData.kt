package com.nakkeez.frametempmonitor.data

import androidx.room.*
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Entity class representing a data model for storing frame rate, battery
 * temperature and CPU temperature data into a database.
 * The data is stored into a table named "frame_temp_data".
 * @param id Unique identifier for the data (auto-generated).
 * @param frameRate Frame rate information.
 * @param batteryTemp Battery temperature information.
 * @param cpuTemp CPU temperature information.
 * @param timestamp Timestamp for when the data was captured
 */
@Entity(tableName = "frame_temp_data")
data class FrameTempData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val frameRate: Float,
    val batteryTemp: Float,
    val cpuTemp: Float,
    // Timestamp depends on the user's time zone
    val timestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
)

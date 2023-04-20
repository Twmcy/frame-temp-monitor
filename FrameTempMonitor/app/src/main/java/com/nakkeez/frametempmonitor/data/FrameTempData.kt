package com.nakkeez.frametempmonitor.data

import androidx.room.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Entity(tableName = "frame_temp_data")
data class FrameTempData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val frameRate: Float,
    val batteryTemp: Float,
    // val timestamp: Long = System.currentTimeMillis()
    // val timestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    val timestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
)

class ZonedDateTimeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): ZonedDateTime? {
        return value?.let {
            ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(value), java.time.ZoneId.systemDefault())
        }
    }

    @TypeConverter
    fun toTimestamp(value: ZonedDateTime?): Long? {
        return value?.toInstant()?.toEpochMilli()
    }
}

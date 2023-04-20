package com.nakkeez.frametempmonitor.data

import androidx.room.TypeConverter
import java.time.ZonedDateTime

/**
 * The ZonedDateTimeConverter class is a type converter that converts
 * a ZonedDateTime object to a Long timestamp value for storing in the
 * database, and other way around.
 * This is needed because Room database does not support ZonedDateTime format.
 */
class ZonedDateTimeConverter {
    // Converts a Long timestamp value to a ZonedDateTime object.
    @TypeConverter
    fun fromTimestamp(value: Long?): ZonedDateTime? {
        return value?.let {
            ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(value), java.time.ZoneId.systemDefault())
        }
    }

    @TypeConverter
    // Converts a ZonedDateTime object to a Long timestamp value.
    fun toTimestamp(value: ZonedDateTime?): Long? {
        return value?.toInstant()?.toEpochMilli()
    }
}
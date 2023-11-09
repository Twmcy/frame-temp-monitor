package com.nakkeez.frametempmonitor.data

import androidx.room.TypeConverter
import java.time.ZonedDateTime

/**
 * Type converter class for converting a ZonedDateTime object to a Long
 * timestamp value for storing into the database, and other way around.
 * This is needed because Room database does not support ZonedDateTime format.
 */
class ZonedDateTimeConverter {
    /**
     * Converts a Long timestamp value to a ZonedDateTime object.
     * @param value Long timestamp value.
     * @return ZonedDateTime object converted from the timestamp value.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): ZonedDateTime? {
        return value?.let {
            ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(value), java.time.ZoneId.systemDefault())
        }
    }

    /**
     * Converts a ZonedDateTime object to a Long timestamp value.
     * @param value ZonedDateTime object.
     * @return Long timestamp value converted from the ZonedDateTime object.
     */
    @TypeConverter
    fun toTimestamp(value: ZonedDateTime?): Long? {
        return value?.toInstant()?.toEpochMilli()
    }
}
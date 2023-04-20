package com.nakkeez.frametempmonitor.data

import androidx.room.*

/**
 * Defines a Data Access Object for the FrameTempData data class
 */
@Dao
interface FrameTempDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(frameTempData: FrameTempData)

    @Query("SELECT * FROM frame_temp_data")
    fun getAll(): List<FrameTempData>

    @Query("DELETE FROM frame_temp_data")
    fun deleteAll()
}

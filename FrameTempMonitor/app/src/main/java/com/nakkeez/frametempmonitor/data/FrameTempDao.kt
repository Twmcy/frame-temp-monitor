package com.nakkeez.frametempmonitor.data

import androidx.room.*

@Dao
interface FrameTempDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(frameTempData: FrameTempData)

    @Query("SELECT * FROM frame_temp_data")
    fun getAll(): List<FrameTempData>

    @Delete
    fun delete(frameTempData: FrameTempData)

    @Query("DELETE FROM frame_temp_data")
    fun deleteAll()
}

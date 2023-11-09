package com.nakkeez.frametempmonitor.data

import androidx.room.*

/**
 * Data Access Object interface for performing database operations on
 * FrameTempData entities.
 */
@Dao
interface FrameTempDao {
    /**
     * Inserts or replaces a FrameTempData entity in the database.
     * @param frameTempData The FrameTempData object to be inserted or replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(frameTempData: FrameTempData)

    /**
     * Retrieves all FrameTempData entities from the database.
     * @return A list of all FrameTempData entities in the database.
     */
    @Query("SELECT * FROM frame_temp_data")
    fun getAll(): List<FrameTempData>

    /**
     * Deletes all FrameTempData entities from the database.
     */
    @Query("DELETE FROM frame_temp_data")
    fun deleteAll()
}

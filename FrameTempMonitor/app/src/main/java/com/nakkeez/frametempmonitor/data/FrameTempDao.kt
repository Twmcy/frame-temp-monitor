package com.nakkeez.frametempmonitor.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FrameTempDao {
    @Insert
    suspend fun insertFrameTemp(frameTemp: FrameTemp)

    @Delete
    suspend fun deleteFrameTemp(frameTemp: FrameTemp)

    @Query("SELECT * FROM performance")
    fun getFrameTemps(): LiveData<List<FrameTemp>>
}

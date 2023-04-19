package com.nakkeez.frametempmonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "performance")
data class FrameTemp(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val frameRate: Float,
    val batteryTemp: Float
)

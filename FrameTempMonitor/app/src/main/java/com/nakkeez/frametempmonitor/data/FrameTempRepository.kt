package com.nakkeez.frametempmonitor.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FrameTempRepository {
    private val _frameRate = MutableLiveData<Float>()
    val frameRate: LiveData<Float>
        get() = _frameRate

    private val _batteryTemp = MutableLiveData<Float>()
    val batteryTemp: LiveData<Float>
        get() = _batteryTemp

    fun updateFrameRate(fps: Float) {
        _frameRate.value = fps
    }

    fun updateBatteryTemp(temp: Float) {
        _batteryTemp.value = temp
    }
}

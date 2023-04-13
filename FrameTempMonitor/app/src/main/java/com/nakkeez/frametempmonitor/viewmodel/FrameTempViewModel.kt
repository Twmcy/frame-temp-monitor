package com.nakkeez.frametempmonitor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FrameTempViewModel : ViewModel() {

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

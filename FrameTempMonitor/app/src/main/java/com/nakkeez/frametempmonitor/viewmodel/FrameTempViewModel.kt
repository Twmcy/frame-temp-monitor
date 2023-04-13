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

class FrameTempViewModelFactory(private val fps: Float, private val temp: Float) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FrameTempViewModel::class.java)) {
            return FrameTempViewModel().apply {
                updateFrameRate(fps)
                updateBatteryTemp(temp)
            } as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.nakkeez.frametempmonitor.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nakkeez.frametempmonitor.FrameTempApplication
import com.nakkeez.frametempmonitor.service.OverlayService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FrameTempRepository(private val frameTempDatabase: FrameTempDatabase) {
    private val _frameRate = MutableLiveData<Float>()
    val frameRate: LiveData<Float>
        get() = _frameRate

    private val _batteryTemp = MutableLiveData<Float>()
    val batteryTemp: LiveData<Float>
        get() = _batteryTemp

    private var isStoring = false
    private var storageJob: Job? = null
    private val dataBuffer = mutableListOf<FrameTempData>()

    fun updateFrameRate(fps: Float) {
        _frameRate.value = fps

        if (isStoring) {
            dataBuffer.add(
                FrameTempData(
                    frameRate = fps,
                    batteryTemp = _batteryTemp.value ?: 0f
                )
            )
        }
    }

    fun updateBatteryTemp(temp: Float) {
        _batteryTemp.value = temp
    }

    fun startStoringData() {
        if (!isStoring) {
            isStoring = true
            storageJob = GlobalScope.launch {
                while (isStoring) {
                    delay(1000)
                    dataBuffer.forEach {
                        frameTempDatabase.frameTempDao().insert(it)
                    }
                    dataBuffer.clear()
                }
            }
        }
    }

    fun stopStoringData() {
        isStoring = false
        storageJob?.cancel()
    }
}

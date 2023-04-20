package com.nakkeez.frametempmonitor.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

class FrameTempRepository(private val frameTempDatabase: FrameTempDatabase, private val preferenceFrameRate: Boolean, private val preferenceBatteryTemp: Boolean) {

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

        if (isStoring && preferenceFrameRate) {
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

        if ((isStoring && preferenceBatteryTemp && !preferenceFrameRate)) {
            dataBuffer.add(
                FrameTempData(
                    frameRate = _frameRate.value ?: 0f,
                    batteryTemp = temp
                )
            )
        }
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

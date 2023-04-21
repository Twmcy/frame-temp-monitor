package com.nakkeez.frametempmonitor.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Provides access to frame rate and battery temperature data and
 * manages storing data to the database
 */
class FrameTempRepository(
    private val frameTempDatabase: FrameTempDatabase,
    // User preferences for tracking performance data
    private val preferenceFrameRate: Boolean,
    private val preferenceBatteryTemp: Boolean
) {

    private val _frameRate = MutableLiveData<Float>()
    val frameRate: LiveData<Float>
        get() = _frameRate

    private val _batteryTemp = MutableLiveData<Float>()
    val batteryTemp: LiveData<Float>
        get() = _batteryTemp

    private val _cpuTemp = MutableLiveData<Float>()
    val cpuTemp: LiveData<Float>
        get() = _cpuTemp

    // Whether the repository is currently storing data or not
    private var isStoring = false
    // The job that runs the data storage task
    private var storageJob: Job? = null
    // The buffer that holds performance data waiting to be stored
    private val dataBuffer = mutableListOf<FrameTempData>()

    fun updateFrameRate(fps: Float) {
        _frameRate.value = fps

        // Add data to the buffer if user wants to track frame rate
        if (isStoring && preferenceFrameRate) {
            dataBuffer.add(
                FrameTempData(
                    frameRate = fps,
                    batteryTemp = _batteryTemp.value ?: 0f
                )
            )
        }
    }

    fun updateBatteryTemp(tempBattery: Float) {
        _batteryTemp.value = tempBattery

        // Add data to the buffer if user wants to track battery temperature
        // but not frame rate
        if ((isStoring && preferenceBatteryTemp && !preferenceFrameRate)) {
            dataBuffer.add(
                FrameTempData(
                    frameRate = _frameRate.value ?: 0f,
                    batteryTemp = tempBattery
                )
            )
        }
    }

    fun updateCpuTemp(tempCpu: Float) {
        _cpuTemp.value = tempCpu
    }

    fun startStoringData() {
        // If not already storing data, start the storage job
        if (!isStoring) {
            isStoring = true
            storageJob = GlobalScope.launch {
                // Continuously store data while isStoring is true
                while (isStoring) {
                    // Insert all data in the buffer into the database
                    delay(1000)
                    dataBuffer.forEach {
                        frameTempDatabase.frameTempDao().insert(it)
                    }
                    // Clear the buffer after data is stored
                    dataBuffer.clear()
                }
            }
        }
    }

    fun stopStoringData() {
        // Stop storing data and cancel the storage job
        isStoring = false
        storageJob?.cancel()
    }
}

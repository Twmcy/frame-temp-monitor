package com.nakkeez.frametempmonitor.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Repository class for managing the storage and retrieval of performance data
 * related to frame rate, battery temperature, and CPU temperature.
 * @property frameTempDatabase Database instance for storing and retrieving performance data.
 * @property preferenceFrameRate Boolean flag indicating whether to track frame rate.
 * @property preferenceBatteryTemp Boolean flag indicating whether to track battery temperature.
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

    // Flag indicating whether the repository is currently storing data.
    private var isStoring = false
    // The job responsible for running the data storage task
    private var storageJob: Job? = null
    // The buffer that holds performance data waiting to be stored
    private val dataBuffer = mutableListOf<FrameTempData>()

    /**
     * Updates the frame rate information and adds data to the buffer
     * if tracking is enabled.
     * @param fps Frame rate value.
     */
    fun updateFrameRate(fps: Float) {
        _frameRate.value = fps

        // Add data to the buffer if user wants to track frame rate
        if (isStoring && preferenceFrameRate) {
            dataBuffer.add(
                FrameTempData(
                    frameRate = fps,
                    batteryTemp = _batteryTemp.value ?: 0f,
                    cpuTemp = _cpuTemp.value ?: 0f,
                )
            )
        }
    }

    /**
     * Updates the battery temperature information and adds data to the buffer
     * if tracking is enabled.
     * @param tempBattery Battery temperature value.
     */
    fun updateBatteryTemp(tempBattery: Float) {
        _batteryTemp.value = tempBattery

        // Add data to the buffer if user wants to track battery temperature
        // but not frame rate
        if ((isStoring && preferenceBatteryTemp && !preferenceFrameRate)) {
            dataBuffer.add(
                FrameTempData(
                    frameRate = _frameRate.value ?: 0f,
                    batteryTemp = tempBattery,
                    cpuTemp = _cpuTemp.value ?: 0f,
                )
            )
        }
    }

    /**
     * Updates the CPU temperature information and adds data to the buffer
     * if tracking is enabled.
     * @param tempCpu CPU temperature value.
     */
    fun updateCpuTemp(tempCpu: Float) {
        _cpuTemp.value = tempCpu

        // Add data to the buffer if user wants to track only CPU temperature
        if ((isStoring && !preferenceBatteryTemp && !preferenceFrameRate)) {
            dataBuffer.add(
                FrameTempData(
                    frameRate = _frameRate.value ?: 0f,
                    batteryTemp = _batteryTemp.value ?: 0f,
                    cpuTemp = tempCpu
                )
            )
        }
    }

    /**
     * Starts storing performance data in the database in global CoroutineScope.
     */
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

    /**
     * Stops the storing of performance data in the database by canceling
     * the global CoroutineScope.
     */
    fun stopStoringData() {
        isStoring = false
        storageJob?.cancel()
    }
}

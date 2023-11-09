package com.nakkeez.frametempmonitor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nakkeez.frametempmonitor.data.FrameTempRepository

/**
 * ViewModel for managing and providing access to FrameTempData-related data for UI components.
 * @property repository The repository providing data access and manipulation.
 */
class FrameTempViewModel(private val repository: FrameTempRepository) : ViewModel() {

    val frameRate: LiveData<Float>
        get() = repository.frameRate

    val batteryTemp: LiveData<Float>
        get() = repository.batteryTemp

    val cpuTemp: LiveData<Float>
        get() = repository.cpuTemp

    /**
     * Updates the frame rate data in the repository.
     * @param fps The new frame rate value to be updated.
     */
    fun updateFrameRate(fps: Float) {
        repository.updateFrameRate(fps)
    }

    /**
     * Updates the battery temperature data in the repository.
     * @param tempBattery The new battery temperature value to be updated.
     */
    fun updateBatteryTemp(tempBattery: Float) {
        repository.updateBatteryTemp(tempBattery)
    }

    /**
     * Updates the CPU temperature data in the repository.
     * @param tempCpu The new CPU temperature value to be updated.
     */
    fun updateCpuTemp(tempCpu: Float) {
        repository.updateCpuTemp(tempCpu)
    }

    /**
     * Factory class for creating instances of FrameTempViewModel.
     * @property repository The repository providing data access and manipulation.
     */
    class FrameTempViewModelFactory(private val repository: FrameTempRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FrameTempViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FrameTempViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}


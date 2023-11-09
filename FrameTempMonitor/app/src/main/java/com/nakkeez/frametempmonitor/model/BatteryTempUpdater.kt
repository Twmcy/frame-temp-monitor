package com.nakkeez.frametempmonitor.model

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import com.nakkeez.frametempmonitor.viewmodel.FrameTempViewModel
import kotlinx.coroutines.*
import java.lang.Runnable

/**
 * Utility class for tracking device's battery temperature.
 * @property context Application context.
 * @property frameTempRepository Repository for managing performance data storage.
 * @property frameTempViewModel ViewModel for handling UI-related data operations.
 */
class BatteryTempUpdater(
    private val context: Context,
    private val frameTempRepository: FrameTempRepository?,
    private val frameTempViewModel: FrameTempViewModel?
) {
    // Handler for scheduling periodic battery temperature updates
    private lateinit var handler: Handler
    // Runnable task for updating battery temperature at regular intervals
    private lateinit var runnable: Runnable
    // Job for checking and updating battery temperature asynchronously
    private var batteryCheck: Job? = null

    /**
     * Starts updating battery temperature at regular intervals.
     */
    fun startUpdatingBatteryTemperature() {
        // Create a Handler and a Runnable for getting the battery temperature
        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                updateBatteryTemperature()
                handler.postDelayed(this, 1000)
            }
        }
        // Start the Runnable to update the temperature every second
        handler.postDelayed(runnable, 1000)
    }

    /**
     * Stops updating battery temperature.
     */
    fun stopUpdatingBatteryTemperature() {
        // Remove any pending callbacks for the battery temperature Runnable if it was started
        if (::handler.isInitialized && ::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }

    /**
     * Tracks and updates battery temperature asynchronously.
     */
    private fun updateBatteryTemperature() {
        batteryCheck?.cancel() // Cancel any existing job
        // Get the battery temperature from the system
        batteryCheck = CoroutineScope(Dispatchers.IO).launch {
            val batteryIntent = context.applicationContext.registerReceiver(
                null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )
            val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val temperatureInCelsius = temperature / 10f
            // Save the calculated temperature value using the main thread
            withContext(Dispatchers.Main) {
                // Update LiveData of ViewModel/Repository depending if it is
                // MainActivity/OverlayService who is tracking the temperature.
                frameTempViewModel?.updateBatteryTemp(temperatureInCelsius)
                frameTempRepository?.updateBatteryTemp(temperatureInCelsius)
            }
        }
    }
}

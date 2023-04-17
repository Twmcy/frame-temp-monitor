package com.nakkeez.frametempmonitor.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import kotlinx.coroutines.*
import java.lang.Runnable

class BatteryTempUpdater(private val context: Context, private val frameTempRepository: FrameTempRepository) {
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var batteryCheck: Job? = null

    fun startUpdatingBatteryTemperature() {
        // Create a Handler and a Runnable to get battery temperature
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

    fun stopUpdatingBatteryTemperature() {
        handler.removeCallbacks(runnable)
    }

    private fun updateBatteryTemperature() {
        batteryCheck?.cancel() // Cancel any existing job
        // Get the battery temperature from the system
        batteryCheck = CoroutineScope(Dispatchers.IO).launch {
            val batteryIntent = context.applicationContext.registerReceiver(null, IntentFilter(
                Intent.ACTION_BATTERY_CHANGED))
            val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val temperatureInCelsius = temperature / 10f
            // Save the calculated frame rate to the repository using main thread
            withContext(Dispatchers.Main) {
                frameTempRepository.updateBatteryTemp(temperatureInCelsius)
            }
        }
    }
}

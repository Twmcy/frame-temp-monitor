package com.nakkeez.frametempmonitor

import android.content.Intent
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nakkeez.frametempmonitor.data.FrameTempDatabase
import com.nakkeez.frametempmonitor.model.BatteryTempUpdater
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import com.nakkeez.frametempmonitor.model.CpuTemperature
import com.nakkeez.frametempmonitor.model.CpuTemperature.Companion.getCpuTemperature
import com.nakkeez.frametempmonitor.model.FrameRateHandler
import com.nakkeez.frametempmonitor.preferences.SettingsActivity
import com.nakkeez.frametempmonitor.service.OverlayService
import com.nakkeez.frametempmonitor.viewmodel.FrameTempViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * MainActivity that calculates the frame rate and battery temperature and
 * displays them in a TextViews. User can start the OverlayService and navigate
 * to the SettingsActivity or FrameTempDataActivity.
 */
class MainActivity : AppCompatActivity() {
    var isOverlayVisible = false

    private lateinit var batteryTempUpdater: BatteryTempUpdater

    private lateinit var frameRateHandler: FrameRateHandler

    private val cpuTempTimer = Timer()

    private lateinit var frameTempDatabase: FrameTempDatabase
    private lateinit var frameTempRepository: FrameTempRepository
    private lateinit var frameTempViewModel: FrameTempViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the values for user's preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val preferenceFrameRate = sharedPreferences.getBoolean("frame_rate", true)
        val preferenceBatteryTemp = sharedPreferences.getBoolean("battery_temperature", true)
        val preferenceCpuTemp = sharedPreferences.getBoolean("cpu_temperature", true)

        frameTempDatabase = FrameTempDatabase.getInstance(applicationContext)
        frameTempRepository =
            FrameTempRepository(frameTempDatabase, preferenceFrameRate, preferenceBatteryTemp)

        // Initialize the FrameTempViewModel using the ViewModelProvider
        frameTempViewModel = ViewModelProvider(
            this,
            FrameTempViewModel.FrameTempViewModelFactory(frameTempRepository)
        )[FrameTempViewModel::class.java]

        // Set a button for navigating to SettingsActivity
        val fabButton = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        fabButton.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        val dataActivityButton = findViewById<Button>(R.id.dataActivityButton)
        dataActivityButton.setOnClickListener {
            val dataIntent = Intent(this, FrameTempDataActivity::class.java)
            startActivity(dataIntent)
        }

        val overlayButton = findViewById<Button>(R.id.overlayButton)
        overlayButton.setOnClickListener {
            isOverlayVisible = if (isOverlayVisible) {
                val intent = Intent(this, OverlayService::class.java)
                stopService(intent)
                overlayButton.text = getString(R.string.overlay_off)
                false
            } else {
                val intent = Intent(this, OverlayService::class.java)
                startService(intent)
                overlayButton.text = getString(R.string.overlay_on)
                true
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            // Show alert dialog to the user saying a separate permission is needed
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        val fpsTextView = findViewById<TextView>(R.id.fpsTextView)
        val tempTextView = findViewById<TextView>(R.id.tempTextView)

        frameRateHandler = FrameRateHandler(frameTempRepository, null)

        if (preferenceFrameRate) {
            // Start observing the frame rate values from ViewModel
            frameTempViewModel.frameRate.observe(this) { frameRate ->
                val fpsText = getString(R.string.frames_per_second, frameRate)
                fpsTextView.text = fpsText
            }

            try {
                // start the frame rate calculations
                frameRateHandler.startCalculatingFrameRate()
            } catch (_: Exception) {
                Toast.makeText(this, "Failed to start tracking frame rate", Toast.LENGTH_LONG)
                    .show()
            }
        }

        batteryTempUpdater = BatteryTempUpdater(this, frameTempRepository, null)

        if (preferenceBatteryTemp) {
            // Start observing battery temperature values from ViewModel
            frameTempViewModel.batteryTemp.observe(this) { batteryTemp ->
                val tempText = getString(R.string.battery_temp, batteryTemp)
                tempTextView.text = tempText
            }

            try {
                // Start tracking battery temperature
                batteryTempUpdater.startUpdatingBatteryTemperature()
            } catch (_: Exception) {
                Toast.makeText(this, "Failed to start tracking battery temperature", Toast.LENGTH_LONG)
                    .show()
            }
        }

        val cpuTextView: TextView = findViewById(R.id.cpuTextView)

        if (preferenceCpuTemp) {
            // Start observing the CPU temperature values from ViewModel
            frameTempViewModel.cpuTemp.observe(this) { cpuTemp ->
                val cpuText = getString(R.string.cpu_temp, cpuTemp)
                cpuTextView.text = cpuText
            }

            try {
                // Make the timer check CPU temperature every second
                cpuTempTimer.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            val cpuTemperature = getCpuTemperature()
                            if (cpuTemperature != null) {
                                frameTempViewModel.updateCpuTemp(cpuTemperature)
                            } else {
                                cpuTempTimer.cancel() // Stop the timer if temperature not found
                            }
                        }
                    }
                }, 0, 1000)
            } catch (_: Exception) {
                Toast.makeText(this, "Failed to start tracking CPU temperature", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Quit the Thread that calculates frame rates
        frameRateHandler.stopCalculatingFrameRate()

        // Remove any pending callbacks for the battery temperature Runnable
        batteryTempUpdater.stopUpdatingBatteryTemperature()

        // Stop the timer that track CPU temperature data
        cpuTempTimer.cancel()
    }
}
package com.nakkeez.frametempmonitor

import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.view.Choreographer
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nakkeez.frametempmonitor.data.BatteryTempUpdater
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import com.nakkeez.frametempmonitor.model.FrameRateHandler
import com.nakkeez.frametempmonitor.preferences.SettingsActivity
import com.nakkeez.frametempmonitor.service.OverlayService
import com.nakkeez.frametempmonitor.viewmodel.FrameTempViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Main activity that calculates the frame rate and battery temperature.
 */
class MainActivity : AppCompatActivity() {
    var isOverlayVisible = false

    // Variables for getting battery temperature
    private lateinit var batteryTempUpdater: BatteryTempUpdater

    // Variables to use with frame rate calculations
    private var frameCount = 0
    private var lastFrameTime: Long = 0

    private lateinit var frameRateHandler: FrameRateHandler

    // Create an instance of FrameTempRepository
    private val frameTempRepository = FrameTempRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set a button for navigating to SettingsActivity
        val fabButton = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        fabButton.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        val fpsTextView = findViewById<TextView>(R.id.fpsTextView)
        val tempTextView = findViewById<TextView>(R.id.tempTextView)

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

        // Get the value of preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val showFrameRate = sharedPreferences.getBoolean("frame_rate", true)
        val showBatteryTemp = sharedPreferences.getBoolean("battery_temperature", true)

        frameRateHandler = FrameRateHandler(frameTempRepository)

        if (showFrameRate) {
            // Start observing the frame rate values inside repository
            frameTempRepository.frameRate.observe(this) {
                val fpsText = getString(R.string.frames_per_second, it)
                fpsTextView.text = fpsText
            }
            // start the frame rate calculations
            frameRateHandler.startCalculatingFrameRate()
        }

        batteryTempUpdater = BatteryTempUpdater(this, frameTempRepository)

        if (showBatteryTemp) {
            // Start observing battery temperature values inside repository
            frameTempRepository.batteryTemp.observe(this) {
                tempTextView.text = getString(R.string.battery_temp, it)
            }
            // Start tracking battery temperature
            batteryTempUpdater.startUpdatingBatteryTemperature()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Quit the Thread that calculates frame rates
        frameRateHandler.stopCalculatingFrameRate()

        // Remove any pending callbacks for the battery temperature Runnable
        batteryTempUpdater.stopUpdatingBatteryTemperature()
    }
}
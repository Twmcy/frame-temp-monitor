package com.nakkeez.frametempmonitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.Choreographer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nakkeez.frametempmonitor.preferences.SettingsActivity
import com.nakkeez.frametempmonitor.service.OverlayService
import com.nakkeez.frametempmonitor.viewmodel.FrameTempViewModel

class MainActivity : AppCompatActivity() {

    // Variables for getting battery temperature
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    // Variables to save frame rate
    private var frameCount = 0
    private var lastFrameTime: Long = 0

    // Instance of FrameTempViewModel
    private lateinit var viewModel: FrameTempViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[FrameTempViewModel::class.java]

        // Set a button for navigating to SettingsActivity
        val fabButton = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        fabButton.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        val fpsTextView = findViewById<TextView>(R.id.fpsTextView)
        val tempTextView = findViewById<TextView>(R.id.tempTextView)

        // Get the value of preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val showOverlay = sharedPreferences.getBoolean("overlay", true)
        val showFrameRate = sharedPreferences.getBoolean("frame_rate", true)
        val showBatteryTemp = sharedPreferences.getBoolean("battery_temperature", true)

        val overlayButton = findViewById<Button>(R.id.overlayButton)
        overlayButton.setOnClickListener {
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
        }

        if (!Settings.canDrawOverlays(this)) {
            // Show alert dialog to the user saying a separate permission is needed
            // Launch the settings activity if the user prefers
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // Observe the LiveData for the frame rate and update the TextView accordingly
        viewModel.frameRate.observe(this) {
            val fpsText = getString(R.string.frames_per_second, String.format("%.2f", it))
            fpsTextView.text = fpsText
        }

        // Observe the LiveData for the battery temperature and update the TextView accordingly
        viewModel.batteryTemp.observe(this) {
            tempTextView.text = getString(R.string.battery_temp, it)
        }

        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                // Calculate the frame rate
                val currentTime = System.nanoTime()
                val elapsedNanos = currentTime - lastFrameTime
                if (elapsedNanos > 1000000000) { // Update once per second
                    val fps = frameCount * 1e9 / elapsedNanos
                    frameCount = 0
                    lastFrameTime = currentTime

                    // Update the frame rate value in the ViewModel
                    viewModel.updateFrameRate(fps.toFloat())
                }

                // Schedule the next frame
                frameCount++
                Choreographer.getInstance().postFrameCallback(this)
            }
        })

        if (showBatteryTemp) {
            // Create a Handler and a Runnable to update the temperature every second
            handler = Handler()
            runnable = object : Runnable {
                override fun run() {
                    updateBatteryTemperature()
                    handler.postDelayed(this, 1000)
                }
            }
            // Start the Runnable to update the temperature every second
            handler.postDelayed(runnable, 1000)
            tempTextView.visibility = View.VISIBLE
        } else {
            tempTextView.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the Runnable when the Activity is destroyed to prevent memory leaks
        handler.removeCallbacks(runnable)
    }

    private fun updateBatteryTemperature() {
        // Get the battery temperature from the system
        val batteryIntent = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatureInCelsius = temperature / 10f
        viewModel.updateBatteryTemp(temperatureInCelsius)
    }
}
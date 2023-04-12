package com.nakkeez.frametempmonitor

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Choreographer
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nakkeez.frametempmonitor.preferences.SettingsActivity

class MainActivity : AppCompatActivity() {

    // Variables for getting battery temperature
    private lateinit var tempTextView: TextView
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    // Variables to save frame rate
    private var frameCount = 0
    private var lastFrameTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set a button for navigating to SettingsActivity
        val fabButton = findViewById<FloatingActionButton>(R.id.floatingActionButton)
        fabButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
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

                    // Display the frame rate
                    val fpsTextView = findViewById<TextView>(R.id.fpsTextView)
                    // fpsTextView.text = "Frames per Second: ${String.format("%.2f", fps)}"
                    val fpsText = getString(R.string.frames_per_second, String.format("%.2f", fps))
                    fpsTextView.text = fpsText
                }

                // Schedule the next frame
                frameCount++
                Choreographer.getInstance().postFrameCallback(this)
            }
        })

        tempTextView = findViewById(R.id.tempTextView)

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
        tempTextView.text = getString(R.string.battery_temp, temperatureInCelsius)
    }
}
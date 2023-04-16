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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nakkeez.frametempmonitor.data.FrameTempRepository
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
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var batteryCheck: Job? = null

    // Variables to use with frame rate calculations
    private var frameCount = 0
    private var lastFrameTime: Long = 0
    private lateinit var fpsHandlerThread: HandlerThread
    private lateinit var fpsHandler: Handler

    // Instance of FrameTempViewModel
    private lateinit var viewModel: FrameTempViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, FrameTempViewModel.FrameTempViewModelFactory(FrameTempRepository()))[FrameTempViewModel::class.java]

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

        // Observe the LiveData for the frame rate and update the TextView accordingly
        viewModel.frameRate.observe(this) {
            val fpsText = getString(R.string.frames_per_second, String.format("%.2f", it))
            fpsTextView.text = fpsText
        }

        // Observe the LiveData for the battery temperature and update the TextView accordingly
        viewModel.batteryTemp.observe(this) {
            tempTextView.text = getString(R.string.battery_temp, it)
        }

        if (showFrameRate) {
            // Make a separate Thread for running the frame rate calculations
            fpsHandlerThread = HandlerThread("FPSHandlerThread")
            fpsHandlerThread.start()

            fpsHandler = Handler(fpsHandlerThread.looper)

            startFpsCalculation()
        }

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
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Quit the Thread that calculates frame rates
        fpsHandlerThread.quit()
        // Remove any pending callbacks for the battery temperature Runnable
        handler.removeCallbacks(runnable)
        // Cancel the coroutine job of battery temperature
        batteryCheck?.cancel()
    }

    private fun startFpsCalculation() {
        fpsHandler.post {
            Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    // Calculate the frame rate
                    val currentTime = System.nanoTime()
                    val elapsedNanos = currentTime - lastFrameTime
                    if (elapsedNanos > 1000000000) { // Update once per second
                        val fps = frameCount * 1e9 / elapsedNanos
                        frameCount = 0
                        lastFrameTime = currentTime

                        // Save the calculated frame rate to the repository using main thread
                        val handler = Handler(Looper.getMainLooper())

                        handler.post {
                            viewModel.updateFrameRate(fps.toFloat())
                        }
                    }

                    // Schedule the next frame
                    frameCount++
                    Choreographer.getInstance().postFrameCallback(this)
                }
            })
        }
    }
    private fun updateBatteryTemperature() {
        lifecycleScope.launch {
            // Get the battery temperature from the system
            val batteryIntent = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val temperatureInCelsius = temperature / 10f
            viewModel.updateBatteryTemp(temperatureInCelsius)
        }
    }
}
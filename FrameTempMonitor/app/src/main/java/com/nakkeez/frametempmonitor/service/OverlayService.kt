package com.nakkeez.frametempmonitor.service

import androidx.lifecycle.LifecycleService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.nakkeez.frametempmonitor.MainActivity
import com.nakkeez.frametempmonitor.R
import com.nakkeez.frametempmonitor.data.FrameTempDatabase
import com.nakkeez.frametempmonitor.model.BatteryTempUpdater
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import com.nakkeez.frametempmonitor.model.CpuTemperature
import com.nakkeez.frametempmonitor.model.FrameRateHandler
import java.util.*

/**
 * LifecycleService for displaying an overlay on the foreground with frame rate and
 * battery temperature data. Has a button for storing captured performance data
 */
class OverlayService : LifecycleService(), View.OnTouchListener {

    // Create variables for showing the overlay
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var initialX: Int = 0
    private var initialY: Int = 0

    private lateinit var saveDataButton: Button

    // Variable to track is Service is storing data
    private var isStoring = false

    private lateinit var batteryTempUpdater: BatteryTempUpdater

    private lateinit var frameRateHandler: FrameRateHandler

    private val cpuHandler = Handler(Looper.getMainLooper())

    private val cpuTempTimer = Timer()

    private lateinit var frameTempDatabase: FrameTempDatabase
    private lateinit var frameTempRepository: FrameTempRepository

    override fun onCreate() {
        super.onCreate()

        // Get the value of preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val preferenceFrameRate = sharedPreferences.getBoolean("frame_rate", true)
        val preferenceBatteryTemp = sharedPreferences.getBoolean("battery_temperature", true)
        val preferenceCpuTemp = sharedPreferences.getBoolean("cpu_temperature", true)
        val preferenceSaveButton = sharedPreferences.getBoolean("save_button", true)
        val preferenceFontSize = sharedPreferences.getString("font_size", "Medium")

        frameTempDatabase = FrameTempDatabase.getInstance(applicationContext)
        frameTempRepository =
            FrameTempRepository(frameTempDatabase, preferenceFrameRate, preferenceBatteryTemp)

        // Create a new view and set its layout parameters
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#D9D3D3D3")) // Set a semi-transparent light grey color
            setOnTouchListener(this@OverlayService) // Set the touch listener

            setPadding(10, 10, 10, 10)
        }

        val dataTextView = TextView(this).apply {
            text = getString(R.string.loading) // Set initial text
            textSize = when (preferenceFontSize) {
                "Small" -> {
                    16f
                }
                "Big" -> {
                    20f
                }
                else -> {
                    18f
                }
            }
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        (overlayView as LinearLayout).addView(dataTextView)

        saveDataButton = Button(this).apply {
            text = getString(R.string.saving_off)

            setOnClickListener {
                saveData(preferenceFrameRate, preferenceBatteryTemp, preferenceCpuTemp)
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 0) // Set margins to 0
                setPadding(15, 0, 15, 0) // Set padding to 0
            }
        }
        if (preferenceSaveButton) {
            (overlayView as LinearLayout).addView(saveDataButton)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // Get the window manager and add the view to the window
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        if (preferenceFrameRate) {
            // Observe the frameRate and update the overlay to display it
            val frameRateObserver = Observer<Float> { frameRate ->
                val batteryTemp = frameTempRepository.batteryTemp.value ?: 0.0f
                val cpuTemp = frameTempRepository.cpuTemp.value ?: 0.0f

                val overlayText = if (preferenceBatteryTemp && preferenceCpuTemp) {
                    "$frameRate fps\nBAT: $batteryTemp °C\nCPU: $cpuTemp °C"
                } else if (preferenceBatteryTemp) {
                    "$frameRate fps\nBAT: $batteryTemp °C"
                } else if (preferenceCpuTemp) {
                    "$frameRate fps\nCPU: $cpuTemp °C"
                } else {
                    "$frameRate fps"
                }

                dataTextView.text = overlayText
            }
            frameTempRepository.frameRate.observe(this, frameRateObserver)
        }

        if (preferenceBatteryTemp) {
            // Observe the battery temperature and update the overlay to display it
            val batteryTempObserver = Observer<Float> { batteryTemp ->
                val frameRate = frameTempRepository.frameRate.value ?: 0.0f
                val cpuTemp = frameTempRepository.cpuTemp.value ?: 0.0f

                val overlayText = if (preferenceFrameRate && preferenceCpuTemp) {
                    "$frameRate fps\nBAT: $batteryTemp °C\nCPU: $cpuTemp °C"
                } else if (preferenceFrameRate) {
                    "$frameRate fps\nBAT: $batteryTemp °C"
                } else if (preferenceCpuTemp) {
                    "BAT: $batteryTemp °C\nCPU: $cpuTemp °C"
                } else {
                    "BAT: $batteryTemp °C"
                }

                dataTextView.text = overlayText
            }
            frameTempRepository.batteryTemp.observe(this, batteryTempObserver)
        }

        if (preferenceCpuTemp) {
            // Observe the CPU temperature and update the overlay to display it
            val cpuTempObserver = Observer<Float> { cpuTemp ->
                val frameRate = frameTempRepository.frameRate.value ?: 0.0f
                val batteryTemp = frameTempRepository.batteryTemp.value ?: 0.0f

                val overlayText = if (preferenceFrameRate && preferenceBatteryTemp) {
                    "$frameRate fps\nBAT: $batteryTemp °C\nCPU: $cpuTemp °C"
                } else if (preferenceFrameRate) {
                    "$frameRate fps\nCPU: $cpuTemp °C"
                } else if (preferenceBatteryTemp) {
                    "BAT: $batteryTemp °C\nCPU: $cpuTemp °C"
                } else {
                    "CPU: $cpuTemp °C"
                }

                dataTextView.text = overlayText
            }
            frameTempRepository.cpuTemp.observe(this, cpuTempObserver)
        }

        // Make a separate Thread for running the frame rate calculations
        frameRateHandler = FrameRateHandler(frameTempRepository, null)

        if (preferenceFrameRate) {
            try {
                // Start the frame rate calculations
                frameRateHandler.startCalculatingFrameRate()
            } catch (_: Exception) {
                Toast.makeText(this, "Failed to start tracking frame rate", Toast.LENGTH_LONG)
                    .show()
            }
        }

        batteryTempUpdater = BatteryTempUpdater(this, frameTempRepository, null)

        if (preferenceBatteryTemp) {
            try {
                // Start tracking battery temperature
                batteryTempUpdater.startUpdatingBatteryTemperature()
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "Failed to start tracking battery temperature",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

        if (preferenceCpuTemp) {
            try {
                // Make the timer check CPU temperature every second
                cpuTempTimer.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        cpuHandler.post {
                            val cpuTemperature = CpuTemperature.getCpuTemperature()
                            if (cpuTemperature != null) {
                                frameTempRepository.updateCpuTemp(cpuTemperature)
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

        try {
            // Remove the view from the window
            windowManager.removeView(overlayView)

            // Set isOverlayVisible to false
            (applicationContext as MainActivity).isOverlayVisible = false
        } catch (_: Exception) {
        }

        try {
            // Quit the Thread that calculates frame rates
            frameRateHandler.stopCalculatingFrameRate()
        } catch (_: Exception) {
        }

        try {
            // Remove any pending callbacks for the battery temperature Runnable
            batteryTempUpdater.stopUpdatingBatteryTemperature()
        } catch (_: Exception) {
        }

        try {
            // Stop the timer that track CPU temperature data
            cpuTempTimer.cancel()
        } catch (_: Exception) {
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Save the initial touch point
                initialX = event.rawX.toInt()
                initialY = event.rawY.toInt()

                // Set the view's alpha to indicate that it is being touched
                view.alpha = 0.6f
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Check if the touch event was a click
                if (event.eventTime - event.downTime < ViewConfiguration.getTapTimeout()) {
                    view.performClick()
                }

                // Reset the view's alpha to indicate that it is no longer being touched
                view.alpha = 1.0f

                // Get the screen height
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenHeight = displayMetrics.heightPixels

                // Check if the view has moved out of the bottom of the screen
                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                if (layoutParams.y + view.height > screenHeight) {
                    // Remove the view from the window if it has moved out of the bottom of the screen
                    windowManager.removeView(view)
                    // Stop the Service after the overlay has been removed
                    stopSelf()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Calculate the new position of the view
                val dx = event.rawX - initialX
                val dy = event.rawY - initialY

                // Update the view's layout parameters with the new position
                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                layoutParams.x += dx.toInt()
                layoutParams.y += dy.toInt()
                windowManager.updateViewLayout(view, layoutParams)

                // Update the initial touch point
                initialX = event.rawX.toInt()
                initialY = event.rawY.toInt()
                return true
            }
        }

        return false
    }

    private fun saveData(
        preferenceFrameRate: Boolean,
        preferenceBatteryTemp: Boolean,
        preferenceCpuTemp: Boolean
    ) {
        if (!preferenceFrameRate && !preferenceBatteryTemp && !preferenceCpuTemp) {
            Toast.makeText(
                this,
                "Enable frame rate or temperature tracking from settings to save data",
                Toast.LENGTH_LONG
            ).show()
        } else {
            if (!isStoring) {
                try {
                    frameTempRepository.startStoringData()
                    saveDataButton.text = getString(R.string.saving_on)
                    Toast.makeText(this, "Started saving the performance data", Toast.LENGTH_LONG)
                        .show()
                    isStoring = true
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Could not start saving performance data",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                try {
                    frameTempRepository.stopStoringData()
                    saveDataButton.text = getString(R.string.saving_off)
                    Toast.makeText(this, "Stopped saving the performance data", Toast.LENGTH_LONG)
                        .show()
                    isStoring = false
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Could not stop saving performance data",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}


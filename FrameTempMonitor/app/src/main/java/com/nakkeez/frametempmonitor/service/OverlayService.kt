package com.nakkeez.frametempmonitor.service

import androidx.lifecycle.LifecycleService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.DisplayMetrics
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
import com.nakkeez.frametempmonitor.model.FrameRateHandler

/**
 * LifecycleService for displaying an overlay on the foreground with frame rate and
 * battery temperature.
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

    // Create variables for getting battery temperature
    private lateinit var batteryTempUpdater: BatteryTempUpdater

    // Create variables to use with frame rate calculations
    private lateinit var frameRateHandler: FrameRateHandler

    // Create an instance of FrameTempRepository
    private lateinit var frameTempDatabase: FrameTempDatabase
    private lateinit var frameTempRepository: FrameTempRepository

    override fun onCreate() {
        super.onCreate()

        // Get the value of preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val preferenceFrameRate = sharedPreferences.getBoolean("frame_rate", true)
        val preferenceBatteryTemp = sharedPreferences.getBoolean("battery_temperature", true)
        val preferenceFontSize = sharedPreferences.getString("font_size", "Medium")

        frameTempDatabase = FrameTempDatabase.getInstance(applicationContext)
        frameTempRepository = FrameTempRepository(frameTempDatabase, preferenceFrameRate, preferenceBatteryTemp)

        // Create a new view and set its layout parameters
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#D9D3D3D3")) // (maybe E6 tai CC?) set a semi-transparent light grey color
            setOnTouchListener(this@OverlayService) // Set the touch listener

            setPadding(10, 10, 10,10 )
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
        }

        (overlayView as LinearLayout).addView(dataTextView)

        saveDataButton = Button(this).apply {
            text = getString(R.string.saving_off)

            setOnClickListener {
                saveData(preferenceFrameRate, preferenceBatteryTemp)
            }

            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 0) // Set margins to 0
                setPadding(15, 0, 15, 0) // Set padding to 0
            }
        }
        (overlayView as LinearLayout).addView(saveDataButton)

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

                val overlayText = if (preferenceBatteryTemp) {
                    "$frameRate fps\n$batteryTemp °C"
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

                val overlayText = if (preferenceFrameRate) {
                    "$frameRate fps\n$batteryTemp °C"
                } else {
                    "$batteryTemp °C"
                }

                dataTextView.text = overlayText
            }
            frameTempRepository.batteryTemp.observe(this, batteryTempObserver)
        }

        // Make a separate Thread for running the frame rate calculations
        frameRateHandler = FrameRateHandler(frameTempRepository, null)

        if (preferenceFrameRate) {
            // start the frame rate calculations
            frameRateHandler.startCalculatingFrameRate()
        }

        batteryTempUpdater = BatteryTempUpdater(this, frameTempRepository, null)

        if (preferenceBatteryTemp) {
            // Start tracking battery temperature
            batteryTempUpdater.startUpdatingBatteryTemperature()
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

        // Quit the Thread that calculates frame rates
        frameRateHandler.stopCalculatingFrameRate()
        // Remove any pending callbacks for the battery temperature Runnable
        batteryTempUpdater.stopUpdatingBatteryTemperature()
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

    private fun saveData(preferenceFrameRate: Boolean, preferenceBatteryTemp: Boolean) {
        if (!preferenceFrameRate && !preferenceBatteryTemp) {
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


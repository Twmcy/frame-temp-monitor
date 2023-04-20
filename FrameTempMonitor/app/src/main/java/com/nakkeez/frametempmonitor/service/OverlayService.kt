package com.nakkeez.frametempmonitor.service

import androidx.lifecycle.LifecycleService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.*
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.nakkeez.frametempmonitor.MainActivity
import com.nakkeez.frametempmonitor.R
import com.nakkeez.frametempmonitor.data.FrameTempDatabase
import com.nakkeez.frametempmonitor.model.BatteryTempUpdater
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import com.nakkeez.frametempmonitor.model.FrameRateHandler

/**
 * Service for displaying an overlay on the foreground with frame rate and
 * battery temperature.
 */
class OverlayService : LifecycleService(), View.OnTouchListener {

    // Create variables for showing the overlay
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var initialX: Int = 0
    private var initialY: Int = 0

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
        val showFrameRate = sharedPreferences.getBoolean("frame_rate", true)
        val showBatteryTemp = sharedPreferences.getBoolean("battery_temperature", true)

        frameTempDatabase = FrameTempDatabase.getInstance(applicationContext)
        frameTempRepository = FrameTempRepository(frameTempDatabase, showFrameRate, showBatteryTemp)

        // Create a new view and set its layout parameters
        overlayView = TextView(this).apply {
            text = getString(R.string.loading) // Set initial text
            textSize = 20f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#D9D3D3D3")) // (maybe E6 tai CC?) set a semi-transparent light grey color
            setOnTouchListener(this@OverlayService) // Set the touch listener
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

        if (showFrameRate) {
            // Observe the frameRate and update the overlay to display it
            val frameRateObserver = Observer<Float> { frameRate ->
                val batteryTemp = frameTempRepository.batteryTemp.value ?: 0.0f

                val overlayText = if (showBatteryTemp) {
                    "$frameRate fps\n$batteryTemp °C"
                } else {
                    "$frameRate fps"
                }

                (overlayView as TextView).text = overlayText
            }
            frameTempRepository.frameRate.observe(this, frameRateObserver)
        }

        if (showBatteryTemp) {
            // Observe the battery temperature and update the overlay to display it
            val batteryTempObserver = Observer<Float> { batteryTemp ->
                val frameRate = frameTempRepository.frameRate.value ?: 0.0f

                val overlayText = if (showFrameRate) {
                    "$frameRate fps\n$batteryTemp °C"
                } else {
                    "$batteryTemp °C"
                }

                (overlayView as TextView).text = overlayText
            }
            frameTempRepository.batteryTemp.observe(this, batteryTempObserver)
        }

        // Make a separate Thread for running the frame rate calculations
        frameRateHandler = FrameRateHandler(frameTempRepository)

        if (showFrameRate) {
            // start the frame rate calculations
            frameRateHandler.startCalculatingFrameRate()
        }

        batteryTempUpdater = BatteryTempUpdater(this, frameTempRepository)

        if (showBatteryTemp) {
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
        } catch (_: Exception) {}

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
}

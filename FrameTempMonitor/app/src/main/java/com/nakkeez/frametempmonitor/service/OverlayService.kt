package com.nakkeez.frametempmonitor.service

import androidx.lifecycle.LifecycleService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.DisplayMetrics
import android.view.*
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.nakkeez.frametempmonitor.MainActivity
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Service for displaying an overlay with frame rate and battery
 * temperature. It's a LifecycleService instead of Service
 * so the overlay can be ViewModelStoreOwner use LiveData.
 */
class OverlayService : LifecycleService(), View.OnTouchListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    // Variables for getting battery temperature
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var batteryCheck: Job? = null

    private var initialX: Int = 0
    private var initialY: Int = 0

    // Variables to save frame rate
    private var frameCount = 0
    private var lastFrameTime: Long = 0

    private lateinit var fpsHandlerThread: HandlerThread
    private lateinit var fpsHandler: Handler

    // Create an instance of FrameTempRepository
    private val frameTempRepository = FrameTempRepository()

    override fun onCreate() {
        super.onCreate()

        // Create a new view and set its layout parameters
        overlayView = TextView(this).apply {
            text = "test" // Set initial text
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

        fpsHandlerThread = HandlerThread("FPSHandlerThread")
        fpsHandlerThread.start()

        fpsHandler = Handler(fpsHandlerThread.looper)

        startFpsCalculation()

        // Observe the frameRate and batteryTemp from repository and
        // update the overlay to display them
        val frameRateObserver = Observer<Float> { frameRate ->
            val batteryTemp = frameTempRepository.batteryTemp.value ?: 0.0f
            (overlayView as TextView).text = "Frame Rate: $frameRate fps\nBattery Temperature: $batteryTemp °C"
        }
        frameTempRepository.frameRate.observe(this, frameRateObserver)

        val batteryTempObserver = Observer<Float> { batteryTemp ->
            val frameRate = frameTempRepository.frameRate.value ?: 0.0f
            (overlayView as TextView).text = "Frame Rate: $frameRate fps\nBattery Temperature: $batteryTemp °C"
        }
        frameTempRepository.batteryTemp.observe(this, batteryTempObserver)

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

        try {
            // Remove the view from the window
            windowManager.removeView(overlayView)

            // Set isOverlayVisible to false
            (applicationContext as MainActivity).isOverlayVisible = false
        } catch (_: Exception) {}

        // Quit the Thread that calculates frame rates
        fpsHandlerThread.quit()
        // Remove any pending callbacks for the battery temperature Runnable
        handler.removeCallbacks(runnable)
        // Cancel the coroutine job of battery temperature
        batteryCheck?.cancel()
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
                            frameTempRepository.updateFrameRate(fps.toFloat())
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
        batteryCheck?.cancel() // Cancel any existing job

        batteryCheck = lifecycleScope.launch {
            // Get the battery temperature from the system
            val batteryIntent = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val temperatureInCelsius = temperature / 10f
            // Save the calculated frame rate to the repository
            frameTempRepository.updateBatteryTemp(temperatureInCelsius)
        }
    }
}

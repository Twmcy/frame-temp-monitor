package com.nakkeez.frametempmonitor.service

import androidx.lifecycle.LifecycleService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.*
import android.widget.TextView
import com.nakkeez.frametempmonitor.MainActivity
import com.nakkeez.frametempmonitor.viewmodel.FrameTempViewModel

/**
 * Service for displaying an overlay with frame rate and battery
 * temperature. It's a LifecycleService instead of Service
 * so the overlay can be ViewModelStoreOwner use LiveData.
 */
class OverlayService : LifecycleService(), View.OnTouchListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    private var initialX: Int = 0
    private var initialY: Int = 0

    // Instance of FrameTempViewModel
    private lateinit var viewModel: FrameTempViewModel

    override fun onCreate() {
        super.onCreate()

        // Create a new view and set its layout parameters
        overlayView = TextView(this).apply {
            text = "Frame Rate: 0\nBattery Temp: 0" // Set initial text
            textSize = 24f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.LTGRAY)
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
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            // Remove the view from the window
            windowManager.removeView(overlayView)

            // Set isOverlayVisible to false
            (applicationContext as MainActivity).isOverlayVisible = false
        } catch (_: Exception) {}
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
}

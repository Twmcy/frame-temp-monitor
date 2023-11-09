package com.nakkeez.frametempmonitor.model

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Choreographer
import androidx.lifecycle.ViewModelProvider
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import com.nakkeez.frametempmonitor.viewmodel.FrameTempViewModel

/**
 * Utility class for calculating and tracking the frame rate the
 * device's display is running on.
 * @property frameTempRepository Repository for managing performance data storage.
 * @property frameTempViewModel ViewModel for handling UI-related data operations.
 */
class FrameRateHandler(
    private val frameTempRepository: FrameTempRepository?,
    private val frameTempViewModel: FrameTempViewModel?
) {
    // HandlerThread for handling frame rate calculations in a separate thread
    private val fpsHandlerThread = HandlerThread("FPSHandlerThread")
    // Handler for updating frame rate calculations
    private lateinit var fpsHandler: Handler
    // Handler for updating UI elements on the main thread
    private val uiHandler = Handler(Looper.getMainLooper())

    // Counter for tracking the number of frames processed
    private var frameCount = 0
    // Timestamp of the last processed frame
    private var lastFrameTime = System.nanoTime()

    /**
     * Starts calculating and updating frame rate using a separate HandlerThread.
     */
    fun startCalculatingFrameRate() {
        fpsHandlerThread.start()

        fpsHandler = Handler(fpsHandlerThread.looper)

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

                        val fpsRounded = String.format("%.1f", fps)

                        // Replace commas with dots, because finnish phones etc. use commas
                        val fpsReplaced = fpsRounded.replace(",", ".")

                        // Save the calculated frame rate value using main thread
                        uiHandler.post {
                            // Update LiveData of ViewModel/Repository depending if it is
                            // MainActivity/OverlayService who is tracking the frame rate
                            frameTempViewModel?.updateFrameRate(fpsReplaced.toFloat())
                            frameTempRepository?.updateFrameRate(fpsReplaced.toFloat())
                        }
                    }

                    // Schedule the next frame
                    frameCount++
                    Choreographer.getInstance().postFrameCallback(this)
                }
            })
        }
    }

    /**
     * Stops calculating frame rate and quits the HandlerThread.
     */
    fun stopCalculatingFrameRate() {
        fpsHandlerThread.quitSafely()
    }
}

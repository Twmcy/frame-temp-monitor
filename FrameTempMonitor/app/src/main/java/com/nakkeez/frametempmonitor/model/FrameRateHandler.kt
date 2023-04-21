package com.nakkeez.frametempmonitor.model

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Choreographer
import androidx.lifecycle.ViewModelProvider
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import com.nakkeez.frametempmonitor.viewmodel.FrameTempViewModel

/**
 * Calculates the frame rate the system is running on
 */
class FrameRateHandler(
    private val frameTempRepository: FrameTempRepository?,
    private val frameTempViewModel: FrameTempViewModel?
) {

    private val fpsHandlerThread = HandlerThread("FPSHandlerThread")

    private lateinit var fpsHandler: Handler

    // Create a handler for updating UI elements on the main thread
    private val uiHandler = Handler(Looper.getMainLooper())

    private var frameCount = 0
    private var lastFrameTime = System.nanoTime()

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

                        // Save the calculated frame rate to the repository using main thread
                        uiHandler.post {
                            // update LiveData from ViewModel/Repository depending if the calculations
                            // are made from OverlayService or MainActivity
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

    fun stopCalculatingFrameRate() {
        fpsHandlerThread.quitSafely()
    }
}

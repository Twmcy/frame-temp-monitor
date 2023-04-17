package com.nakkeez.frametempmonitor.model

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Choreographer
import androidx.lifecycle.ViewModelProvider
import com.nakkeez.frametempmonitor.data.FrameTempRepository
import com.nakkeez.frametempmonitor.viewmodel.FrameTempViewModel

class FrameRateHandler(private val frameTempRepository: FrameTempRepository) {

    private val fpsHandlerThread = HandlerThread("FPSHandlerThread")

    private lateinit var fpsHandler: Handler

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

                        val fpsRounded = String.format("%.2f", fps)

                        // Save the calculated frame rate to the repository using main thread
                        val handler = Handler(Looper.getMainLooper())

                        handler.post {
                            frameTempRepository.updateFrameRate(fpsRounded.toFloat())
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

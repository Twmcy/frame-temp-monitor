package com.nakkeez.frametempmonitor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Choreographer
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private var frameCount = 0
    private var lastFrameTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                    fpsTextView.text = "${String.format("%.2f", fps)} fps"
                }

                // Schedule the next frame
                frameCount++
                Choreographer.getInstance().postFrameCallback(this)
            }
        })
    }
}
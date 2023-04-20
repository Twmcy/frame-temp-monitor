package com.nakkeez.frametempmonitor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nakkeez.frametempmonitor.adapter.FrameTempAdapter
import com.nakkeez.frametempmonitor.data.FrameTempData
import com.nakkeez.frametempmonitor.data.FrameTempDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FrameTempDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FrameTempAdapter
    private lateinit var db: FrameTempDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frame_temp_data)

        recyclerView = findViewById(R.id.recyclerview)

        db = FrameTempDatabase.getInstance(this)

        setupRecyclerView()

        val deleteButton = findViewById<FloatingActionButton>(R.id.delFloatingActionButton)

        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                // Delete performance data from Room database
                withContext(Dispatchers.IO) { db.frameTempDao().deleteAll() }
                // Create recyclerview again so user will notice that the data is removed
                setupRecyclerView()
            }
        }
    }

    private suspend fun loadFrameTempData(): List<FrameTempData> =
        withContext(Dispatchers.IO) { db.frameTempDao().getAll() }

    private fun setupRecyclerView() {
        lifecycleScope.launch {
            val data = loadFrameTempData()

            adapter = FrameTempAdapter(data)

            recyclerView.adapter = adapter
            LinearLayoutManager(this@FrameTempDataActivity).also { recyclerView.layoutManager = it }
        }
    }
}


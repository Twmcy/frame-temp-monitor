package com.nakkeez.frametempmonitor

import android.os.Bundle
import android.view.MenuItem
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

/**
 * Activity responsible for displaying and managing saved performance data.
 */
class FrameTempDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FrameTempAdapter
    private lateinit var db: FrameTempDatabase

    /**
     * Initializes the activity, sets up the UI, and initializes necessary components.
     * @param savedInstanceState The saved state of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frame_temp_data)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

    /**
     * Loads performance data from the Room database.
     * @return List of performance data.
     */
    private suspend fun loadFrameTempData(): List<FrameTempData> =
        withContext(Dispatchers.IO) { db.frameTempDao().getAll() }

    /**
     * Sets up the RecyclerView with performance data.
     */
    private fun setupRecyclerView() {
        lifecycleScope.launch {
            val data = loadFrameTempData()

            adapter = FrameTempAdapter(data)

            recyclerView.adapter = adapter
            LinearLayoutManager(this@FrameTempDataActivity).also { recyclerView.layoutManager = it }
        }
    }

    /**
     * Handles the Up button presses by returning the user to MainActivity.
     * @param item The selected menu item.
     * @return True if the selection is handled, otherwise execute the
     * default behaviour defined in the superclass.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}


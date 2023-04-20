package com.nakkeez.frametempmonitor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nakkeez.frametempmonitor.R
import com.nakkeez.frametempmonitor.data.FrameTempData

/**
 * Defines a RecyclerView adapter for a list of FrameTempData objects.
 */
class FrameTempAdapter(private val data: List<FrameTempData>) :
    RecyclerView.Adapter<FrameTempAdapter.ViewHolder>() {

    // Create and return a ViewHolder for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.frame_temp_data_item, parent, false)
        return ViewHolder(view)
    }

    // Bind the data to the ViewHolder for a given position
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    // Return the number of items in the list
    override fun getItemCount(): Int = data.size

    // Define a ViewHolder that holds the views for a single item in the list
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val frameDataTextView: TextView = itemView.findViewById(R.id.frameDataTextView)
        private val tempDataTextView: TextView = itemView.findViewById(R.id.tempDataTextView)
        private val timeDataTextView: TextView = itemView.findViewById(R.id.timeDataTextView)

        // Bind the FrameTempData object's attributes to the Views in the item view
        fun bind(frameTempData: FrameTempData) {
            frameDataTextView.text = String.format(
                itemView.context.getString(R.string.data_fps),
                frameTempData.frameRate
            )
            tempDataTextView.text = String.format(
                itemView.context.getString(R.string.data_temp),
                frameTempData.batteryTemp
            )
            timeDataTextView.text = String.format(
                itemView.context.getString(R.string.data_time),
                frameTempData.timestamp
            )
        }
    }
}

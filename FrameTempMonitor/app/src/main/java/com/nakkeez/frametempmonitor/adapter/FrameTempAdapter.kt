package com.nakkeez.frametempmonitor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nakkeez.frametempmonitor.R
import com.nakkeez.frametempmonitor.data.FrameTempData

/**
 * Adapter for managing and displaying a list of FrameTempData objects in a RecyclerView.
 * @param data List of FrameTempData objects to be displayed.
 */
class FrameTempAdapter(private val data: List<FrameTempData>) :
    RecyclerView.Adapter<FrameTempAdapter.ViewHolder>() {

    /**
     * Creates a new ViewHolder by inflating the item view layout.
     * @param parent The parent ViewGroup.
     * @param viewType The type of the view.
     * @return A new ViewHolder instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.frame_temp_data_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds data to the views in the ViewHolder for a given position.
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the data list.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    /**
     * Gets the total number of items in the data list.
     * @return The number of items in the data list.
     */
    override fun getItemCount(): Int = data.size

    /**
     * ViewHolder class for holding the views of a single item in the RecyclerView.
     * @param itemView The item view.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val frameDataTextView: TextView = itemView.findViewById(R.id.frameDataTextView)
        private val batteryDataTextView: TextView = itemView.findViewById(R.id.batteryDataTextView)
        private val cpuDataTextView: TextView = itemView.findViewById(R.id.cpuDataTextView)
        private val timeDataTextView: TextView = itemView.findViewById(R.id.timeDataTextView)

        /**
         * Binds FrameTempData object's attributes to the views in the item view.
         * @param frameTempData The FrameTempData object to bind.
         */
        fun bind(frameTempData: FrameTempData) {
            frameDataTextView.text = String.format(
                itemView.context.getString(R.string.data_fps),
                frameTempData.frameRate
            )
            batteryDataTextView.text = String.format(
                itemView.context.getString(R.string.data_battery),
                frameTempData.batteryTemp
            )
            cpuDataTextView.text = String.format(
                itemView.context.getString(R.string.data_cpu),
                frameTempData.cpuTemp
            )
            timeDataTextView.text = String.format(
                itemView.context.getString(R.string.data_time),
                frameTempData.timestamp
            )
        }
    }
}

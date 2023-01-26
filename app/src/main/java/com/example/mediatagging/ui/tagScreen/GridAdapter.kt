package com.example.mediatagging.ui.tagScreen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mediatagging.R

class GridAdapter(private val itemCount: Int) : RecyclerView.Adapter<GridAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tag_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Bind the data for the item
    }

    override fun getItemCount() = itemCount

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        fun create(parent: ViewGroup, spanCount: Int): GridAdapter {
            val itemCount = spanCount * spanCount
            return GridAdapter(itemCount)
        }
    }
}

package com.example.mediatagging.ui.tagScreen

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mediatagging.R
import com.example.mediatagging.databinding.TagcountListItemBinding
import com.example.mediatagging.model.TagCountModel

class TagCountAdapter(
    private val context: Context,
    val tagCountList: ArrayList<TagCountModel>
) : RecyclerView.Adapter<TagCountAdapter.ContainerViewHolder>() {
    val TAG = "TagCountAdapter"

    inner class ContainerViewHolder(val binding: TagcountListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContainerViewHolder {
        val binding =
            TagcountListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ContainerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContainerViewHolder, position: Int) {
        val item = tagCountList[position]
        holder.binding.apply {
            tvTagCount.text = item.count.toString()
            tvTagName.text = "#${item.tagName}"
        }
    }

    override fun getItemCount(): Int {
        return tagCountList.size
    }

}
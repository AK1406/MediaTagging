package com.example.mediatagging.ui.project

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mediatagging.R
import com.example.mediatagging.databinding.ImageListItemBinding
import com.example.mediatagging.model.ImageModel
import com.example.mediatagging.model.VideoModel

class PhotosAdapter(
    private val context: Context,
    val clickListener: (item: ImageModel, pos: Int) -> Unit,
    val deleteListener: (pos: Int, view: View) -> Unit
) : RecyclerView.Adapter<PhotosAdapter.ContainerViewHolder>() {
    val TAG = "PhotosAdapter"
    var imageList = mutableListOf<ImageModel>()

    inner class ContainerViewHolder(val binding: ImageListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContainerViewHolder {
        val binding =
            ImageListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ContainerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContainerViewHolder, position: Int) {
        val item = imageList[position]
        holder.binding.ivImage.setOnClickListener {
            holder.binding.flDelete.visibility = View.GONE
            clickListener.invoke(item, position)
        }
        holder.binding.ivImage.setOnLongClickListener {
            holder.binding.flDelete.visibility = View.VISIBLE
            holder.binding.flDelete.setOnClickListener {
                deleteListener.invoke(position, holder.binding.flDelete)
            }
            true
        }
        try {
            holder.binding.tvImageName.text = item.imageTitle
        } catch (e: Exception) {
            Log.i(TAG, "exception: $e")
        }
        Glide.with(context)
            .load(item.imageUri)
            .placeholder(R.drawable.project)
            .into(holder.binding.ivImage)

        if (item.playAnimation == true) {
            holder.binding.laLoadingAnimation.visibility = View.VISIBLE
            holder.binding.laLoadingAnimation.repeatCount = ValueAnimator.INFINITE
            holder.binding.laLoadingAnimation.playAnimation()
        }

        if (item.playAnimation == false) {
            holder.binding.laLoadingAnimation.visibility = View.GONE
            holder.binding.laLoadingAnimation.cancelAnimation()
        }
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    fun insertImage(image: ImageModel) {
        this.imageList.add(image)
        notifyItemInserted(imageList.size)
    }


    fun insertImageList(images: ArrayList<ImageModel>) {
        this.imageList.clear()
        this.imageList.addAll(images)
        notifyDataSetChanged()
    }

    fun reverseList() {
        this.imageList.reverse()
        notifyDataSetChanged()
    }

    fun sortByMaxTag() {
        this.imageList.sortWith(compareByDescending { it.addedTags?.size })
        notifyDataSetChanged()
    }

    fun updateImage(image: ImageModel, pos: Int) {
        this.imageList[pos] = image
        notifyItemChanged(pos)
    }

    fun updateTags(pos: Int, tags: ArrayList<String>) {
        this.imageList[pos].addedTags?.clear()
        this.imageList[pos].addedTags?.addAll(tags)
        notifyDataSetChanged()
    }
}
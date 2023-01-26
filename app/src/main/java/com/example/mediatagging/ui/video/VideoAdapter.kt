package com.example.mediatagging.ui.video

import android.R.attr.data
import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mediatagging.R
import com.example.mediatagging.databinding.VideoListItemBinding
import com.example.mediatagging.model.ImageModel
import com.example.mediatagging.model.VideoModel


class VideoAdapter(
    private val context: Context,
    val clickListener: (item: VideoModel, pos: Int) -> Unit,
    val deleteListener: (pos: Int) -> Unit
) : RecyclerView.Adapter<VideoAdapter.ContainerViewHolder>() {
    val TAG = "VideoAdapter"
    var videoList = mutableListOf<VideoModel>()

    inner class ContainerViewHolder(val binding: VideoListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    fun clear() {
        videoList.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContainerViewHolder {
        val binding =
            VideoListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ContainerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContainerViewHolder, position: Int) {
        val item = videoList[position]
        holder.binding.ivVideo.setOnClickListener {
            holder.binding.flDelete.visibility = View.GONE
            clickListener.invoke(item, position)
        }

        holder.binding.ivVideo.setOnLongClickListener {
            holder.binding.flDelete.visibility = View.VISIBLE
            holder.binding.flDelete.setOnClickListener {
                deleteListener.invoke(position)
            }
            true
        }
        val interval: Long = 1000
        val options: RequestOptions = RequestOptions().frame(interval)
        holder.binding.tvVideoName.text = item.videoTitle
        holder.binding.cvVideo.setOnClickListener {
            clickListener.invoke(item, position)
        }
        Glide.with(context).asBitmap().load(item.videoUri).placeholder(R.drawable.video)
            .apply(options).into(holder.binding.ivVideo)

        if (item.playAnimation == true) {
            holder.binding.laLoadingAnimation.visibility = View.VISIBLE
            holder.binding.ivVideoPlay.visibility = View.GONE
            holder.binding.laLoadingAnimation.repeatCount = ValueAnimator.INFINITE
            holder.binding.laLoadingAnimation.playAnimation()
        }

        if (item.playAnimation == false) {
            holder.binding.laLoadingAnimation.visibility = View.GONE
            holder.binding.ivVideoPlay.visibility = View.VISIBLE
            holder.binding.laLoadingAnimation.cancelAnimation()
        }

    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    fun insertVideo(video: VideoModel) {
        this.videoList.add(video)
        notifyItemInserted(videoList.size)
    }


    fun insertVideoList(videos: ArrayList<VideoModel>) {
        this.videoList.clear()
        this.videoList.addAll(videos)
        notifyDataSetChanged()
    }


    fun updateVideo(video: VideoModel, pos: Int) {
        this.videoList[pos] = video
        notifyItemChanged(pos)
    }

    fun reverseList() {
        this.videoList.reverse()
        notifyDataSetChanged()
    }

    fun sortByMaxTag() {
        this.videoList.sortWith(compareByDescending { it.addedTags?.size })
        notifyDataSetChanged()
    }

    fun updateImage(video: VideoModel, pos: Int) {
        this.videoList[pos] = video
        notifyItemChanged(pos)
    }

    fun updateTags(pos:Int,tags:ArrayList<String>){
        this.videoList[pos].addedTags?.clear()
        this.videoList[pos].addedTags?.addAll(tags)
        notifyDataSetChanged()
    }

}
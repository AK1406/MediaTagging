package com.example.mediatagging.ui.project

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mediatagging.databinding.ProjectLineItemBinding
import com.example.mediatagging.model.ProjectModel

class ProjectAdapter(
    private val context: Context,
    val clickImage: (item: ProjectModel, pos: Int) -> Unit,
    val clickVideo: (item: ProjectModel, pos: Int) -> Unit,
    val showMenu: (view: View, item: ProjectModel, pos: Int) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ContainerViewHolder>() {
    val TAG = "ProjectAdapter"
    var projectList = mutableListOf<ProjectModel>()

    inner class ContainerViewHolder(val binding: ProjectLineItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContainerViewHolder {
        val binding =
            ProjectLineItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ContainerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContainerViewHolder, position: Int) {
        val item = projectList[position]
        holder.binding.categoryName.text = item.projectName
        holder.binding.tvImageCount.text = item.imageCount.toString() ?: "0"
        holder.binding.tvVideoCount.text = item.videoCount.toString() ?: "0"

        holder.binding.flProjectOption.setOnClickListener {
            showMenu.invoke(it, item, position)
        }

        holder.binding.flImage.setOnClickListener {
            clickImage.invoke(item, position)
        }

        holder.binding.flVideo.setOnClickListener {
            clickVideo.invoke(item, position)
        }
    }

    override fun getItemCount(): Int {
        return projectList.size
    }

    fun insertProject(project: ProjectModel) {
        this.projectList.add(project)
        notifyItemInserted(projectList.size)
    }

    fun insertProjectList(projects: ArrayList<ProjectModel>) {
        this.projectList.clear()
        this.projectList.addAll(projects)
        notifyDataSetChanged()
    }

    fun updateProject(pos: Int, newProject: ProjectModel) {
        this.projectList[pos].projectName = newProject.projectName
        notifyItemChanged(pos)
    }

    fun removeProject(projectName: String) {
        val position = this.projectList.indexOfFirst { it.projectName == projectName }
        if (position != -1) {
            this.projectList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
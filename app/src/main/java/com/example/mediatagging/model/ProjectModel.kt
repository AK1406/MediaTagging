package com.example.mediatagging.model

import java.io.Serializable

data class ProjectModel(
    var projectName: String?,
    val imageList: ArrayList<ImageModel>? = null,
    val videoList: ArrayList<VideoModel>? = null,
    val imageCount: Int? = 0,
    val videoCount: Int? = 0
) : Serializable {
    constructor() : this("", arrayListOf(), arrayListOf(), 0,0)
}
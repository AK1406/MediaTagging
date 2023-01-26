package com.example.mediatagging.model

import java.io.Serializable

data class VideoModel(
    val videoTitle: String? = "video",
    val videoUri: String?,
    val addedTags: ArrayList<String>? = null,
    var playAnimation : Boolean = false
) : Serializable {
    constructor() : this("",  "", arrayListOf(),false)
}
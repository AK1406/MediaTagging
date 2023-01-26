package com.example.mediatagging.model

import java.io.Serializable

data class ImageModel(
    val imageTitle:String?="image",
    val imageUri : String?,
    val addedTags : ArrayList<String>? = null,
    var playAnimation : Boolean = false
): Serializable {
    constructor(): this("", "",arrayListOf(),false)
}
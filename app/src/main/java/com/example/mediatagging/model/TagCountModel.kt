package com.example.mediatagging.model

data class TagCountModel(
    val tagName: String?,
    val count: Int?,
    val tagList: ArrayList<String>
){
    constructor():this("",0, arrayListOf())
}
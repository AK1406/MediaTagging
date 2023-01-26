package com.example.mediatagging.model.response

import com.example.mediatagging.model.Choices
import java.io.Serializable
data class GptResponse(
    val id:String?,
    val model : String?,
    val choices :ArrayList<Choices>
   /* val usage: String?,
    val prompt_tokens : Int?,
    val completion_tokens : Int?,
    val total_tokens : Int?*/
):Serializable


package com.example.mediatagging.ui.project

import android.provider.ContactsContract.CommonDataKinds.Im
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mediatagging.model.ImageModel

class ImageViewModel : ViewModel() {

    val imageListResult = MutableLiveData<ArrayList<ImageModel>>()

    init {
        imageListResult.postValue(arrayListOf())
    }

    fun setImageList(imageList: ArrayList<ImageModel>) {
        imageListResult.postValue(imageList)
    }
}
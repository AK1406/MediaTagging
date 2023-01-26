package com.example.mediatagging.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mediatagging.ui.image.Profile

class DashboardViewModel : ViewModel() {
    val profile = MutableLiveData<Profile>()
    val totalProject = MutableLiveData<Int>()

    init {
        totalProject.postValue(0)
    }

    fun setProfileDetails(name: String?, email: String?, profileUri: String?) {
        profile.postValue(Profile(name, profileUri, email, totalProject.value))
    }
}
package com.example.mediatagging.utils

interface ApiListener {

    fun showProgress();

    fun hideProgress();

    fun networkError(message: String);
}
package com.example.mediatagging.ui.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mediatagging.R
import com.example.mediatagging.databinding.ActivityDashboardBinding
import com.example.mediatagging.utils.FragmentController
import com.example.mediatagging.utils.constants.Constants


class DashboardActivity : AppCompatActivity() {
    lateinit var binding: ActivityDashboardBinding
    val TAG = "DashboardActivity"
    lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    fun initView() {
        initToolbar()
        setupViewModel()
        handleData()
        setupHomeFragment()
    }

    fun setupViewModel() {
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
    }


    private fun initToolbar() {
        binding.toolbar.tvHeaderName.visibility = View.VISIBLE
        binding.toolbar.ivBack.visibility = View.VISIBLE
    }


    fun handleData() {
        val url = intent.getStringExtra("user")
        val email = intent.getStringExtra("email")
        val name = intent.getStringExtra("name")
        dashboardViewModel.setProfileDetails(name,email,url)
        Log.i(TAG, "profile: $url")
        url?.let { downloadImage(it) }

    }


    fun downloadImage(uri: String) {
        Glide.with(this).load(uri).apply(RequestOptions().centerCrop())
            .into(binding.toolbar.ivProfile)
    }

    private fun setupHomeFragment() {
        Log.i(TAG, "setupHomeFragment")
        FragmentController.setFragment(
            this, R.id.fragmentMain, false, HomeFragment::class.java
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}
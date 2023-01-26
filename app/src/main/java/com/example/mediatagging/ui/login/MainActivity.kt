package com.example.mediatagging.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.example.mediatagging.R
import com.example.mediatagging.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding
    private val SPLASH_TIME_OUT:Long = 4000 // 1 sec
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.laSplashAnimation.visibility = View.VISIBLE
        binding.laSplashAnimation.playAnimation()
        Handler().postDelayed({
            startActivity(Intent(this,SignInActivity::class.java))
            finish()
        }, SPLASH_TIME_OUT)
    }
}
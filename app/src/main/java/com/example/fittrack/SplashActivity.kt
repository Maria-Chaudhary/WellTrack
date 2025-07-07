package com.example.fittrack

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FitTrack) // Apply app theme
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val illustration = findViewById<ImageView>(R.id.illustrationImage)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        val tvFooter = findViewById<TextView>(R.id.tvFooter)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.anim)

        logoImage.startAnimation(fadeIn)

        Handler().postDelayed({
            illustration.startAnimation(fadeIn)
        }, 500)

        Handler().postDelayed({
            tvAppName.startAnimation(fadeIn)
            tvTagline.startAnimation(fadeIn)
        }, 700)

        Handler().postDelayed({
            tvFooter.startAnimation(fadeIn)
        }, 1000)

        // Move to login/main screen after 2.5 seconds
        Handler().postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2500)
    }
}

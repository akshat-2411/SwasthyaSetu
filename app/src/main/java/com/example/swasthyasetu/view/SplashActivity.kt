package com.example.swasthyasetu.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.app.ActivityOptionsCompat
import com.example.swasthyasetu.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    override val requireAuthentication: Boolean = false

    private lateinit var binding: ActivitySplashBinding

    companion object {
        private const val SPLASH_DELAY_MS = 1500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable Activity Transitions for the window
        window.requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS)
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            animateLogoAndNavigate()
        }, SPLASH_DELAY_MS)
    }

    private fun animateLogoAndNavigate() {
        // Slightly scale up the logo like a heartbeat to grab attention before transitioning
        val scaleX = ObjectAnimator.ofFloat(binding.ivSplashLogo, View.SCALE_X, 1f, 1.25f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivSplashLogo, View.SCALE_Y, 1f, 1.25f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 450
            interpolator = OvershootInterpolator(1.5f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    navigateNext()
                }
            })
            start()
        }
    }

    private fun navigateNext() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        val targetActivity = if (currentUser != null) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }

        val intent = Intent(this, targetActivity)

        if (targetActivity == LoginActivity::class.java) {
            // Initiate the shared element transition
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                binding.ivSplashLogo,
                "logoTransition"
            )
            startActivity(intent, options.toBundle())
            // Delay the finish() call to ensure the activity isn't immediately destroyed,
            // which would clip or kill the shared element transition
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 800)
        } else {
            startActivity(intent)
            finish()
        }
    }
}

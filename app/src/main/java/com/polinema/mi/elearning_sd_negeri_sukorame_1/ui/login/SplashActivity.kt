package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.login

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ActivitySplashBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home.HomeActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fullscreen
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAnimations()
    }

    private fun startAnimations() {
        binding.logoContainer.scaleX = 0.3f
        binding.logoContainer.scaleY = 0.3f

        val fadeInLogo = ObjectAnimator.ofFloat(binding.logoContainer, "alpha", 0f, 1f)
        val scaleXLogo = ObjectAnimator.ofFloat(binding.logoContainer, "scaleX", 0.3f, 1f)
        val scaleYLogo = ObjectAnimator.ofFloat(binding.logoContainer, "scaleY", 0.3f, 1f)

        val logoAnim = AnimatorSet().apply {
            playTogether(fadeInLogo, scaleXLogo, scaleYLogo)
            duration = 700
            interpolator = OvershootInterpolator(1.2f)
            startDelay = 200
        }

        val fadeInDots = ObjectAnimator.ofFloat(binding.dotsContainer, "alpha", 0f, 1f).apply {
            duration = 400
            startDelay = 900
        }
        val fadeInTag = ObjectAnimator.ofFloat(binding.tvTagline, "alpha", 0f, 1f).apply {
            duration = 400
            startDelay = 1000
        }

        val dot1Anim = pulseDot(binding.dot1, 1100)
        val dot2Anim = pulseDot(binding.dot2, 1300)
        val dot3Anim = pulseDot(binding.dot3, 1500)

        logoAnim.start()
        fadeInDots.start()
        fadeInTag.start()
        dot1Anim.start()
        dot2Anim.start()
        dot3Anim.start()

        // Cek sesi login setelah animasi selesai
        binding.root.postDelayed({
            val session = SessionManager(this)
            val target = if (session.isLoggedIn()) HomeActivity::class.java
                         else LoginActivity::class.java
            startActivity(
                Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2800)
    }

    private fun pulseDot(view: android.view.View, delay: Long): ValueAnimator {
        return ValueAnimator.ofFloat(0.3f, 1f, 0.3f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            startDelay = delay
            addUpdateListener { view.alpha = it.animatedValue as Float }
        }
    }
}

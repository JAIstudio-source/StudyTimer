package com.madeby.JAI

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        videoView = findViewById(R.id.splashVideoView)

        val footerContainer = findViewById<android.view.View>(R.id.splashFooterContainer)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val navBarInset = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom
            val baseMargin = (48 * resources.displayMetrics.density).toInt()
            val params = footerContainer.layoutParams as android.widget.RelativeLayout.LayoutParams
            params.bottomMargin = baseMargin + navBarInset
            footerContainer.layoutParams = params
            insets
        }

        val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.logo_animation)
        videoView.setVideoURI(videoUri)

        videoView.setOnCompletionListener {
            navigateToNextScreen()
        }

        videoView.setOnErrorListener { _, _, _ ->
            navigateToNextScreen()
            true
        }

        // Tap to skip splash video
        videoView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                videoView.stopPlayback()
                navigateToNextScreen()
            }
            true
        }

        videoView.start()
    }

    private fun navigateToNextScreen() {
        val nextIntent = if (AuthManager.isLoggedIn(this)) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(nextIntent)
        finish()
    }
}

package com.indianservers.aiexplorer

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AiExplorerSplashScreen(Modifier.fillMaxSize())
            LaunchedEffect(Unit) {
                delay(SPLASH_DURATION_MILLIS)
                val transition = ActivityOptions.makeCustomAnimation(this@SplashActivity, 0, 0)
                val mainIntent = Intent(this@SplashActivity, MainActivity::class.java).putExtras(intent)
                startActivity(mainIntent, transition.toBundle())
                finish()
            }
        }
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 2_000L
    }
}

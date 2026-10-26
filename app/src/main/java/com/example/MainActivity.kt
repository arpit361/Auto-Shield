package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.autoshield.browser.BrowserViewModel
import com.example.autoshield.media.CarMediaSessionManager
import com.example.autoshield.ui.AutoShieldHomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during car travel video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize Android MediaSession for steering wheel controls & car integration
        CarMediaSessionManager.initialize(applicationContext)

        // Handle external or car launcher intents with URL
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                AutoShieldHomeScreen(viewModel = browserViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && (data.scheme == "http" || data.scheme == "https")) {
            browserViewModel.navigateTo(data.toString())
        }
    }
}

package com.example.autoshield.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.ItemList
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.example.MainActivity
import com.example.autoshield.media.CarMediaSessionManager
import com.example.autoshield.shield.BraveShieldEngine

class CarHomeScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val videoState = CarMediaSessionManager.videoState.value
        val shieldStats = BraveShieldEngine.stats.value

        val paneBuilder = Pane.Builder()

        // 1. Current Playback Row
        val playStatus = if (videoState.isPlaying) "Playing ▶" else "Paused ⏸"
        val playbackRow = Row.Builder()
            .setTitle(videoState.title)
            .addText("Status: $playStatus | ${videoState.currentTimeSeconds}s / ${videoState.durationSeconds}s")
            .addText("Tata Tiago Harman Audio & Steering Controls: Active")
            .addAction(
                Action.Builder()
                    .setTitle(if (videoState.isPlaying) "Pause" else "Play")
                    .setOnClickListener {
                        if (videoState.isPlaying) {
                            CarMediaSessionManager.onPauseCommand?.invoke()
                        } else {
                            CarMediaSessionManager.onPlayCommand?.invoke()
                        }
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("+10s")
                    .setOnClickListener {
                        CarMediaSessionManager.onSkipForwardCommand?.invoke()
                        invalidate()
                    }
                    .build()
            )
            .build()
        paneBuilder.addRow(playbackRow)

        // 2. Brave Shields Status Row
        val shieldStatusText = if (shieldStats.shieldsActive) "Shields UP (Ad & Tracker Block Active)" else "Shields Disabled"
        val motionModeText = if (shieldStats.passengerMotionMode) "Passenger Moving Car Mode: ACTIVE" else "Motion Lock: Normal"
        val shieldsRow = Row.Builder()
            .setTitle("AutoShield Privacy & Motion Guard")
            .addText("$shieldStatusText | Ads Blocked: ${shieldStats.adsBlocked}")
            .addText("$motionModeText (Prevents car motion lockouts)")
            .addAction(
                Action.Builder()
                    .setTitle(if (shieldStats.shieldsActive) "Shields ON" else "Shields OFF")
                    .setOnClickListener {
                        BraveShieldEngine.toggleShields(!shieldStats.shieldsActive)
                        invalidate()
                    }
                    .build()
            )
            .build()
        paneBuilder.addRow(shieldsRow)

        // 3. Quick Portal Launcher Row
        val quickPortalRow = Row.Builder()
            .setTitle("Open Video on Phone / Dash Display")
            .addText("Tap to launch YouTube / Browser on device display with AdBlock")
            .setOnClickListener {
                val intent = Intent(carContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                carContext.startActivity(intent)
            }
            .build()
        paneBuilder.addRow(quickPortalRow)

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(if (shieldStats.passengerMotionMode) "Passenger Mode: ON" else "Passenger Mode: OFF")
                    .setOnClickListener {
                        BraveShieldEngine.togglePassengerMotionMode(!shieldStats.passengerMotionMode)
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("AutoShield • Tata Tiago Companion")
            .setActionStrip(actionStrip)
            .build()
    }
}

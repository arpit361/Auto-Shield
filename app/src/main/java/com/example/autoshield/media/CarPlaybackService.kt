package com.example.autoshield.media

import android.app.Service
import android.content.Intent
import android.os.IBinder

class CarPlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = CarMediaSessionManager.buildPlaybackNotification(this)
        startForeground(CarMediaSessionManager.NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

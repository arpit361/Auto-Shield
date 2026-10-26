package com.example.autoshield.media

import android.media.browse.MediaBrowser.MediaItem
import android.os.Bundle
import android.service.media.MediaBrowserService

/**
 * MediaBrowserService for Android Auto.
 * Registers AutoShield in Android Auto's native Media/Audio app list
 * on head units (e.g. Tata Tiago Harman display).
 */
class AutoShieldMediaService : MediaBrowserService() {

    override fun onCreate() {
        super.onCreate()
        CarMediaSessionManager.initialize(applicationContext)
        val token = CarMediaSessionManager.getSessionToken()
        if (token != null) {
            sessionToken = token
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("autoshield_media_root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }
}

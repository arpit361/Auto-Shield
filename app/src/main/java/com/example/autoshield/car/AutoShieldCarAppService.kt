package com.example.autoshield.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.R
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Android Auto CarAppService for AutoShield.
 * Uses Navigation category in Manifest and ALLOW_ALL_HOSTS_VALIDATOR
 * so head units (including Tata Tiago / Harman systems) can communicate cleanly.
 */
class AutoShieldCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return AutoShieldSession()
    }
}

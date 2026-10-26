package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.autoshield.browser.BrowserViewModel
import com.example.autoshield.shield.BraveShieldEngine
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AutoShield", appName)
    }

    @Test
    fun `brave shield engine toggles correctly`() {
        BraveShieldEngine.resetStats()
        assertTrue(BraveShieldEngine.stats.value.shieldsActive)
        assertEquals(0, BraveShieldEngine.stats.value.adsBlocked)

        BraveShieldEngine.recordAdSkipped()
        assertEquals(1, BraveShieldEngine.stats.value.adsBlocked)
    }

    @Test
    fun `brave farbling and anti-fingerprinting engine produces scripts and tracks probes`() {
        BraveShieldEngine.resetStats()
        val farblingScript = BraveShieldEngine.getFarblingProtectionScript()
        assertTrue(farblingScript.contains("CanvasRenderingContext2D"))
        assertTrue(farblingScript.contains("UNMASKED_RENDERER_WEBGL"))
        assertTrue(farblingScript.contains("AudioBuffer"))

        assertEquals(0, BraveShieldEngine.stats.value.fingerprintProbesBlocked)
        BraveShieldEngine.recordFingerprintProbeBlocked()
        assertEquals(1, BraveShieldEngine.stats.value.fingerprintProbesBlocked)

        BraveShieldEngine.recordHttpsUpgrade()
        assertEquals(1, BraveShieldEngine.stats.value.httpsUpgrades)
    }

    @Test
    fun `browser viewModel manages speed dial and home navigation`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BrowserViewModel(app)

        assertTrue(vm.uiState.value.isHomePage)
        assertTrue(vm.uiState.value.speedDials.isNotEmpty())

        val initialCount = vm.uiState.value.speedDials.size
        vm.addSpeedDial("Custom Portal", "https://example.com/video", "#10B981")
        assertEquals(initialCount + 1, vm.uiState.value.speedDials.size)

        vm.navigateTo("https://m.youtube.com")
        assertFalse(vm.uiState.value.isHomePage)
        assertEquals("https://m.youtube.com", vm.uiState.value.currentUrl)

        vm.goHome()
        assertTrue(vm.uiState.value.isHomePage)
    }

    @Test
    fun `brave content blocker okhttp interceptor blocks ad and tracker domains`() {
        BraveShieldEngine.resetStats()
        val interceptor = com.example.autoshield.shield.BraveContentBlockerInterceptor(BraveShieldEngine)

        val adUrl = "https://googleads.g.doubleclick.net/pagead/ads?client=ca-pub".toHttpUrl()
        val adDecision = interceptor.evaluateUrl(adUrl)
        assertTrue(adDecision is com.example.autoshield.shield.BraveContentBlockerInterceptor.BlockDecision.Blocked)

        val trackerUrl = "https://google-analytics.com/analytics.js".toHttpUrl()
        val trackerDecision = interceptor.evaluateUrl(trackerUrl)
        assertTrue(trackerDecision is com.example.autoshield.shield.BraveContentBlockerInterceptor.BlockDecision.Blocked)

        val fpUrl = "https://fingerprint.com/v3/telemetry".toHttpUrl()
        val fpDecision = interceptor.evaluateUrl(fpUrl)
        assertTrue(fpDecision is com.example.autoshield.shield.BraveContentBlockerInterceptor.BlockDecision.Blocked)
    }

    @Test
    fun `brave content blocker strips tracking query parameters`() {
        BraveShieldEngine.resetStats()
        val interceptor = com.example.autoshield.shield.BraveContentBlockerInterceptor(BraveShieldEngine)

        val trackedUrl = "https://example.com/article?utm_source=newsletter&utm_medium=email&gclid=xyz789&fbclid=abc123&keep_this=valuable".toHttpUrl()
        val sanitized = interceptor.stripTrackingParams(trackedUrl)

        assertTrue(sanitized != null)
        assertFalse(sanitized!!.contains("utm_source"))
        assertFalse(sanitized.contains("gclid"))
        assertFalse(sanitized.contains("fbclid"))
        assertTrue(sanitized.contains("keep_this=valuable"))
    }

    @Test
    fun `brave content blocker preserves youtube video playback and google auth`() {
        val interceptor = com.example.autoshield.shield.BraveContentBlockerInterceptor(BraveShieldEngine)

        val ytPlaybackUrl = "https://m.youtube.com/api/stats/playback?ns=yt&docid=xyz".toHttpUrl()
        val ytDecision = interceptor.evaluateUrl(ytPlaybackUrl)
        assertTrue(ytDecision is com.example.autoshield.shield.BraveContentBlockerInterceptor.BlockDecision.Allowed)

        val googleAuthUrl = "https://accounts.google.com/ServiceLogin?service=youtube".toHttpUrl()
        val authDecision = interceptor.evaluateUrl(googleAuthUrl)
        assertTrue(authDecision is com.example.autoshield.shield.BraveContentBlockerInterceptor.BlockDecision.Allowed)
    }
}

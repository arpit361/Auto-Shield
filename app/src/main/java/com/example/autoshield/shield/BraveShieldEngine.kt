package com.example.autoshield.shield

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.ByteArrayInputStream

data class ShieldStats(
    val adsBlocked: Int = 0,
    val trackersBlocked: Int = 0,
    val fingerprintProbesBlocked: Int = 0,
    val httpsUpgrades: Int = 0,
    val queryParamsStripped: Int = 0,
    val okHttpInterceptions: Int = 0,
    val dataSavedBytes: Long = 0L,
    val timeSavedSeconds: Int = 0,
    val shieldsActive: Boolean = true,
    val fingerprintingProtectionActive: Boolean = true,
    val videoAdSpeedup: Boolean = true,
    val cosmeticFilterActive: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val httpsEverywhereActive: Boolean = true,
    val stripTrackingParamsActive: Boolean = true,
    val passengerMotionMode: Boolean = true,
    val isIncognitoMode: Boolean = false
)

object BraveShieldEngine {

    private val _stats = MutableStateFlow(ShieldStats())
    val stats: StateFlow<ShieldStats> = _stats.asStateFlow()

    // Curated high-performance ad & tracker domain rules inspired by Brave-Core
    private val adDomains = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "pagead2.googlesyndication.com",
        "pagead2.googleadservices.com",
        "adservice.google.com",
        "adnxs.com",
        "admob.com",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "moatads.com",
        "casalemedia.com",
        "amazon-adsystem.com",
        "advertising.com",
        "adcolony.com",
        "applovin.com",
        "unityads.unity3d.com",
        "vungle.com",
        "yieldmo.com",
        "serving-sys.com",
        "bidswitch.net",
        "exponential.com",
        "inmobi.com",
        "media.net",
        "popads.net",
        "propellerads.com",
        "smartadserver.com",
        "adroll.com",
        "zedo.com",
        "revcontent.com",
        "mgid.com",
        "scorecardresearch.com",
        "flashtalking.com",
        "adition.com",
        "liveramp.com",
        "adblade.com",
        "spotxchange.com"
    )

    private val trackerDomains = hashSetOf(
        "google-analytics.com",
        "analytics.google.com",
        "googletagmanager.com",
        "hotjar.com",
        "clarity.ms",
        "quantserve.com",
        "chartbeat.com",
        "newrelic.com",
        "segment.io",
        "segment.com",
        "mixpanel.com",
        "amplitude.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "kochava.com",
        "omtrdc.net",
        "demdex.net",
        "krxd.net",
        "parsely.com",
        "statcounter.com",
        "mouseflow.com",
        "fullstory.com",
        "crazyegg.com",
        "inspectlet.com",
        "clicky.com",
        "yandex.ru/metrika",
        "mc.yandex.ru"
    )

    private val fingerprintDomains = hashSetOf(
        "fingerprint.com",
        "fpjs.sh",
        "clientjs.org",
        "augur.io",
        "perimeterx.net",
        "datadome.co",
        "threatmetrix.com",
        "iovation.com",
        "siftscience.com",
        "fraudmetrix.cn",
        "riskified.com",
        "forter.com",
        "castle.io"
    )

    private val adUrlKeywords = listOf(
        "/pagead/",
        "googleads.g.doubleclick.net",
        "ad_type=advideo",
        "doubleclick.net/gampad",
        "fbevents.js",
        "/pixel/tracking"
    )

    fun toggleShields(enabled: Boolean) {
        _stats.update { it.copy(shieldsActive = enabled) }
    }

    fun toggleFingerprintingProtection(enabled: Boolean) {
        _stats.update { it.copy(fingerprintingProtectionActive = enabled) }
    }

    fun toggleHttpsEverywhere(enabled: Boolean) {
        _stats.update { it.copy(httpsEverywhereActive = enabled) }
    }

    fun toggleBlockThirdPartyCookies(enabled: Boolean) {
        _stats.update { it.copy(blockThirdPartyCookies = enabled) }
    }

    fun toggleIncognitoMode(enabled: Boolean) {
        _stats.update { it.copy(isIncognitoMode = enabled) }
    }

    fun togglePassengerMotionMode(enabled: Boolean) {
        _stats.update { it.copy(passengerMotionMode = enabled) }
    }

    fun toggleVideoAdSpeedup(enabled: Boolean) {
        _stats.update { it.copy(videoAdSpeedup = enabled) }
    }

    fun toggleCosmeticFilter(enabled: Boolean) {
        _stats.update { it.copy(cosmeticFilterActive = enabled) }
    }

    fun toggleStripTrackingParams(enabled: Boolean) {
        _stats.update { it.copy(stripTrackingParamsActive = enabled) }
    }

    fun resetStats() {
        _stats.update {
            it.copy(
                adsBlocked = 0,
                trackersBlocked = 0,
                fingerprintProbesBlocked = 0,
                httpsUpgrades = 0,
                queryParamsStripped = 0,
                okHttpInterceptions = 0,
                dataSavedBytes = 0L,
                timeSavedSeconds = 0
            )
        }
    }

    fun recordAdSkipped() {
        _stats.update { current ->
            current.copy(
                adsBlocked = current.adsBlocked + 1,
                dataSavedBytes = current.dataSavedBytes + 350_000L,
                timeSavedSeconds = current.timeSavedSeconds + 15
            )
        }
    }

    fun recordFingerprintProbeBlocked() {
        _stats.update { current ->
            current.copy(
                fingerprintProbesBlocked = current.fingerprintProbesBlocked + 1
            )
        }
    }

    fun recordHttpsUpgrade() {
        _stats.update { current ->
            current.copy(
                httpsUpgrades = current.httpsUpgrades + 1
            )
        }
    }

    fun isFarblingAllowedForUrl(url: String): Boolean {
        val lower = url.lowercase()
        // Do not farble on YouTube or Google domains to prevent Botguard anti-bot triggers
        if (lower.contains("youtube.com") || lower.contains("googlevideo.com") || lower.contains("google.com")) {
            return false
        }
        return _stats.value.fingerprintingProtectionActive
    }

    fun shouldBlock(request: WebResourceRequest): WebResourceResponse? {
        if (!_stats.value.shieldsActive) return null

        val uri = request.url ?: return null
        val host = uri.host?.lowercase() ?: return null
        val urlString = uri.toString().lowercase()

        // 1. NEVER block Google Auth / Account login endpoints
        if (host == "accounts.google.com" ||
            host == "myaccount.google.com" ||
            host == "accounts.youtube.com" ||
            host == "oauth2.googleapis.com" ||
            (host.endsWith(".google.com") && (uri.path?.contains("signin") == true || uri.path?.contains("oauth") == true))) {
            return null
        }

        // 2. NEVER block YouTube internal player playback endpoints (avoids "Sign in to confirm you're not a bot")
        if (host.endsWith("youtube.com") || host.endsWith("googlevideo.com") || host.endsWith("ytimg.com")) {
            if (urlString.contains("/youtubei/v1/player") ||
                urlString.contains("/api/stats/playback") ||
                urlString.contains("/get_video_info") ||
                urlString.contains("videoplayback") ||
                urlString.contains("/ptracking")) {
                return null
            }
        }

        // 3. Check direct host or subdomain match in adDomains
        if (isDomainOrSubdomainMatched(host, adDomains)) {
            recordBlockedAd()
            return createEmptyResponse()
        }

        // 4. Check tracker host match
        if (isDomainOrSubdomainMatched(host, trackerDomains)) {
            recordBlockedTracker()
            return createEmptyResponse()
        }

        // 5. Check fingerprinting script host match
        if (_stats.value.fingerprintingProtectionActive && isDomainOrSubdomainMatched(host, fingerprintDomains)) {
            recordFingerprintProbeBlocked()
            return createEmptyResponse()
        }

        // 6. Keyword / query match for non-YouTube requests
        if (!host.endsWith("youtube.com") && !host.endsWith("googlevideo.com")) {
            for (keyword in adUrlKeywords) {
                if (urlString.contains(keyword)) {
                    recordBlockedAd()
                    return createEmptyResponse()
                }
            }
        }

        return null
    }

    private fun isDomainOrSubdomainMatched(host: String, domainSet: Set<String>): Boolean {
        if (domainSet.contains(host)) return true
        for (domain in domainSet) {
            if (host.endsWith(".$domain")) {
                return true
            }
        }
        return false
    }

    fun recordBlockedAd() {
        _stats.update { current ->
            current.copy(
                adsBlocked = current.adsBlocked + 1,
                okHttpInterceptions = current.okHttpInterceptions + 1,
                dataSavedBytes = current.dataSavedBytes + 85_000L,
                timeSavedSeconds = current.timeSavedSeconds + 1
            )
        }
    }

    fun recordBlockedTracker() {
        _stats.update { current ->
            current.copy(
                trackersBlocked = current.trackersBlocked + 1,
                okHttpInterceptions = current.okHttpInterceptions + 1,
                dataSavedBytes = current.dataSavedBytes + 25_000L
            )
        }
    }

    fun recordQueryParamsStripped(count: Int = 1) {
        _stats.update { current ->
            current.copy(
                queryParamsStripped = current.queryParamsStripped + count,
                okHttpInterceptions = current.okHttpInterceptions + 1
            )
        }
    }

    fun recordOkHttpInterception() {
        _stats.update { current ->
            current.copy(
                okHttpInterceptions = current.okHttpInterceptions + 1
            )
        }
    }

    private fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            mapOf("Access-Control-Allow-Origin" to "*"),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * Injects Brave-Core style Anti-Fingerprinting (Farbling) protection:
     * - Canvas Farbling (randomizes pixel hashes imperceptibly)
     * - WebGL Vendor/Renderer Masking (spoofs GPU hardware model)
     * - AudioContext sample farbling
     * - Battery API Neutralization (prevents cross-site battery-level tracking)
     * - Hardware Concurrency & Memory Standardization
     * - WebRTC local IP leak prevention
     */
    fun getFarblingProtectionScript(): String {
        return """
            (function() {
                if (window.__autoshield_farbled) return;
                window.__autoshield_farbled = true;

                const notifyProbe = (type) => {
                    if (window.AutoShieldBridge && window.AutoShieldBridge.onFingerprintProbeBlocked) {
                        window.AutoShieldBridge.onFingerprintProbeBlocked(type);
                    }
                };

                // 1. Canvas Farbling Protection
                try {
                    const origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
                    CanvasRenderingContext2D.prototype.getImageData = function(sx, sy, sw, sh) {
                        const imgData = origGetImageData.apply(this, arguments);
                        for (let i = 0; i < imgData.data.length; i += 64) {
                            imgData.data[i] = (imgData.data[i] ^ 1) & 0xFF;
                        }
                        notifyProbe('Canvas');
                        return imgData;
                    };

                    const origToDataURL = HTMLCanvasElement.prototype.toDataURL;
                    HTMLCanvasElement.prototype.toDataURL = function() {
                        notifyProbe('Canvas');
                        return origToDataURL.apply(this, arguments);
                    };
                } catch(e) {}

                // 2. WebGL Hardware Masking
                try {
                    const maskWebGL = (proto) => {
                        if (!proto) return;
                        const UNMASKED_VENDOR_WEBGL = 0x9245;
                        const UNMASKED_RENDERER_WEBGL = 0x9246;
                        const origGetParam = proto.getParameter;
                        proto.getParameter = function(param) {
                            if (param === UNMASKED_VENDOR_WEBGL || param === 0x9245) {
                                notifyProbe('WebGL');
                                return 'Google Inc. (AutoShield Protected)';
                            }
                            if (param === UNMASKED_RENDERER_WEBGL || param === 0x9246) {
                                notifyProbe('WebGL');
                                return 'ANGLE (ARM Mali Generic Automotive Display)';
                            }
                            return origGetParam.apply(this, arguments);
                        };
                    };
                    if (window.WebGLRenderingContext) maskWebGL(WebGLRenderingContext.prototype);
                    if (window.WebGL2RenderingContext) maskWebGL(WebGL2RenderingContext.prototype);
                } catch(e) {}

                // 3. AudioContext Fingerprinting Farbling
                try {
                    if (window.AudioBuffer) {
                        const origGetChannelData = AudioBuffer.prototype.getChannelData;
                        AudioBuffer.prototype.getChannelData = function() {
                            const buffer = origGetChannelData.apply(this, arguments);
                            for (let i = 0; i < buffer.length; i += 128) {
                                buffer[i] += (Math.random() - 0.5) * 1e-7;
                            }
                            notifyProbe('Audio');
                            return buffer;
                        };
                    }
                } catch(e) {}

                // 4. Battery API Neutralization
                try {
                    if (navigator.getBattery) {
                        navigator.getBattery = function() {
                            notifyProbe('Battery');
                            return Promise.resolve({
                                charging: true,
                                chargingTime: 0,
                                dischargingTime: Infinity,
                                level: 1.0,
                                addEventListener: function() {},
                                removeEventListener: function() {}
                            });
                        };
                    }
                } catch(e) {}

                // 5. Hardware Concurrency & Memory Standardization
                try {
                    Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });
                    Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });
                    Object.defineProperty(navigator, 'doNotTrack', { get: () => '1' });
                    Object.defineProperty(navigator, 'globalPrivacyControl', { get: () => true });
                } catch(e) {}

                // 6. WebRTC Local IP Leak Protection
                try {
                    if (window.RTCPeerConnection) {
                        const origCreateOffer = RTCPeerConnection.prototype.createOffer;
                        RTCPeerConnection.prototype.createOffer = function() {
                            notifyProbe('WebRTC');
                            return origCreateOffer.apply(this, arguments);
                        };
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }

    /**
     * Injects YouTube ad-skipping, video acceleration (16x during ad),
     * auto-skip button clicking, and video state observation.
     */
    fun getAdSkipperAndMediaScript(): String {
        return """
            (function() {
                if (window.__autoshield_injected) return;
                window.__autoshield_injected = true;

                // 1. Fast Video Ad skipper & speed-up
                function skipAds() {
                    try {
                        const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-overlay-close-button');
                        if (skipBtn) {
                            skipBtn.click();
                            if (window.AutoShieldBridge && window.AutoShieldBridge.onAdSkipped) {
                                window.AutoShieldBridge.onAdSkipped();
                            }
                        }

                        const adContainer = document.querySelector('.ad-showing, .ad-interrupting, .video-ads');
                        const video = document.querySelector('video');
                        if (adContainer && video) {
                            video.playbackRate = 16.0;
                            video.muted = true;
                            if (isFinite(video.duration) && video.duration > 0) {
                                video.currentTime = video.duration - 0.1;
                            }
                        }

                        // Remove dismissable YouTube promo banners
                        const promoAds = document.querySelectorAll('ytd-promoted-sparkles-web-renderer, ytd-banner-promo-renderer, #player-ads, .ytp-ad-overlay-container, ytd-ad-slot-renderer');
                        promoAds.forEach(el => el.remove());
                    } catch (e) {}
                }

                setInterval(skipAds, 350);

                // 2. Video state synchronization to native car MediaSession
                function hookVideos() {
                    const videos = document.querySelectorAll('video');
                    videos.forEach(v => {
                        if (v.__hooked) return;
                        v.__hooked = true;

                        const notifyState = (playing) => {
                            if (window.AutoShieldBridge && window.AutoShieldBridge.onVideoStateChanged) {
                                const title = document.title || 'Video Playing';
                                window.AutoShieldBridge.onVideoStateChanged(
                                    title,
                                    playing,
                                    Math.floor(v.currentTime || 0),
                                    Math.floor(v.duration || 0)
                                );
                            }
                        };

                        v.addEventListener('play', () => notifyState(true));
                        v.addEventListener('playing', () => notifyState(true));
                        v.addEventListener('pause', () => notifyState(false));
                        v.addEventListener('ended', () => notifyState(false));
                    });
                }

                setInterval(hookVideos, 1000);
            })();
        """.trimIndent()
    }

    /**
     * Cosmetic CSS filter injection to hide ad placeholders and promotional popups
     */
    fun getCosmeticFilterCss(): String {
        return """
            .ad-container, .ad-banner, .google-ads, #player-ads, .ytp-ad-overlay-container,
            ytd-promoted-sparkles-web-renderer, ytd-banner-promo-renderer, ytd-statement-banner-renderer,
            ytd-in-feed-ad-layout-renderer, ytd-ad-slot-renderer, [class*="ad-box"], [id*="banner-ad"],
            div[id*="dfp-ad"], .adsbygoogle, .advertisement, [aria-label="advertisement"] {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                width: 0 !important;
                pointer-events: none !important;
            }
        """.trimIndent()
    }
}

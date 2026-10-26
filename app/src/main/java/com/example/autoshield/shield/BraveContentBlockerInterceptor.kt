package com.example.autoshield.shield

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayInputStream

/**
 * High-performance OkHttp Interceptor providing Brave-like content blocking,
 * tracker filtering, query parameter sanitization (Clean URLs), and HTTPS Everywhere enforcement.
 */
class BraveContentBlockerInterceptor(
    private val shieldEngine: BraveShieldEngine = BraveShieldEngine
) : Interceptor {

    sealed class BlockDecision {
        object Allowed : BlockDecision()
        data class Blocked(val reason: String, val category: String) : BlockDecision()
        data class Modified(val sanitizedUrl: String) : BlockDecision()
    }

    companion object {
        // Query parameters known for cross-site tracking, campaign correlation, and fingerprinting (Brave Query Filter)
        val TRACKING_QUERY_PARAMS = setOf(
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_term",
            "utm_content",
            "utm_id",
            "utm_reader",
            "gclid",
            "gclsrc",
            "dclid",
            "fbclid",
            "msclkid",
            "mc_eid",
            "igshid",
            "_hsenc",
            "_openstat",
            "yclid",
            "wickedid",
            "fbadid",
            "sc_campaign",
            "mkt_tok"
        )

        // Popular ad delivery networks and bidding exchanges
        val AD_DOMAINS = hashSetOf(
            "doubleclick.net",
            "googleads.g.doubleclick.net",
            "googlesyndication.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "criteo.com",
            "criteo.net",
            "taboola.com",
            "outbrain.com",
            "pubmatic.com",
            "adnxs.com",
            "rubiconproject.com",
            "adcolony.com",
            "unityads.unity3d.com",
            "applovin.com",
            "inmobi.com",
            "ironsrc.com",
            "vungle.com",
            "chartboost.com",
            "mopub.com",
            "adroll.com",
            "smartadserver.com",
            "openx.net",
            "amazon-adsystem.com",
            "casalemedia.com",
            "advertising.com",
            "exponential.com",
            "media.net",
            "sovrn.com",
            "spotxchange.com",
            "teads.tv",
            "yieldmo.com",
            "bidswitch.net",
            "sharethrough.com",
            "zergnet.com",
            "mgid.com",
            "revcontent.com",
            "popcash.net",
            "popads.net",
            "propellerads.com",
            "exoclick.com",
            "juicyads.com",
            "trafficjunky.com"
        )

        // Web trackers, session recorders, and analytics engines
        val TRACKER_DOMAINS = hashSetOf(
            "google-analytics.com",
            "analytics.google.com",
            "googletagmanager.com",
            "hotjar.com",
            "clarity.ms",
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
            "scorecardresearch.com",
            "quantserve.com",
            "statcounter.com",
            "mouseflow.com",
            "fullstory.com",
            "crazyegg.com",
            "inspectlet.com",
            "clicky.com",
            "yandex.ru",
            "mc.yandex.ru"
        )

        // Device and browser fingerprinting probes
        val FINGERPRINT_DOMAINS = hashSetOf(
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

        // Suspicious URL keywords indicating ad requests
        val AD_KEYWORDS = listOf(
            "/pagead/",
            "googleads.g.doubleclick.net",
            "ad_type=advideo",
            "doubleclick.net/gampad",
            "fbevents.js",
            "/pixel/tracking"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val stats = shieldEngine.stats.value

        // 1. If Brave Shields are globally disabled, proceed normally
        if (!stats.shieldsActive) {
            return chain.proceed(originalRequest)
        }

        // 2. Evaluate URL against Content Blocker rules
        val decision = evaluateUrl(originalUrl)

        when (decision) {
            is BlockDecision.Blocked -> {
                // Record metrics in BraveShieldEngine
                when (decision.category) {
                    "ad" -> shieldEngine.recordBlockedAd()
                    "tracker" -> shieldEngine.recordBlockedTracker()
                    "fingerprint" -> shieldEngine.recordFingerprintProbeBlocked()
                }

                // Return synthetic empty HTTP 204 No Content response
                return createBlockedResponse(originalRequest, decision.reason)
            }
            is BlockDecision.Modified -> {
                // Tracking parameters were stripped or HTTP was upgraded to HTTPS
                val modifiedHttpUrl = decision.sanitizedUrl.toHttpUrlOrNull() ?: originalUrl
                val sanitizedRequest = originalRequest.newBuilder()
                    .url(modifiedHttpUrl)
                    .header("DNT", "1")
                    .header("Sec-GPC", "1")
                    .build()
                return chain.proceed(sanitizedRequest)
            }
            BlockDecision.Allowed -> {
                // Add Brave Privacy Headers
                val requestWithPrivacyHeaders = originalRequest.newBuilder()
                    .header("DNT", "1")
                    .header("Sec-GPC", "1")
                    .build()
                return chain.proceed(requestWithPrivacyHeaders)
            }
        }
    }

    /**
     * Evaluates an OkHttp [HttpUrl] to determine if it should be blocked, sanitized, or allowed.
     */
    fun evaluateUrl(httpUrl: HttpUrl): BlockDecision {
        val host = httpUrl.host.lowercase()
        val urlString = httpUrl.toString()
        val stats = shieldEngine.stats.value

        // A. Whitelist essential YouTube/Google playback and auth infrastructure
        if (isWhitelistedDomain(host, urlString)) {
            return BlockDecision.Allowed
        }

        // B. Check Ad Domains
        if (isDomainMatched(host, AD_DOMAINS)) {
            return BlockDecision.Blocked("Blocked Ad Domain: $host", "ad")
        }

        // C. Check Tracker Domains
        if (isDomainMatched(host, TRACKER_DOMAINS)) {
            return BlockDecision.Blocked("Blocked Tracker Domain: $host", "tracker")
        }

        // D. Check Fingerprinting Domains
        if (stats.fingerprintingProtectionActive && isDomainMatched(host, FINGERPRINT_DOMAINS)) {
            return BlockDecision.Blocked("Blocked Fingerprinting Probe: $host", "fingerprint")
        }

        // E. Check Ad URL keywords (non-YouTube)
        if (!host.contains("youtube.com") && !host.contains("googlevideo.com")) {
            for (keyword in AD_KEYWORDS) {
                if (urlString.contains(keyword, ignoreCase = true)) {
                    return BlockDecision.Blocked("Blocked Ad Keyword: $keyword", "ad")
                }
            }
        }

        // F. Strip tracking query parameters (Brave Clean URLs)
        if (stats.stripTrackingParamsActive) {
            val sanitized = stripTrackingParams(httpUrl)
            if (sanitized != null) {
                shieldEngine.recordQueryParamsStripped()
                return BlockDecision.Modified(sanitized)
            }
        }

        // G. HTTPS Everywhere Upgrade
        if (stats.httpsEverywhereActive && httpUrl.scheme == "http" && !isLocalHost(host)) {
            val upgraded = httpUrl.newBuilder().scheme("https").build().toString()
            shieldEngine.recordHttpsUpgrade()
            return BlockDecision.Modified(upgraded)
        }

        return BlockDecision.Allowed
    }

    /**
     * Strips tracking query parameters from an [HttpUrl] (e.g. utm_*, fbclid, gclid).
     * Returns the sanitized URL string if any parameters were removed, or null if untouched.
     */
    fun stripTrackingParams(httpUrl: HttpUrl): String? {
        val queryNames = httpUrl.queryParameterNames
        val hasTrackingParams = queryNames.any { it.lowercase() in TRACKING_QUERY_PARAMS }
        if (!hasTrackingParams) return null

        val builder = httpUrl.newBuilder()
        for (param in TRACKING_QUERY_PARAMS) {
            builder.removeAllQueryParameters(param)
        }
        return builder.build().toString()
    }

    /**
     * Checks an Android [WebResourceRequest] and returns a blocked [WebResourceResponse] if
     * the OkHttp Content Blocker determines the resource is an ad or tracker.
     */
    fun checkWebResource(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url ?: return null
        val httpUrl = url.toString().toHttpUrlOrNull() ?: return null
        val decision = evaluateUrl(httpUrl)

        return when (decision) {
            is BlockDecision.Blocked -> {
                when (decision.category) {
                    "ad" -> shieldEngine.recordBlockedAd()
                    "tracker" -> shieldEngine.recordBlockedTracker()
                    "fingerprint" -> shieldEngine.recordFingerprintProbeBlocked()
                }
                createWebResourceEmptyResponse()
            }
            else -> null
        }
    }

    private fun isDomainMatched(host: String, set: Set<String>): Boolean {
        if (set.contains(host)) return true
        for (domain in set) {
            if (host.endsWith(".$domain")) return true
        }
        return false
    }

    private fun isWhitelistedDomain(host: String, url: String): Boolean {
        // Critical for YouTube & Google Auth functionality
        if (host == "accounts.google.com" || host == "myaccount.google.com") return true
        if (host.endsWith("googlevideo.com")) return true
        if (host.endsWith("youtube.com")) {
            if (url.contains("/api/stats/playback") ||
                url.contains("/ptracking") ||
                url.contains("/youtubei/v1/player") ||
                url.contains("/videoplayback") ||
                url.contains("/v1/account") ||
                url.contains("service=youtube")
            ) {
                return true
            }
        }
        return false
    }

    private fun isLocalHost(host: String): Boolean {
        return host == "localhost" || host == "127.0.0.1" || host.startsWith("192.168.")
    }

    private fun createBlockedResponse(request: Request, reason: String): Response {
        val emptyBody = "".toResponseBody("text/plain; charset=utf-8".toMediaType())
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(204)
            .message("Blocked by Brave Content Blocker ($reason)")
            .body(emptyBody)
            .addHeader("Access-Control-Allow-Origin", "*")
            .addHeader("X-Brave-Shield-Blocked", "true")
            .build()
    }

    private fun createWebResourceEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "X-Brave-Shield-Blocked" to "true"
            ),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}

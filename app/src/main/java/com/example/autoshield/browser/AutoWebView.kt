package com.example.autoshield.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.autoshield.media.CarMediaSessionManager
import com.example.autoshield.shield.BraveHttpClient
import com.example.autoshield.shield.BraveShieldEngine

class AutoShieldJsBridge(
    private val onAdSkippedCallback: () -> Unit,
    private val onVideoStateChangedCallback: (String, Boolean, Long, Long) -> Unit,
    private val onFingerprintProbeBlockedCallback: (String) -> Unit
) {
    @JavascriptInterface
    fun onAdSkipped() {
        onAdSkippedCallback()
    }

    @JavascriptInterface
    fun onVideoStateChanged(title: String, isPlaying: Boolean, currentTime: Long, duration: Long) {
        onVideoStateChangedCallback(title, isPlaying, currentTime, duration)
    }

    @JavascriptInterface
    fun onFingerprintProbeBlocked(apiType: String) {
        onFingerprintProbeBlockedCallback(apiType)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AutoWebView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    onCustomViewChanged: (View?, WebChromeClient.CustomViewCallback?) -> Unit
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Strict privacy + Full web standards & video player compatibility
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false

                // Allow media blob & video stream decoding
                allowContentAccess = true
                allowFileAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false

                savePassword = false
                saveFormData = false
                setGeolocationEnabled(false)

                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false

                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true

                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT

                // Authentic Chrome 130 on Android UA (Pixel 8 Pro) without 'wv' / 'Version/4.0'
                userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
            }

            // Accept cookies (including YouTube & Google session cookies)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // JavaScript bridge to native Kotlin
            addJavascriptInterface(
                AutoShieldJsBridge(
                    onAdSkippedCallback = {
                        BraveShieldEngine.recordAdSkipped()
                    },
                    onVideoStateChangedCallback = { title, isPlaying, currentTime, duration ->
                        CarMediaSessionManager.updateVideoInfo(
                            title = title,
                            isPlaying = isPlaying,
                            currentTime = currentTime,
                            duration = duration,
                            url = url ?: ""
                        )
                    },
                    onFingerprintProbeBlockedCallback = { _ ->
                        BraveShieldEngine.recordFingerprintProbeBlocked()
                    }
                ),
                "AutoShieldBridge"
            )

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    viewModel.setProgress(newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    viewModel.updatePageStatus(
                        title = title ?: "",
                        url = view?.url ?: "",
                        canBack = view?.canGoBack() ?: false,
                        canForward = view?.canGoForward() ?: false
                    )
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    onCustomViewChanged(view, callback)
                    viewModel.setFullscreenVideo(true)
                }

                override fun onHideCustomView() {
                    onCustomViewChanged(null, null)
                    viewModel.setFullscreenVideo(false)
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    if (transport != null && view != null) {
                        transport.webView = view
                        resultMsg.sendToTarget()
                        return true
                    }
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url ?: return false
                    // HTTPS Everywhere: Upgrade insecure http to https (except local test hosts)
                    if (BraveShieldEngine.stats.value.httpsEverywhereActive && url.scheme == "http") {
                        val host = url.host ?: ""
                        if (!host.contains("localhost") && !host.contains("127.0.0.1")) {
                            val secureUrl = url.buildUpon().scheme("https").build()
                            BraveShieldEngine.recordHttpsUpgrade()
                            view?.loadUrl(secureUrl.toString())
                            return true
                        }
                    }
                    return false
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    if (request != null) {
                        // 1. Primary OkHttp Content Blocker Interceptor
                        val okHttpBlocked = BraveHttpClient.contentBlockerInterceptor.checkWebResource(request)
                        if (okHttpBlocked != null) {
                            return okHttpBlocked
                        }

                        // 2. Brave Shield Engine rules (CSS & ad acceleration fallback)
                        val blocked = BraveShieldEngine.shouldBlock(request)
                        if (blocked != null) {
                            return blocked
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    val currentUrl = url ?: ""
                    viewModel.setProgress(15)
                    viewModel.updatePageStatus(
                        title = view?.title ?: "",
                        url = currentUrl,
                        canBack = view?.canGoBack() ?: false,
                        canForward = view?.canGoForward() ?: false
                    )

                    // 1. Only inject Farbling on non-Google/YouTube domains to pass YouTube Botguard checks
                    if (BraveShieldEngine.isFarblingAllowedForUrl(currentUrl)) {
                        val farblingJs = BraveShieldEngine.getFarblingProtectionScript()
                        view?.evaluateJavascript(farblingJs, null)
                    }

                    // 2. Inject cosmetic CSS to collapse video ad slots and banners
                    val css = BraveShieldEngine.getCosmeticFilterCss()
                    val jsCss = """
                        (function() {
                            var style = document.createElement('style');
                            style.type = 'text/css';
                            style.innerHTML = `$css`;
                            document.head ? document.head.appendChild(style) : document.documentElement.appendChild(style);
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(jsCss, null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val currentUrl = url ?: ""
                    viewModel.setProgress(100)
                    viewModel.updatePageStatus(
                        title = view?.title ?: "",
                        url = currentUrl,
                        canBack = view?.canGoBack() ?: false,
                        canForward = view?.canGoForward() ?: false
                    )

                    // Re-apply farbling only on non-Google/YouTube domains
                    if (BraveShieldEngine.isFarblingAllowedForUrl(currentUrl)) {
                        val farblingJs = BraveShieldEngine.getFarblingProtectionScript()
                        view?.evaluateJavascript(farblingJs, null)
                    }

                    val js = BraveShieldEngine.getAdSkipperAndMediaScript()
                    view?.evaluateJavascript(js, null)
                }
            }
        }
    }

    // Connect steering wheel / MediaSession callbacks to the active webView
    DisposableEffect(webView) {
        CarMediaSessionManager.onPlayCommand = {
            webView.post {
                webView.evaluateJavascript("document.querySelector('video')?.play()", null)
            }
        }
        CarMediaSessionManager.onPauseCommand = {
            webView.post {
                webView.evaluateJavascript("document.querySelector('video')?.pause()", null)
            }
        }
        CarMediaSessionManager.onSeekCommand = { seconds ->
            webView.post {
                webView.evaluateJavascript("const v = document.querySelector('video'); if (v) v.currentTime = $seconds;", null)
            }
        }
        CarMediaSessionManager.onSkipForwardCommand = {
            webView.post {
                webView.evaluateJavascript("const v = document.querySelector('video'); if (v) v.currentTime += 10;", null)
            }
        }
        CarMediaSessionManager.onSkipBackwardCommand = {
            webView.post {
                webView.evaluateJavascript("const v = document.querySelector('video'); if (v) v.currentTime = Math.max(0, v.currentTime - 10);", null)
            }
        }

        onDispose {
            CarMediaSessionManager.onPlayCommand = null
            CarMediaSessionManager.onPauseCommand = null
            CarMediaSessionManager.onSeekCommand = null
            CarMediaSessionManager.onSkipForwardCommand = null
            CarMediaSessionManager.onSkipBackwardCommand = null
        }
    }

    // Observe Desktop Mode toggle
    val isDesktop = viewModel.uiState.value.isDesktopMode
    LaunchedEffect(isDesktop) {
        val desktopUA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        val mobileUA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        webView.settings.userAgentString = if (isDesktop) desktopUA else mobileUA
        if (!viewModel.uiState.value.isHomePage) {
            webView.reload()
        }
    }

    // Observe URL changes from ViewModel
    val targetUrl = viewModel.uiState.value.currentUrl
    val isHome = viewModel.uiState.value.isHomePage

    LaunchedEffect(targetUrl, isHome) {
        if (!isHome && targetUrl.isNotBlank() && targetUrl != "brave://newtab") {
            if (webView.url != targetUrl) {
                webView.loadUrl(targetUrl)
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

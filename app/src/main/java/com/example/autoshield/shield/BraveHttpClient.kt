package com.example.autoshield.shield

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Singleton OkHttpClient pre-configured with the [BraveContentBlockerInterceptor]
 * for privacy-preserving network requests, filter list fetching, and WebResource handling.
 */
object BraveHttpClient {

    val contentBlockerInterceptor = BraveContentBlockerInterceptor(BraveShieldEngine)

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(contentBlockerInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}

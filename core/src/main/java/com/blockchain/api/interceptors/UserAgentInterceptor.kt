package com.blockchain.api.interceptors

import java.util.Locale
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    private val versionName: String,
    private val versionType: String,
    private val buildTypeSuffix: String,
) : Interceptor {

    /**
     * Inserts a pre-formatted header into all web requests, matching the pattern
     * "Blockchain-Android/6.4.2 (Android 5.0.1)".
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val userAgent = "Blockchain-Android$buildTypeSuffix/$versionName (Android $versionType)"

        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .header(
                name = "Accept-Language",
                value = "${Locale.getDefault().toLanguageTag()},${Locale.getDefault().language}"
            )
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}

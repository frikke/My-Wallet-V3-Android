package com.blockchain.network.interceptor

import java.util.concurrent.TimeUnit
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation

private const val HEADER_PRAGMA = "Pragma"
private const val HEADER_CACHE_CONTROL = "Cache-Control"
private const val NO_CACHE = 0

private fun getCacheableMaxAge(request: Request): Int {
    val invocation: Invocation? = request.tag(Invocation::class.java)
    val annotation: Cacheable? = invocation?.method()?.getAnnotation(Cacheable::class.java)

    return annotation?.maxAge ?: NO_CACHE
}

class RequestCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val cacheMaxAge = getCacheableMaxAge(request)

        if (cacheMaxAge != NO_CACHE) {
            val cacheControl = CacheControl.Builder()
                .maxAge(cacheMaxAge, TimeUnit.SECONDS)
                .build()

            request = request.newBuilder()
                .removeHeader(HEADER_PRAGMA)
                .removeHeader(HEADER_CACHE_CONTROL)
                .cacheControl(cacheControl)
                .build()
        }
        return chain.proceed(request)
    }
}

class ResponseCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        val request = chain.request()
        val cacheMaxAge = getCacheableMaxAge(request)

        return if (cacheMaxAge != NO_CACHE) {
            val cacheControl = CacheControl.Builder()
                .maxAge(cacheMaxAge, TimeUnit.SECONDS)
                .build()

            response.newBuilder()
                .removeHeader(HEADER_PRAGMA)
                .removeHeader(HEADER_CACHE_CONTROL)
                .header(HEADER_CACHE_CONTROL, cacheControl.toString())
                .build()
        } else {
            response
        }
    }
}

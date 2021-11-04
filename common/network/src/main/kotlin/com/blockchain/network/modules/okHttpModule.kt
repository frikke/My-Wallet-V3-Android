package com.blockchain.network.modules

import com.blockchain.appinfo.AppInfo
import com.blockchain.network.TLSSocketFactory
import com.blockchain.network.interceptor.RequestCacheInterceptor
import com.blockchain.network.interceptor.ResponseCacheInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.koin.dsl.module

private const val API_TIMEOUT = 30L
private const val PING_INTERVAL = 10L

private const val HTTP_CACHE_SIZE = 5 * 1024 * 1024.toLong()

private fun cache(dir: File): Cache =
    Cache(dir, HTTP_CACHE_SIZE)

val okHttpModule = module {
    single {
        val appInfo: AppInfo = get()
        val builder = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
            .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .certificatePinner(get())
            .cache(cache(appInfo.cacheDir))
            .addNetworkInterceptor(ResponseCacheInterceptor())

        get<OkHttpInterceptors>().forEach {
            builder.addInterceptor(it)
        }
        builder.addInterceptor(RequestCacheInterceptor())

        /*
          Enable TLS specific version V.1.2
          Issue Details : https://github.com/square/okhttp/issues/1934
         */
        TLSSocketFactory().also {
            builder.sslSocketFactory(it, it.systemDefaultTrustManager())
        }
        builder.build()
    }
}

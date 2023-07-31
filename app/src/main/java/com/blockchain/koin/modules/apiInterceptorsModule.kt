package com.blockchain.koin.modules

import android.os.Build
import com.blockchain.api.interceptors.ApiLoggingInterceptor
import com.blockchain.api.interceptors.AuthInterceptor
import com.blockchain.api.interceptors.DeviceIdInterceptor
import com.blockchain.api.interceptors.RequestIdInterceptor
import com.blockchain.api.interceptors.SSLPinningInterceptor
import com.blockchain.api.interceptors.SessionIdInterceptor
import com.blockchain.api.interceptors.SessionInfo
import com.blockchain.api.interceptors.UserAgentInterceptor
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.instrumentation.InstrumentationInterceptor // ktlint-disable instrumentation-ruleset:no-instrumentation-import
import com.blockchain.network.modules.OkHttpAuthInterceptor
import com.blockchain.network.modules.OkHttpInterceptors
import com.blockchain.network.modules.OkHttpLoggingInterceptors
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.facebook.stetho.okhttp3.StethoInterceptor
import java.util.UUID
import okhttp3.Interceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig

val apiInterceptorsModule = module {

    single {
        val env: EnvironmentConfig = get()
        val buildTypeSuffix = if (env.isAlphaBuild()) "-alpha" else ""
        val versionName = BuildConfig.VERSION_NAME.removeSuffix(BuildConfig.VERSION_NAME_SUFFIX)

        OkHttpInterceptors(
            mutableListOf(
                SSLPinningInterceptor(sslPinningEmitter = get()),
                UserAgentInterceptor(
                    versionName = versionName,
                    versionType = Build.VERSION.RELEASE,
                    buildTypeSuffix = buildTypeSuffix
                ),
                DeviceIdInterceptor(prefs = lazy { get() }, get()),
                RequestIdInterceptor { UUID.randomUUID().toString() },
                SessionIdInterceptor(environmentUrls = get(), sessionId = SessionInfo)
            )
        )
    }

    single {
        val env: EnvironmentConfig = get()
        OkHttpLoggingInterceptors(
            mutableListOf<Interceptor>().apply {
                // add for staging and alpha debugs
                if (env.isRunningInDebugMode()) {
                    add(StethoInterceptor())
                    add(ApiLoggingInterceptor())
                    add(ChuckerInterceptor.Builder(androidContext()).build())
                    add(InstrumentationInterceptor())
                    // add for alpha prod build
                } else if (!env.isRunningInDebugMode() && env.isCompanyInternalBuild()) {
                    add(ChuckerInterceptor.Builder(androidContext()).build())
                }
            }
        )
    }

    single {
        OkHttpAuthInterceptor(
            AuthInterceptor()
        )
    }
}

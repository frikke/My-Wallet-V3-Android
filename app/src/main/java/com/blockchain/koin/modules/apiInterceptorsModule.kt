package com.blockchain.koin.modules

import android.os.Build
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.koin.authInterceptorFeatureFlag
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
import piuk.blockchain.androidcore.data.api.interceptors.ApiLoggingInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.AuthInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.DeviceIdInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.RequestIdInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.SSLPinningInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.UserAgentInterceptor
import piuk.blockchain.androidcore.utils.SessionPrefs

val apiInterceptorsModule = module {

    single {
        val versionName = BuildConfig.VERSION_NAME.removeSuffix(BuildConfig.VERSION_NAME_SUFFIX)
        OkHttpInterceptors(
            mutableListOf(
                SSLPinningInterceptor(sslPinningEmitter = get()),
                UserAgentInterceptor(versionName, Build.VERSION.RELEASE),
                DeviceIdInterceptor(prefs = lazy { get<SessionPrefs>() }, get()),
                RequestIdInterceptor { UUID.randomUUID().toString() }
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
                    // add for alpha prod build
                } else if (!env.isRunningInDebugMode() && env.isCompanyInternalBuild()) {
                    add(ChuckerInterceptor.Builder(androidContext()).build())
                }
            }
        )
    }

    single {
        OkHttpAuthInterceptor(
            AuthInterceptor(
                authInterceptorFeatureFlag = get(authInterceptorFeatureFlag),
            )
        )
    }
}

package com.blockchain.api.interceptors

import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.koin.payloadScope
import com.blockchain.logging.Logger
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.network.interceptor.AuthenticationNotRequired
import com.blockchain.network.interceptor.CustomAuthentication
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.koin.core.component.KoinComponent
import retrofit2.Invocation

class AuthInterceptor : Interceptor, KoinComponent {

    private val nabuToken: NabuToken
        get() = payloadScope.get()
    private val nabuDataManager: NabuDataManager
        get() = payloadScope.get()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestAnnotations = originalRequest.tag(Invocation::class.java)?.method()?.annotations.orEmpty()

        if (requestAnnotations.any { it is CustomAuthentication }) {
            return chain.proceed(originalRequest)
        } else if (requestAnnotations.any { it is AuthenticationNotRequired }) {
            if (originalRequest.header("authorization") != null) {
                val url = originalRequest.url
                Logger.w("authorization header stripped on AuthenticationNotRequired call url: $url")
            }

            val request = originalRequest.newBuilder()
                .removeHeader("authorization")
                .build()
            return chain.proceed(request)
        }

        val offlineToken = try {
            nabuToken.fetchNabuToken().blockingGet()
        } catch (ex: Exception) {
            Logger.e("fetchNabuToken failed ${originalRequest.url} $ex")
            return chain.proceed(originalRequest)
        }

        val sessionToken = try {
            nabuDataManager.currentToken(offlineToken).blockingGet()
        } catch (ex: Exception) {
            Logger.e("currentToken failed ${originalRequest.url} $ex")
            return chain.proceed(originalRequest)
        }

        val request = originalRequest.newBuilder()
            .header("authorization", sessionToken.authHeader)
            .build()

        val response = chain.proceed(request)

        return if (response.code == NabuErrorStatusCodes.TokenExpired.code) {
            response.body?.closeQuietly()
            nabuDataManager.clearAccessToken()
            try {
                val refreshedToken = nabuDataManager.refreshToken(offlineToken).blockingGet()
                val newRequest = request
                    .newBuilder()
                    .header("authorization", refreshedToken.authHeader)
                    .build()
                chain.proceed(newRequest)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }
}

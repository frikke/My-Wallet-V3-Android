package com.blockchain.api.interceptors

import com.blockchain.api.NabuErrorStatusCodes
import com.blockchain.koin.payloadScope
import com.blockchain.logging.Logger
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.network.interceptor.AuthenticationNotRequired
import com.blockchain.network.interceptor.CustomAuthentication
import okhttp3.Interceptor
import okhttp3.Request
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
            currentToken(offlineToken)
        } catch (e: Exception) {
            return chain.proceed(originalRequest)
        }

        val request = originalRequest.newBuilder()
            .header("authorization", sessionToken.authHeader)
            .build()

        val response = chain.proceed(request)

        return if (response.code == NabuErrorStatusCodes.TokenExpired.code) {
            synchronized(this) {
                try {
                    val currentToken = currentToken(offlineToken)
                    if (!currentToken.hasExpired()) {
                        val newRequest = request
                            .newBuilder()
                            .header("authorization", currentToken.authHeader)
                            .build()
                        return chain.proceed(newRequest)
                    } else {
                        val newRequest = requestWitRefreshedToken(request, response, offlineToken)
                        chain.proceed(newRequest)
                    }
                } catch (_: Exception) {
                    response
                }
            }
        } else {
            response
        }
    }

    @Synchronized
    private fun currentToken(offlineToken: NabuOfflineToken): NabuSessionTokenResponse {
        return nabuDataManager.currentToken(offlineToken).blockingGet()
    }

    private fun requestWitRefreshedToken(
        request: Request,
        response: Response,
        offlineToken: NabuOfflineToken
    ): Request {
        response.body?.closeQuietly()
        nabuDataManager.clearAccessToken()
        val refreshedToken = nabuDataManager.refreshToken(offlineToken).blockingGet()
        return request
            .newBuilder()
            .header("authorization", refreshedToken.authHeader)
            .build()
    }
}

package com.blockchain.api.interceptors

import com.blockchain.enviroment.EnvironmentUrls
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Interceptor
import okhttp3.Response

class SessionIdInterceptor(
    private val environmentUrls: EnvironmentUrls,
    private val sessionId: SessionInfo
) : Interceptor {

    companion object {
        private const val sessionIDHeaderKey: String = "X-Session-ID"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Only set the header for nabu-gateway requests
        if (!originalRequest.url.toString().startsWith(environmentUrls.nabuApi)) {
            return chain.proceed(originalRequest)
        }

        return sessionId.getSessionId()?.let { sessionId ->
            chain.proceed(
                originalRequest.newBuilder()
                    .header(sessionIDHeaderKey, sessionId)
                    .build()
            )
        } ?: chain.proceed(originalRequest)
    }
}

object SessionInfo {
    private val sessionIdValue = AtomicReference<String?>(null)
    private val userIdValue = AtomicReference<String?>(null)

    fun getSessionId() = sessionIdValue.get()

    fun setSessionId(sessionId: String) {
        sessionIdValue.set(sessionId)
    }

    fun clearSessionId() {
        sessionIdValue.set(null)
    }

    fun getUserId() = userIdValue.get()

    fun setUserId(userId: String) {
        userIdValue.set(userId)
    }

    fun clearUserId() {
        userIdValue.set(null)
    }
}

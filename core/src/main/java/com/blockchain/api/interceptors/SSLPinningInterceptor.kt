package com.blockchain.api.interceptors

import javax.net.ssl.SSLPeerUnverifiedException
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import piuk.blockchain.androidcore.data.connectivity.SSLPinningEmitter

class SSLPinningInterceptor(val sslPinningEmitter: SSLPinningEmitter) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {
            return chain.proceed(request)
        } catch (exception: SSLPeerUnverifiedException) {
            sslPinningEmitter.emit()
        }

        // If an SSL exception was captured, we are returning a fake response
        // with a forbidden status code.
        //
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .message("")
            .body("".toResponseBody())
            .code(403)
            .build()
    }
}

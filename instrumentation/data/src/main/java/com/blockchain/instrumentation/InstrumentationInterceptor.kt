package com.blockchain.instrumentation

import com.blockchain.logging.Logger
import java.util.UUID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Invocation

/**
 * Example usage:
 *
 * ```
 * interface Api {
 *     @Instrument("SUCCESS_EMPTY", 200, """{"cards":[]}""")
 *     @Instrument("SUCCESS_CARD", 200, """
 *         {
 *             "cards": [
 *                 {
 *                     "type": "AMEX",
 *                     "number": 123,
 *                 }
 *             ]
 *         }
 *     """)
 *     @Instrument("FAILURE", 400, """{"error":"something"}""")
 *     fun method(): Something
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Instrument(val responses: Array<Response>)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Response(val key: String, val code: Int, val json: String)

class InstrumentationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        val annotation = originalRequest.tag(Invocation::class.java)?.method()
            ?.annotations
            .orEmpty()
            .filterIsInstance<Instrument>()
            .firstOrNull() ?: return chain.proceed(originalRequest)

        val requestId = UUID.randomUUID()
        val instrumentedResponses = annotation.responses.map {
            InstrumentedResponse.Json(it.key, it.code, it.json)
        }
        InstrumentationQueue.add(requestId, originalRequest.url.encodedPath, true, instrumentedResponses)

        val myInstrumentedRequest = runBlocking {
            InstrumentationQueue.queue
                .map { queue -> queue.find { it.requestId == requestId } }
                .firstOrNull { myRequest ->
                    if (myRequest == null) {
                        Logger.e("NULL REQUEST")
                        true
                    } else if (myRequest.pickedResponse != null) {
                        InstrumentationQueue.remove(requestId)
                        true
                    } else {
                        false
                    }
                }
        }

        val pickedInstrumentedResponse =
            myInstrumentedRequest?.pickedResponse?.orElse(null) as? InstrumentedResponse.Json
        return if (pickedInstrumentedResponse != null) {
            okhttp3.Response.Builder()
                .request(originalRequest)
                .protocol(Protocol.HTTP_2)
                .message("")
                .code(pickedInstrumentedResponse.code)
                .body(pickedInstrumentedResponse.json.toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        } else {
            chain.proceed(originalRequest)
        }
    }
}

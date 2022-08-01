package com.blockchain.api.adapters

import com.blockchain.outcome.Outcome
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit

class OutcomeCallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        callType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        // Call<*>
        if (callType !is ParameterizedType) return null
        val rawCallType = getRawType(callType)
        if (rawCallType != Call::class.java) return null

        // Call<Outcome<*, *>>
        val outcomeType = getParameterUpperBound(0, callType)
        if (outcomeType !is ParameterizedType) return null
        val rawOutcomeType = getRawType(outcomeType)
        if (rawOutcomeType != Outcome::class.java) return null

        // Call<Outcome<Exception, returnType>>
        val returnType = extractReturnType(outcomeType) ?: return null
        return OutcomeCallAdapter<Any>(returnType)
    }

    private fun extractReturnType(responseType: ParameterizedType): Type? {
        // We only support ApiErrors for now on Outcome
        return when (getParameterUpperBound(0, responseType)) {
            Exception::class.java -> {
                return getParameterUpperBound(1, responseType)
            }
            else -> null
        }
    }
}

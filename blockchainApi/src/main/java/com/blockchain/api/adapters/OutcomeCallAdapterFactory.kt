package com.blockchain.api.adapters

import com.blockchain.outcome.Outcome
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit

class OutcomeCallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        // Check that the return type is a Call and is also a parameterized (generic) type
        check(returnType is ParameterizedType) { "Return type must be a parameterized type." }
        return when (getRawType(returnType)) {
            Call::class.java -> outcomeAdapter(returnType)
            else -> null
        }
    }

    private fun outcomeAdapter(returnType: ParameterizedType): CallAdapter<Type, out Call<out Any>>? {
        // Check that the type parameter upper bound on position 0 is Outcome and is parameterized
        val responseType = getParameterUpperBound(0, returnType)
        check(responseType is ParameterizedType) { "Response type must be a parameterized type." }
        return when (getRawType(responseType)) {
            Outcome::class.java -> {
                extractReturnType(responseType)?.let { type -> OutcomeCallAdapter(type) }
            }
            else -> null
        }
    }

    private fun extractReturnType(responseType: ParameterizedType): Type? {
        // We only support ApiErrors for now on Outcome
        return when (getParameterUpperBound(0, responseType)) {
            ApiError::class.java -> {
                return getParameterUpperBound(1, responseType)
            }
            else -> null
        }
    }
}

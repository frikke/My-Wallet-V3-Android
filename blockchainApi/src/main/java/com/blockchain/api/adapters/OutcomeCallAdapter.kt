package com.blockchain.api.adapters

import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

class OutcomeCallAdapter<R>(
    private val successType: Type
) : CallAdapter<R, Call<Outcome<ApiError, R>>> {

    // Wrap the original Call into an OutcomeCall
    override fun adapt(call: Call<R>): Call<Outcome<ApiError, R>> = OutcomeCall(call, successType)

    override fun responseType(): Type = successType
}
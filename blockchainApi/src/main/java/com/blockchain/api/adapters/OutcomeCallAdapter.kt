package com.blockchain.api.adapters

import com.blockchain.outcome.Outcome
import java.lang.reflect.Type
import retrofit2.Call
import retrofit2.CallAdapter

class OutcomeCallAdapter<R>(
    private val successType: Type
) : CallAdapter<R, Call<Outcome<ApiException, R>>> {

    // Wrap the original Call into an OutcomeCall
    override fun adapt(call: Call<R>): Call<Outcome<ApiException, R>> = OutcomeCall(call, successType)

    override fun responseType(): Type = successType
}

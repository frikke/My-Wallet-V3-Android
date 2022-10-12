package com.blockchain.api.fraud

import com.blockchain.api.fraud.data.FraudFlowsResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.GET

internal interface FraudApi {
    @GET("user/risk/settings")
    suspend fun getFraudFlows(): Outcome<Exception, FraudFlowsResponse>
}

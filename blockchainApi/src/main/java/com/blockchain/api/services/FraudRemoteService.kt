package com.blockchain.api.services

import com.blockchain.api.fraud.FraudApi
import com.blockchain.api.fraud.data.FraudFlowsResponse
import com.blockchain.outcome.Outcome

class FraudRemoteService internal constructor(
    private val api: FraudApi
) {
    suspend fun getFraudFlows(): Outcome<Exception, FraudFlowsResponse> = api.getFraudFlows()
}

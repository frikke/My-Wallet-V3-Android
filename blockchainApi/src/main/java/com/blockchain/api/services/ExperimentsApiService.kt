package com.blockchain.api.services

import com.blockchain.api.experiments.ExperimentsApi
import com.blockchain.outcome.Outcome

class ExperimentsApiService internal constructor(
    private val api: ExperimentsApi
) {
    suspend fun getExperiments(): Outcome<Exception, Map<String, Int>> = api.getExperiments()
}

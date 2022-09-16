package com.blockchain.api.experiments

import com.blockchain.outcome.Outcome
import retrofit2.http.GET

internal interface ExperimentsApi {

    @GET("experiments")
    suspend fun getExperiments(): Outcome<Exception, Map<String, Int>>
}

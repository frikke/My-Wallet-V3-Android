package com.blockchain.api.mercuryexperiments

import com.blockchain.api.mercuryexperiments.data.MercuryExperimentsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

interface MercuryExperimentsApi {

    @GET("experiments/mercury")
    fun getMercuryExperiments(): Single<MercuryExperimentsResponse>
}

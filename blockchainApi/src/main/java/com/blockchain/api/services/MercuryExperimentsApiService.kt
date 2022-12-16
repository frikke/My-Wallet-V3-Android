package com.blockchain.api.services

import com.blockchain.api.mercuryexperiments.MercuryExperimentsApi
import com.blockchain.api.mercuryexperiments.data.MercuryExperimentsResponse
import io.reactivex.rxjava3.core.Single

class MercuryExperimentsApiService(
    private val api: MercuryExperimentsApi
) {

    fun getMercuryExperiments(): Single<MercuryExperimentsResponse> = api.getMercuryExperiments()
}

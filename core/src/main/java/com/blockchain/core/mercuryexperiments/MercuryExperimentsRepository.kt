package com.blockchain.core.mercuryexperiments

import com.blockchain.api.mercuryexperiments.data.MercuryExperimentsResponse
import com.blockchain.api.services.MercuryExperimentsApiService
import com.blockchain.core.mercuryexperiments.mapper.toDomain
import com.blockchain.domain.mercuryexperiments.MercuryExperimentsService
import com.blockchain.domain.mercuryexperiments.model.MercuryExperiments
import io.reactivex.rxjava3.core.Single

class MercuryExperimentsRepository(
    private val mercuryExperimentsApiService: MercuryExperimentsApiService
) : MercuryExperimentsService {

    override fun getMercuryExperiments(): Single<MercuryExperiments> =
        mercuryExperimentsApiService.getMercuryExperiments().map(MercuryExperimentsResponse::toDomain)
}

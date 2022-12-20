package com.blockchain.domain.mercuryexperiments

import com.blockchain.domain.mercuryexperiments.model.MercuryExperiments
import io.reactivex.rxjava3.core.Single

interface MercuryExperimentsService {

    fun getMercuryExperiments(): Single<MercuryExperiments>
}

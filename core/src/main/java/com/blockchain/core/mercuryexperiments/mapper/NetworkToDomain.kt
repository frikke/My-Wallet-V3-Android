package com.blockchain.core.mercuryexperiments.mapper

import com.blockchain.api.mercuryexperiments.data.MercuryExperimentsResponse
import com.blockchain.domain.mercuryexperiments.model.MercuryExperiments

fun MercuryExperimentsResponse.toDomain() = MercuryExperiments(
    walletAwarenessPrompt = walletAwarenessPrompt
)

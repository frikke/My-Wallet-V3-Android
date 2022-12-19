package com.blockchain.domain.mercuryexperiments.model

data class MercuryExperiments(
    val walletAwarenessPrompt: Int?
) {
    val isInExperiment: Boolean
        get() = walletAwarenessPrompt != null
}

package com.blockchain.api.services

import com.blockchain.api.txlimits.TxLimitsApi

class TxLimitsService(
    private val api: TxLimitsApi
) {

    fun getCrossborderLimits(
        outputCurrency: String,
        sourceCurrency: String,
        targetCurrency: String,
        sourceAccountType: String,
        targetAccountType: String
    ) = api.getCrossborderLimits(
        outputCurrency = outputCurrency,
        sourceCurrency = sourceCurrency,
        targetCurrency = targetCurrency,
        sourceAccountType = sourceAccountType,
        targetAccountType = targetAccountType
    )

    fun getFeatureLimits() = api.getFeatureLimits()
}

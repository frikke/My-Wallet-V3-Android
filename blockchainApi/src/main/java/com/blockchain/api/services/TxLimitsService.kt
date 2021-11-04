package com.blockchain.api.services

import com.blockchain.api.txlimits.TxLimitsApi

class TxLimitsService(
    private val api: TxLimitsApi
) {

    fun getSeamlessLimits(
        authHeader: String,
        outputCurrency: String,
        sourceCurrency: String,
        targetCurrency: String,
        sourceAccountType: String,
        targetAccountType: String
    ) = api.getSeamlessLimits(
        authorization = authHeader,
        outputCurrency = outputCurrency,
        sourceCurrency = sourceCurrency,
        targetCurrency = targetCurrency,
        sourceAccountType = sourceAccountType,
        targetAccountType = targetAccountType
    )
}
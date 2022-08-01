package com.blockchain.api.services

import com.blockchain.api.custodial.CustodialBalanceApi
import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import com.blockchain.api.wrapErrorMessage
import io.reactivex.rxjava3.core.Single

class CustodialBalanceService internal constructor(
    private val custodialBalanceApi: CustodialBalanceApi
) {
    fun getTradingBalanceForAllAssets(authHeader: String): Single<Map<String, TradingBalanceResponseDto>> =
        custodialBalanceApi
            .tradingBalanceForAllAssets(authHeader)
            .wrapErrorMessage()
}

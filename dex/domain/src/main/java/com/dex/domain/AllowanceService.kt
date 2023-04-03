package com.dex.domain

import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork

interface AllowanceService {
    suspend fun tokenAllowance(assetInfo: AssetInfo): Outcome<Exception, TokenAllowance>
    suspend fun buildAllowanceTransaction(assetInfo: AssetInfo): Outcome<Exception, AllowanceTransaction>
    suspend fun pushAllowanceTransaction(
        network: CoinNetwork,
        rawTx: String,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, String>
}

data class TokenAllowance(
    val allowanceAmount: String
)

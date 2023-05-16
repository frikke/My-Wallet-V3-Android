package com.dex.domain

import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Money
import kotlinx.serialization.json.JsonObject

interface AllowanceService {
    suspend fun tokenAllowance(assetInfo: AssetInfo): Outcome<Exception, TokenAllowance>
    suspend fun buildAllowanceTransaction(
        assetInfo: AssetInfo,
        amount: Money?
    ): Outcome<Exception, AllowanceTransaction>

    suspend fun pushAllowanceTransaction(
        network: CoinNetwork,
        rawTx: JsonObject,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, String>

    suspend fun allowanceTransactionProgress(assetInfo: AssetInfo): AllowanceTransactionState
    suspend fun revokeAllowanceTransactionProgress(assetInfo: AssetInfo): AllowanceTransactionState
}

data class TokenAllowance(
    private val allowanceAmount: String
) {
    val isTokenAllowed: Boolean
        get() = allowanceAmount != "0"
}

enum class AllowanceTransactionState {
    PENDING, FAILED, COMPLETED
}

private const val MAX_AMOUNT = "MAX"

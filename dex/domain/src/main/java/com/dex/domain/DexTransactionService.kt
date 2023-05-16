package com.dex.domain

import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.outcome.Outcome
import info.blockchain.balance.CoinNetwork
import kotlinx.serialization.json.JsonObject

interface DexTransactionService {
    suspend fun buildTx(dexTransaction: DexTransaction): Outcome<Exception, BuiltDexTransaction>
    suspend fun pushTx(
        coinNetwork: CoinNetwork,
        rawTx: JsonObject,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, String>
}

data class BuiltDexTransaction(
    val rawTx: JsonObject,
    val preImages: List<PreImage>
)

package com.dex.domain

import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.outcome.Outcome
import info.blockchain.balance.CoinNetwork

interface DexTransactionService {
    suspend fun buildTx(dexTransaction: DexTransaction): Outcome<Exception, BuiltDexTransaction>
    suspend fun pushTx(
        coinNetwork: CoinNetwork,
        rawTx: String,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, String>
}

data class BuiltDexTransaction(
    val rawTx: String,
    val preImages: List<PreImage>
)

package com.dex.domain

import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.ethereum.EvmNetworkPreImageSigner
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

class AllowanceTransactionProcessor(
    private val allowanceService: AllowanceService,
    private val evmNetworkSigner: EvmNetworkPreImageSigner
) {
    private lateinit var transaction: AllowanceTransaction
    suspend fun buildTx(assetInfo: AssetInfo): Outcome<Exception, AllowanceTransaction> {
        val transactionOutcome = allowanceService.buildAllowanceTransaction(
            assetInfo = assetInfo
        )
        (transactionOutcome as? Outcome.Success)?.value?.let {
            transaction = it
        }
        return transactionOutcome
    }

    suspend fun pushTx(): Outcome<Exception, String> {
        check(this::transaction.isInitialized)
        val transactionSignatures = transaction.preImages.map { unsignedPreImage ->
            evmNetworkSigner.signPreImage(unsignedPreImage)
        }
        val network = transaction.currencyToAllow.coinNetwork
        require(network != null)
        return allowanceService.pushAllowanceTransaction(
            network = network,
            signatures = transactionSignatures,
            rawTx = transaction.rawTx
        )
    }
}

data class AllowanceTransaction(
    val fees: Money,
    val currencyToAllow: AssetInfo,
    val rawTx: String,
    val preImages: List<PreImage>
)

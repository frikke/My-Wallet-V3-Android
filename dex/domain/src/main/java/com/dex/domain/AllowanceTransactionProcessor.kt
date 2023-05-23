package com.dex.domain

import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.ethereum.EvmNetworkPreImageSigner
import com.blockchain.outcome.Outcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import kotlinx.serialization.json.JsonObject

class AllowanceTransactionProcessor(
    private val allowanceService: AllowanceService,
    private val evmNetworkSigner: EvmNetworkPreImageSigner
) {
    private lateinit var transaction: AllowanceTransaction
    suspend fun buildTx(assetInfo: AssetInfo, amount: Money? = null): Outcome<Exception, AllowanceTransaction> {
        val transactionOutcome = allowanceService.buildAllowanceTransaction(
            assetInfo = assetInfo,
            amount = amount
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
            assetInfo = transaction.currencyToAllow,
            rawTx = transaction.rawTx
        )
    }

    suspend fun revokeAllowance(assetInfo: AssetInfo) {
        val build = buildTx(assetInfo, Money.zero(assetInfo))
        (build as? Outcome.Success)?.value?.let { builtTx ->
            val transactionSignatures = builtTx.preImages.map { unsignedPreImage ->
                evmNetworkSigner.signPreImage(unsignedPreImage)
            }
            val network = builtTx.currencyToAllow.coinNetwork
            require(network != null)
            allowanceService.pushAllowanceTransaction(
                network = network,
                assetInfo = assetInfo,
                signatures = transactionSignatures,
                rawTx = builtTx.rawTx
            )
        }
    }
}

data class AllowanceTransaction(
    val fees: Money,
    val currencyToAllow: AssetInfo,
    val rawTx: JsonObject,
    val preImages: List<PreImage>
)

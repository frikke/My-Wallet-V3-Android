package com.dex.data

import com.blockchain.api.dex.DexTransactionsApiService
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.dex.domain.BuiltDexTransaction
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionService
import info.blockchain.balance.CoinNetwork
import kotlinx.serialization.json.JsonObject

class DexTransactionRepository(
    private val apiService: DexTransactionsApiService,
    private val nonCustodialService: NonCustodialService,
) : DexTransactionService {
    override suspend fun buildTx(dexTransaction: DexTransaction): Outcome<Exception, BuiltDexTransaction> {
        val quote = dexTransaction.quote
        require(quote != null) {
            "Cannot build a transaction without a quote"
        }
        val coinNetwork = dexTransaction.sourceAccount.currency.coinNetwork
        require(coinNetwork != null)

        return apiService.buildDexSwapTx(
            destination = quote.destinationContractAddress,
            data = quote.data,
            value = quote.value,
            fee = quote.gasPrice,
            gasLimit = quote.gasLimit,
            networkNativeCurrency = coinNetwork.nativeAssetTicker,
        ).map { builtDexTxResponse ->
            BuiltDexTransaction(
                rawTx = builtDexTxResponse.rawTx,
                preImages = builtDexTxResponse.preImages.map {
                    PreImage(
                        rawPreImage = it.rawPreImage,
                        signingKey = it.signingKey,
                        signatureAlgorithm = it.signatureAlgorithm,
                        descriptor = it.descriptor
                    )
                }
            )
        }
    }

    override suspend fun pushTx(
        coinNetwork: CoinNetwork,
        rawTx: JsonObject,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, String> {
        return nonCustodialService.pushTransaction(
            signatures = signatures,
            rawTx = rawTx,
            currency = coinNetwork.nativeAssetTicker
        ).map {
            it.txId
        }
    }
}

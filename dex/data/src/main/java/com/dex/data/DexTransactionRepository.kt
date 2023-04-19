package com.dex.data

import com.blockchain.api.dex.DexTransactionsApiService
import com.blockchain.api.dex.PubKeySource
import com.blockchain.api.dex.RawTxResponse
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.dex.domain.BuiltDexTransaction
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionService
import info.blockchain.balance.CoinNetwork
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class DexTransactionRepository(
    private val apiService: DexTransactionsApiService,
    private val networkAccountsService: NetworkAccountsService,
    private val nonCustodialService: NonCustodialService,
) : DexTransactionService {
    override suspend fun buildTx(dexTransaction: DexTransaction): Outcome<Exception, BuiltDexTransaction> {
        val quote = dexTransaction.quote
        require(quote != null) {
            "Cannot build a transaction without a quote"
        }
        val coinNetwork = dexTransaction.sourceAccount.currency.coinNetwork
        require(coinNetwork != null)
        val network = networkAccountsService.allNetworkWallets()
            .first {
                it.currency.networkTicker == coinNetwork.nativeAssetTicker
            }

        return apiService.buildDexSwapTx(
            destination = quote.destinationContractAddress,
            data = quote.data,
            sources = network.publicKey().map {
                PubKeySource(
                    pubKey = it.address,
                    style = it.style.name,
                    descriptor = "legacy"
                )
            },
            value = quote.value,
            gasLimit = quote.gasLimit,
            network = coinNetwork.networkTicker
        ).map { builtDexTxResponse ->
            BuiltDexTransaction(
                rawTx = Json.encodeToString(RawTxResponse.serializer(), builtDexTxResponse.rawTx),
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
        rawTx: String,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, String> {
        return nonCustodialService.pushTransaction(
            signatures = signatures,
            rawTx = Json.decodeFromString(JsonObject.serializer(), rawTx),
            currency = coinNetwork.networkTicker
        ).map {
            it.txId
        }
    }
}

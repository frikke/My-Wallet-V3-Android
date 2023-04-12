package com.dex.data

import com.blockchain.DefiWalletReceiveAddressService
import com.blockchain.api.dex.DexTransactionsApiService
import com.blockchain.api.dex.PubKeySource
import com.blockchain.api.dex.RawTxResponse
import com.blockchain.coincore.eth.GasFeeCalculator
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.store.firstOutcome
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.dex.domain.AllowanceService
import com.dex.domain.AllowanceTransaction
import com.dex.domain.TokenAllowance
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import java.math.BigInteger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class DexAllowanceRepository(
    private val apiService: DexTransactionsApiService,
    private val dexAllowanceStorage: DexAllowanceStorage,
    private val defiAccountReceiveAddressService: DefiWalletReceiveAddressService,
    private val networkAccountsService: NetworkAccountsService,
    private val nonCustodialService: NonCustodialService,
    private val assetCatalogue: AssetCatalogue,
    private val gasFeeCalculator: GasFeeCalculator
) : AllowanceService {
    override suspend fun tokenAllowance(assetInfo: AssetInfo): Outcome<Exception, TokenAllowance> {
        val address = defiAccountReceiveAddressService.receiveAddress(assetInfo)
        return address.flatMap {
            val contractAddress = assetInfo.l2identifier ?: return@flatMap Outcome.Failure(
                IllegalArgumentException(
                    "Currency is missing network address"
                )
            )
            dexAllowanceStorage.stream(
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale).withKey(
                    AllowanceKey(
                        address = it.address,
                        currencyContract = contractAddress,
                        networkSymbol = assetInfo.coinNetwork!!.nativeAssetTicker
                    )
                )
            ).firstOutcome().map { resp ->
                TokenAllowance(
                    resp.result.allowance
                )
            }
        }
    }

    override suspend fun buildAllowanceTransaction(assetInfo: AssetInfo): Outcome<Exception, AllowanceTransaction> {
        val address = defiAccountReceiveAddressService.receiveAddress(assetInfo)
        val contractAddress = assetInfo.l2identifier
        val coinNetwork = assetInfo.coinNetwork
        require(coinNetwork != null)
        val nativeAsset = assetCatalogue.fromNetworkTicker(coinNetwork.nativeAssetTicker)
        require(nativeAsset != null)
        val network = networkAccountsService.allNetworkWallets()
            .first {
                it.currency.networkTicker == coinNetwork.nativeAssetTicker
            }

        require(contractAddress != null)
        return address.flatMap {
            apiService.buildAllowanceTx(
                destination = contractAddress.lowercase(),
                sources = network.publicKey().map {
                    PubKeySource(
                        pubKey = it.address,
                        style = it.style.name,
                        descriptor = "legacy"
                    )
                },
                network = coinNetwork.networkTicker
            ).map { buildTxResponse ->
                val gasPrice = BigInteger(buildTxResponse.rawTx.payload.gasPrice.hex.removePrefix("0x"), 16)
                val gasLimit = BigInteger(buildTxResponse.rawTx.payload.gasLimit.hex.removePrefix("0x"), 16)

                AllowanceTransaction(
                    fees = gasFeeCalculator.fee(
                        nativeAsset = nativeAsset,
                        gasPriceWei = gasPrice,
                        gasLimit = gasLimit,
                    ),
                    rawTx = Json.encodeToString(RawTxResponse.serializer(), buildTxResponse.rawTx),
                    preImages = buildTxResponse.preImages.map {
                        PreImage(
                            rawPreImage = it.rawPreImage,
                            signingKey = it.signingKey,
                            signatureAlgorithm = it.signatureAlgorithm,
                            descriptor = it.descriptor
                        )
                    },
                    currencyToAllow = assetInfo
                )
            }
        }
    }

    override suspend fun pushAllowanceTransaction(
        network: CoinNetwork,
        rawTx: String,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, String> =
        nonCustodialService.pushTransaction(
            currency = network.nativeAssetTicker,
            rawTx = Json.decodeFromString(JsonObject.serializer(), rawTx),
            signatures = signatures
        ).map {
            it.txId
        }
}

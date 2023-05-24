package com.dex.data

import com.blockchain.DefiWalletReceiveAddressService
import com.blockchain.api.dex.DexTransactionsApiService
import com.blockchain.api.dex.GasValuesPayload
import com.blockchain.coincore.eth.GasFeeCalculator
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.firstOutcome
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.preferences.DexPrefs
import com.dex.domain.AllowanceService
import com.dex.domain.AllowanceTransaction
import com.dex.domain.AllowanceTransactionState
import com.dex.domain.TokenAllowance
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Money
import java.math.BigInteger
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class DexAllowanceRepository(
    private val apiService: DexTransactionsApiService,
    private val dexAllowanceStorage: DexAllowanceStorage,
    private val defiAccountReceiveAddressService: DefiWalletReceiveAddressService,
    private val nonCustodialService: NonCustodialService,
    private val json: Json,
    private val dexPrefs: DexPrefs,
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
                        networkSymbol = assetInfo.coinNetwork!!.networkTicker
                    )
                )
            ).firstOutcome().map { resp ->
                TokenAllowance(
                    resp.result.allowance
                )
            }.doOnSuccess { alowance ->
                if (alowance.isTokenAllowed) {
                    dexPrefs.allowanceApprovedButPendingTokens =
                        dexPrefs.allowanceApprovedButPendingTokens.minus(assetInfo.networkTicker)
                }
            }
        }
    }

    override suspend fun buildAllowanceTransaction(
        assetInfo: AssetInfo,
        amount: Money?
    ): Outcome<Exception, AllowanceTransaction> {
        val address = defiAccountReceiveAddressService.receiveAddress(assetInfo)
        val contractAddress = assetInfo.l2identifier
        val coinNetwork = assetInfo.coinNetwork
        require(coinNetwork != null)
        val nativeAsset = assetCatalogue.fromNetworkTicker(coinNetwork.nativeAssetTicker)
        require(nativeAsset != null)

        require(contractAddress != null)
        return address.flatMap {
            apiService.buildAllowanceTx(
                destination = contractAddress.lowercase(),
                amount = amount?.toBigInteger()?.toString() ?: "MAX",
                network = coinNetwork.networkTicker
            ).map { buildTxResponse ->
                val gasValuesPayload = json.decodeFromJsonElement(GasValuesPayload.serializer(), buildTxResponse.rawTx)
                val gasPrice = gasValuesPayload.payload?.gasPrice?.let {
                    BigInteger(it.hex.removePrefix("0x"), 16)
                } ?: BigInteger.ZERO
                val gasLimit = gasValuesPayload.payload?.gasLimit?.let {
                    BigInteger(it.hex.removePrefix("0x"), 16)
                } ?: BigInteger.ZERO

                AllowanceTransaction(
                    fees = gasFeeCalculator.fee(
                        nativeAsset = nativeAsset,
                        gasPriceWei = gasPrice,
                        gasLimit = gasLimit
                    ),
                    rawTx = buildTxResponse.rawTx,
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
        rawTx: JsonObject,
        assetInfo: AssetInfo,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, String> =
        nonCustodialService.pushTransaction(
            currency = network.nativeAssetTicker,
            rawTx = rawTx,
            signatures = signatures
        ).map {
            it.txId
        }.doOnSuccess {
            dexPrefs.allowanceApprovedButPendingTokens = dexPrefs.allowanceApprovedButPendingTokens.plus(
                assetInfo.networkTicker
            )
        }

    private suspend fun pollForAllowanceState(
        assetInfo: AssetInfo,
        checkState: (TokenAllowance) -> Boolean
    ): AllowanceTransactionState {
        val startTimeMillis = System.currentTimeMillis()
        val timeoutMillis = 320_000L
        var elapsedTimeMillis = 0L
        while (elapsedTimeMillis < timeoutMillis) {
            val allowance = tokenAllowance(assetInfo)
            (allowance as? Outcome.Success)?.value?.let {
                if (checkState(it)) {
                    return AllowanceTransactionState.COMPLETED
                }
            }
            delay(1000L)
            elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis
        }
        return AllowanceTransactionState.PENDING
    }

    override suspend fun allowanceTransactionProgress(assetInfo: AssetInfo): AllowanceTransactionState {
        return pollForAllowanceState(assetInfo) { it.isTokenAllowed }.also {
            if (it == AllowanceTransactionState.COMPLETED) {
                dexPrefs.allowanceApprovedButPendingTokens =
                    dexPrefs.allowanceApprovedButPendingTokens.minus(assetInfo.networkTicker)
            }
        }
    }

    override suspend fun isAllowanceApprovedButPending(assetInfo: AssetInfo): Boolean {
        return assetInfo.networkTicker in dexPrefs.allowanceApprovedButPendingTokens
    }

    override suspend fun revokeAllowanceTransactionProgress(assetInfo: AssetInfo): AllowanceTransactionState {
        return pollForAllowanceState(assetInfo) { !it.isTokenAllowed }
    }
}

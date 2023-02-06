package com.blockchain.core.chains.dynamicselfcustody.data

import com.blockchain.api.selfcustody.BuildTxResponse
import com.blockchain.api.selfcustody.PushTxResponse
import com.blockchain.api.selfcustody.Signature
import com.blockchain.api.selfcustody.Status
import com.blockchain.api.selfcustody.TransactionDirection
import com.blockchain.api.selfcustody.TransactionResponse
import com.blockchain.api.services.DynamicSelfCustodyService
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialAccountBalance
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialDerivedAddress
import com.blockchain.core.chains.dynamicselfcustody.domain.model.NonCustodialTxHistoryItem
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.wallet.CoinType
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.asSingle
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.bitcoinj.core.Sha256Hash
import org.spongycastle.util.encoders.Hex

internal class NonCustodialRepository(
    private val dynamicSelfCustodyService: DynamicSelfCustodyService,
    private val currencyPrefs: CurrencyPrefs,
    private val subscriptionsStore: NonCustodialSubscriptionsStore,
    private val assetCatalogue: AssetCatalogue,
    private val coinTypeStore: CoinTypeStore
) : NonCustodialService {

    private val supportedCoins: Single<Map<String, CoinType>>
        get() = getAllSupportedCoins()

    private fun getAllSupportedCoins(): Single<Map<String, CoinType>> {
        return coinTypeStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .asSingle().map { coinTypes ->
                coinTypes.map { coinTypeDto ->
                    coinTypeDto.derivations.map { derivationDto ->
                        CoinType(
                            network = coinTypeDto.type,
                            type = derivationDto.coinType,
                            purpose = derivationDto.purpose
                        )
                    }
                }
                    .flatten()
                    .associateBy { it.network.name }
            }
    }

    override suspend fun getCoinTypeFor(currency: Currency): CoinType? {
        return supportedCoins.await()[currency.networkTicker]
    }

    override fun getSubscriptions(refreshStrategy: FreshnessStrategy): Flow<Outcome<Exception, List<String>>> {
        return subscriptionsStore.stream(refreshStrategy)
            .filter { it !is DataResource.Loading }
            .map { response ->
                when (response) {
                    is DataResource.Data -> Outcome.Success(response.data.currencies.map { it.ticker })
                    is DataResource.Loading -> throw IllegalStateException("Subscriptions not loaded")
                    is DataResource.Error -> Outcome.Failure(response.error)
                }
            }
    }

    override suspend fun subscribe(
        currency: String,
        label: String,
        addresses: List<String>
    ): Outcome<Exception, Boolean> =
        dynamicSelfCustodyService.subscribe(
            currency = currency,
            accountName = label,
            addresses = addresses
        )
            .map { it.success }

    override suspend fun unsubscribe(currency: String): Outcome<Exception, Boolean> =
        dynamicSelfCustodyService.unsubscribe(
            currency = currency
        )
            .map { it.success }

    override suspend fun getBalances(currencies: List<String>):
        Outcome<Exception, List<NonCustodialAccountBalance>> =
        dynamicSelfCustodyService.getBalances(
            currencies = currencies,
            fiatCurrency = currencyPrefs.selectedFiatCurrency.networkTicker
        ).map { balancesResponse ->
            balancesResponse.balances.mapNotNull { balanceResponse ->
                NonCustodialAccountBalance(
                    networkTicker = balanceResponse.currency,
                    amount = balanceResponse.balance?.amount ?: return@mapNotNull null,
                    pending = balanceResponse.pending?.amount ?: return@mapNotNull null,
                    price = balanceResponse.price
                )
            }
        }

    override suspend fun getAddresses(currencies: List<String>):
        Outcome<Exception, List<NonCustodialDerivedAddress>> =
        dynamicSelfCustodyService.getAddresses(
            currencies = currencies
        ).map { addressesResponse ->
            addressesResponse.addressEntries.map { addressEntry ->
                addressEntry.addresses.map { derivedAddress ->
                    NonCustodialDerivedAddress(
                        pubKey = derivedAddress.pubKey,
                        address = derivedAddress.address,
                        includesMemo = derivedAddress.includesMemo,
                        format = derivedAddress.format,
                        default = derivedAddress.default,
                        accountIndex = addressEntry.accountInfo.index
                    )
                }
            }.flatten()
        }

    override suspend fun getTransactionHistory(
        currency: String,
        contractAddress: String?
    ): Outcome<Exception, List<NonCustodialTxHistoryItem>> =
        dynamicSelfCustodyService.getTransactionHistory(
            currency = currency,
            contractAddress = contractAddress
        ).map { response ->
            response.history.map { historyItem ->
                historyItem.toHistoryEvent()
            }
        }

    override suspend fun buildTransaction(
        currency: String,
        accountIndex: Int,
        type: String,
        transactionTarget: String,
        amount: String,
        fee: String,
        memo: String,
        feeCurrency: String
    ): Outcome<Exception, BuildTxResponse> =
        dynamicSelfCustodyService.buildTransaction(
            currency,
            accountIndex,
            type,
            transactionTarget,
            amount,
            fee,
            memo
        )

    override fun getFeeCurrencyFor(asset: AssetInfo): AssetInfo =
        asset.coinNetwork?.nativeAssetTicker?.let { ticker ->
            (assetCatalogue.fromNetworkTicker(ticker) as? AssetInfo) ?: asset
        } ?: asset

    override suspend fun pushTransaction(
        currency: String,
        rawTx: JsonObject,
        signatures: List<TransactionSignature>
    ): Outcome<Exception, PushTxResponse> =
        dynamicSelfCustodyService.pushTransaction(
            currency = currency,
            rawTx = rawTx,
            signatures = signatures.map {
                Signature(
                    preImage = it.preImage,
                    signingKey = it.signingKey,
                    signatureAlgorithm = it.signatureAlgorithm,
                    signature = it.signature
                )
            }
        )

    private fun getHashedString(input: String): String = String(Hex.encode(Sha256Hash.hash(input.toByteArray())))

    companion object {
        private val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        const val COIN_CONFIGURATIONS = "android_ff_coin_configurations"
    }
}

private fun TransactionResponse.toHistoryEvent(): NonCustodialTxHistoryItem {
    val sourceAddress = movements.firstOrNull { transactionMovement ->
        transactionMovement.type == TransactionDirection.SENT
    }?.address ?: ""
    val targetAddress = movements.firstOrNull { transactionMovement ->
        transactionMovement.type == TransactionDirection.RECEIVED
    }?.address ?: ""

    return NonCustodialTxHistoryItem(
        txId = txId,
        value = movements.first().amount,
        from = sourceAddress,
        to = targetAddress,
        timestamp = timestamp,
        fee = fee,
        status = status ?: Status.PENDING
    )
}

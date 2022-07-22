package com.blockchain.core.chains.dynamicselfcustody.data

import com.blockchain.api.adapters.ApiError
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
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.StoreRequest
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import org.bitcoinj.core.Sha256Hash
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class NonCustodialRepository(
    private val subscriptionsStore: NonCustodialSubscriptionsStore,
    private val dynamicSelfCustodyService: DynamicSelfCustodyService,
    private val payloadDataManager: PayloadDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val assetCatalogue: AssetCatalogue
) : NonCustodialService {

    override suspend fun authenticate(): Outcome<ApiError, Boolean> =
        dynamicSelfCustodyService.authenticate(
            guid = payloadDataManager.guid,
            sharedKey = getHashedString(payloadDataManager.sharedKey)
        )
            .map { it.success }

    override suspend fun subscribe(
        currency: String,
        label: String,
        addresses: List<String>
    ): Outcome<ApiError, Boolean> =
        dynamicSelfCustodyService.subscribe(
            guidHash = getHashedString(payloadDataManager.guid),
            sharedKeyHash = getHashedString(payloadDataManager.sharedKey),
            currency = currency,
            accountName = label,
            addresses = addresses
        )
            .map { it.success }

    override suspend fun unsubscribe(currency: String): Outcome<ApiError, Boolean> =
        dynamicSelfCustodyService.unsubscribe(
            guidHash = getHashedString(payloadDataManager.guid),
            sharedKeyHash = getHashedString(payloadDataManager.sharedKey),
            currency = currency
        )
            .map { it.success }

    override fun getSubscriptions(request: StoreRequest): Flow<List<String>> {
        return subscriptionsStore.stream(request)
            .mapData { subscriptionsResponse ->
                subscriptionsResponse.currencies.map { it.ticker }
            }
            .getDataOrThrow()
    }

    override suspend fun getBalances(currencies: List<String>): Outcome<ApiError, List<NonCustodialAccountBalance>> =
        dynamicSelfCustodyService.getBalances(
            guidHash = getHashedString(payloadDataManager.guid),
            sharedKeyHash = getHashedString(payloadDataManager.sharedKey),
            currencies = currencies,
            fiatCurrency = currencyPrefs.selectedFiatCurrency.networkTicker
        ).map { balancesResponse ->
            balancesResponse.balances.map { balanceResponse ->
                NonCustodialAccountBalance(
                    networkTicker = balanceResponse.currency,
                    amount = balanceResponse.balance.amount,
                    pending = balanceResponse.pending.amount,
                    price = balanceResponse.price
                )
            }
        }

    override suspend fun getAddresses(currencies: List<String>): Outcome<ApiError, List<NonCustodialDerivedAddress>> =
        dynamicSelfCustodyService.getAddresses(
            guidHash = getHashedString(payloadDataManager.guid),
            sharedKeyHash = getHashedString(payloadDataManager.sharedKey),
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
    ): Outcome<ApiError, List<NonCustodialTxHistoryItem>> =
        dynamicSelfCustodyService.getTransactionHistory(
            guidHash = getHashedString(payloadDataManager.guid),
            sharedKeyHash = getHashedString(payloadDataManager.sharedKey),
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
    ): Outcome<ApiError, BuildTxResponse> =
        dynamicSelfCustodyService.buildTransaction(
            getHashedString(payloadDataManager.guid),
            getHashedString(payloadDataManager.sharedKey),
            currency,
            accountIndex,
            type,
            transactionTarget,
            amount,
            fee,
            memo
        )

    override fun getFeeCurrencyFor(asset: AssetInfo): AssetInfo =
        asset.l1chainTicker?.let { ticker ->
            (assetCatalogue.fromNetworkTicker(ticker) as? AssetInfo) ?: asset
        } ?: asset

    override suspend fun pushTransaction(
        currency: String,
        rawTx: JsonObject,
        signatures: List<TransactionSignature>
    ): Outcome<ApiError, PushTxResponse> =
        dynamicSelfCustodyService.pushTransaction(
            guidHash = getHashedString(payloadDataManager.guid),
            sharedKeyHash = getHashedString(payloadDataManager.sharedKey),
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

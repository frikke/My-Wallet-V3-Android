package com.blockchain.coincore.selfcustody

import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.dynamicselfcustody.NonCustodialService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.getOrThrow
import com.blockchain.outcome.map
import com.blockchain.preferences.WalletStatusPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.wallet.dynamicselfcustody.CoinConfiguration
import info.blockchain.wallet.dynamicselfcustody.DynamicHDAccount
import info.blockchain.wallet.keys.SigningKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.rx3.rxSingle
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import timber.log.Timber

class DynamicNonCustodialAccount(
    val payloadManager: PayloadDataManager,
    assetInfo: AssetInfo,
    coinConfiguration: CoinConfiguration,
    override val addressResolver: AddressResolver,
    private val nonCustodialService: NonCustodialService,
    override val exchangeRates: ExchangeRatesDataManager,
    override val label: String,
    private val walletPreferences: WalletStatusPrefs
) : CryptoNonCustodialAccount(assetInfo) {

    private val internalAccount: DynamicHDAccount = payloadManager.getDynamicHdAccount(coinConfiguration)
        ?: throw IllegalStateException("Unsupported Coin Configuration!")

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val receiveAddress: Single<ReceiveAddress>
        get() = rxSingle { getReceiveAddress() }

    private suspend fun getReceiveAddress() = nonCustodialService.getAddresses(listOf(currency.networkTicker))
        .getOrThrow().find {
            it.pubKey == String(Hex.encode(internalAccount.address.pubKey)) && it.default
        }?.let { nonCustodialDerivedAddress ->
            DynamicNonCustodialAddress(
                address = nonCustodialDerivedAddress.address,
                asset = currency
            )
        } ?: throw IllegalStateException("Couldn't derive receive address for ${currency.networkTicker}")

    override fun getOnChainBalance(): Observable<Money> = rxSingle {
        // Check if we are subscribed to the given currency.
        val subscriptions = nonCustodialService.getSubscriptions().getOrDefault(emptyList())
        if (subscriptions.contains(currency.networkTicker)) {
            // Get the balance if we found the currency in the subscriptions
            getBalance().getOrDefault(Money.fromMajor(currency, BigDecimal.ZERO))
        } else {
            // If not, we need to subscribe. However if the list of subscriptions is empty then it's the first time
            // we're calling this endpoint. In that case we also need to authenticate.
            subscribeToBalance(authRequired = subscriptions.isEmpty()).flatMap {
                getBalance()
            }.getOrDefault(Money.fromMajor(currency, BigDecimal.ZERO))
        }
    }
        .doOnSuccess { hasFunds.set(it.isPositive) }
        .toObservable()

    private suspend fun subscribeToBalance(authRequired: Boolean) =
        if (authRequired) {
            nonCustodialService.authenticate().flatMap {
                nonCustodialService.subscribe(
                    currency = currency.networkTicker,
                    label = label,
                    addresses = listOf(String(Hex.encode(internalAccount.address.pubKey)))
                )
            }
        } else {
            nonCustodialService.subscribe(
                currency = currency.networkTicker,
                label = label,
                addresses = listOf(String(Hex.encode(internalAccount.address.pubKey)))
            )
        }
            .doOnFailure {
                Timber.e(it.exception)
            }

    private suspend fun getBalance() = nonCustodialService.getBalances(listOf(currency.networkTicker))
        .map { accountBalances ->
            accountBalances.firstOrNull { it.networkTicker == currency.networkTicker }?.let { balance ->
                Money.fromMinor(currency, balance.amount)
            } ?: Money.fromMajor(currency, BigDecimal.ZERO)
        }

    override val isArchived: Boolean = false

    override val isDefault: Boolean = true

    override val activity: Single<ActivitySummaryList> = rxSingleOutcome {
        val accountAddress = getReceiveAddress()
        nonCustodialService.getTransactionHistory(
            currency = currency.networkTicker,
            contractAddress = currency.l2identifier
        )
            .map { history ->
                history.map { item ->
                    DynamicActivitySummaryItem(
                        asset = currency,
                        event = item,
                        accountAddress = accountAddress.address,
                        exchangeRates = exchangeRates,
                        account = this@DynamicNonCustodialAccount
                    )
                }
            }
    }

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        DynamicOnChanTxEngine(
            nonCustodialService = nonCustodialService,
            walletPreferences = walletPreferences,
            requireSecondPassword = false,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )

    override fun updateLabel(newLabel: String): Completable {
        return Completable.complete()
    }

    override fun archive(): Completable = Completable.complete()

    override fun unarchive(): Completable = Completable.complete()

    override fun setAsDefault(): Completable = Completable.complete()

    override val xpubAddress: String
        get() = internalAccount.bitcoinSerializedBase58Address

    override val hasStaticAddress: Boolean = false

    fun getSigningKey(): SigningKey {
        return internalAccount.signingKey
    }
}

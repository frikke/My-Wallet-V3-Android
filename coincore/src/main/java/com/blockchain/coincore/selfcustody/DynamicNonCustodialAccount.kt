package com.blockchain.coincore.selfcustody

import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.dynamicselfcustody.domain.NonCustodialService
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.wallet.CoinType
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.getOrThrow
import com.blockchain.outcome.map
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.DEFAULT_ADDRESS_DESCRIPTOR
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.wallet.dynamicselfcustody.DynamicHDAccount
import info.blockchain.wallet.keys.SigningKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx3.rxSingle
import org.spongycastle.util.encoders.Hex

class DynamicNonCustodialAccount(
    val payloadManager: PayloadDataManager,
    assetInfo: AssetInfo,
    coinType: CoinType,
    override val addressResolver: AddressResolver,
    private val nonCustodialService: NonCustodialService,
    override val exchangeRates: ExchangeRatesDataManager,
    override val label: String,
    private val walletPreferences: WalletStatusPrefs
) : CryptoNonCustodialAccount(assetInfo), NetworkWallet {

    private val internalAccount: DynamicHDAccount = payloadManager.getDynamicHdAccount(coinType)
        ?: throw IllegalStateException("Unsupported Coin Configuration!")

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

    override fun getOnChainBalance(): Observable<out Money> = rxSingle {
        // Check if we are subscribed to the given currency.
        val subscriptions = nonCustodialService.getSubscriptions().first().getOrDefault(emptyList())

        if (subscriptions.contains(currency.networkTicker)) {
            // Get the balance if we found the currency in the subscriptions
            getBalance().getOrDefault(Money.fromMajor(currency, BigDecimal.ZERO))
        } else {
            // If not, we need to subscribe. However if the list of subscriptions is empty then it's the first time
            // we're calling this endpoint. In that case we also need to authenticate.
            subscribeToBalance().flatMap {
                getBalance()
            }.getOrDefault(Money.fromMajor(currency, BigDecimal.ZERO))
        }
    }
        .toObservable()

    private suspend fun getBalance() = nonCustodialService.getBalances(listOf(currency.networkTicker))
        .map { accountBalances ->
            accountBalances.firstOrNull { it.networkTicker == currency.networkTicker }?.let { balance ->
                Money.fromMinor(currency, balance.amount)
            } ?: Money.fromMajor(currency, BigDecimal.ZERO)
        }

    private suspend fun subscribeToBalance(): Outcome<Exception, Boolean> =
        nonCustodialService.subscribe(
            currency = currency.networkTicker,
            label = label,
            addresses = listOf(String(Hex.encode(internalAccount.address.pubKey)))
        )

    override val index: Int
        get() = 0

    override suspend fun publicKey(): List<PublicKey> =
        listOf(
            PublicKey(
                address = String(Hex.encode(internalAccount.address.pubKey)),
                descriptor = DEFAULT_ADDRESS_DESCRIPTOR,
                style = PubKeyStyle.SINGLE,
            )
        )
}

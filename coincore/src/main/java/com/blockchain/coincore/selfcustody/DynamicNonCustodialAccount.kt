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
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.outcome.getOrThrow
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.wallet.dynamicselfcustody.CoinConfiguration
import info.blockchain.wallet.dynamicselfcustody.DynamicHDAccount
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.rx3.rxSingle
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class DynamicNonCustodialAccount(
    payloadManager: PayloadDataManager,
    assetInfo: AssetInfo,
    coinConfiguration: CoinConfiguration,
    custodialWalletManager: CustodialWalletManager,
    identity: UserIdentity,
    override val addressResolver: AddressResolver,
    private val nonCustodialService: NonCustodialService,
    override val exchangeRates: ExchangeRatesDataManager,
    override val label: String
) : CryptoNonCustodialAccount(payloadManager, assetInfo, custodialWalletManager, identity) {

    private val internalAccount: DynamicHDAccount = payloadDataManager.getDynamicHdAccount(coinConfiguration)
        ?: throw IllegalStateException("Unsupported Coin Configuration!")

    override val baseActions: Set<AssetAction> = defaultActions

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val receiveAddress: Single<ReceiveAddress>
        get() {
            return rxSingle {
                nonCustodialService.getAddresses(listOf(currency.networkTicker))
                    .getOrThrow().find {
                        it.pubKey == String(Hex.encode(internalAccount.address.pubKey)) && it.default
                    }?.let { nonCustodialDerivedAddress ->
                        DynamicNonCustodialAddress(
                            address = nonCustodialDerivedAddress.address,
                            asset = currency
                        )
                    } ?: throw IllegalStateException("Couldn't derive receive address for ${currency.networkTicker}")
            }
        }

    override fun getOnChainBalance(): Observable<Money> = Observable.just(Money.fromMajor(currency, BigDecimal.ZERO))

    override val isArchived: Boolean = false

    override val isDefault: Boolean = true

    override val activity: Single<ActivitySummaryList> = Single.just(listOf())

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        DynamicOnChanTxEngine()

    override fun updateLabel(newLabel: String): Completable {
        return Completable.complete()
    }

    override fun archive(): Completable = Completable.complete()

    override fun unarchive(): Completable = Completable.complete()

    override fun setAsDefault(): Completable = Completable.complete()

    override val xpubAddress: String
        get() = internalAccount.bitcoinSerializedBase58Address

    override val hasStaticAddress: Boolean = false
}

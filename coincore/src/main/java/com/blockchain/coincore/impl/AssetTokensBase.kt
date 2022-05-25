package com.blockchain.coincore.impl

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TradingAccount
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import exchange.ExchangeLinking
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import java.util.concurrent.atomic.AtomicBoolean
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

interface AccountRefreshTrigger {
    fun forceAccountsRefresh()
}

/*internal*/ abstract class CryptoAssetBase internal constructor(
    protected val payloadManager: PayloadDataManager,
    protected val exchangeRates: ExchangeRatesDataManager,
    protected val currencyPrefs: CurrencyPrefs,
    protected val labels: DefaultLabels,
    protected val custodialManager: CustodialWalletManager,
    private val interestBalance: InterestBalanceDataManager,
    protected val tradingBalances: TradingBalanceDataManager,
    private val exchangeLinking: ExchangeLinking,
    protected val remoteLogger: RemoteLogger,
    protected val identity: UserIdentity,
    protected val addressResolver: AddressResolver
) : CryptoAsset, AccountRefreshTrigger {

    private val activeAccounts: ActiveAccountList by unsafeLazy {
        ActiveAccountList(assetInfo, custodialManager)
    }

    protected val accounts: Single<SingleAccountList>
        get() = activeAccounts.fetchAccountList(::loadAccounts)
            .flatMap {
                updateLabelsIfNeeded(it).toSingle { it }
            }

    private fun updateLabelsIfNeeded(list: SingleAccountList): Completable =
        Completable.concat(
            list.map {
                val cryptoNonCustodialAccount = it as? CryptoNonCustodialAccount
                if (cryptoNonCustodialAccount?.labelNeedsUpdate() == true) {
                    cryptoNonCustodialAccount.updateLabel(
                        cryptoNonCustodialAccount.label.replace(
                            labels.getOldDefaultNonCustodialWalletLabel(assetInfo as AssetInfo),
                            labels.getDefaultNonCustodialWalletLabel()
                        )
                    ).doOnError { error ->
                        remoteLogger.logException(error)
                    }.onErrorComplete()
                } else {
                    Completable.complete()
                }
            }
        )

    private fun loadAccounts(): Single<SingleAccountList> =
        Single.zip(
            loadNonCustodialAccounts(labels),
            loadCustodialAccounts(),
            loadInterestAccounts()
        ) { nc, c, i ->
            nc + c + i
        }.doOnError {
            val errorMsg = "Error loading accounts for ${assetInfo.networkTicker}"
            Timber.e("$errorMsg: $it")
            remoteLogger.logException(it, errorMsg)
        }

    private fun CryptoNonCustodialAccount.labelNeedsUpdate(): Boolean {
        val regex = """${labels.getOldDefaultNonCustodialWalletLabel(assetInfo)}(\s?)([\d]*)""".toRegex()
        return label.matches(regex)
    }

    final override fun forceAccountsRefresh() {
        activeAccounts.setForceRefresh()
    }

    abstract fun loadCustodialAccounts(): Single<SingleAccountList>
    abstract fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList>

    private fun loadInterestAccounts(): Single<SingleAccountList> =
        custodialManager.getInterestAvailabilityForAsset(assetInfo)
            .map {
                if (it) {
                    listOf(
                        CryptoInterestAccount(
                            currency = assetInfo,
                            label = labels.getDefaultInterestWalletLabel(),
                            interestBalance = interestBalance,
                            custodialWalletManager = custodialManager,
                            exchangeRates = exchangeRates,
                            identity = identity
                        )
                    )
                } else {
                    emptyList()
                }
            }

    override fun interestRate(): Single<Double> =
        custodialManager.getInterestAvailabilityForAsset(assetInfo)
            .flatMap {
                if (it) {
                    custodialManager.getInterestAccountRates(assetInfo)
                } else {
                    Single.just(0.0)
                }
            }

    final override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        accounts.flatMapMaybe {
            Maybe.fromCallable {
                it.makeAccountGroup(assetInfo, labels, filter)
            }
        }

    final override fun defaultAccount(): Single<SingleAccount> {
        return accounts.map {
            // todo(othman): remove when it.first { a -> a.isDefault } crash is fixed
            remoteLogger.logEvent("defaultAccount, accounts: ${it.size}, hasDefault: ${it.any { it.isDefault }}")
            it.forEach {
                remoteLogger.logEvent("defaultAccount, account: ${it.label}")
            }
            //

            it.first { a -> a.isDefault }
        }
    }

    private fun getNonCustodialAccountList(): Single<SingleAccountList> =
        accountGroup(filter = AssetFilter.NonCustodial)
            .map { group -> group.accounts }
            .defaultIfEmpty(emptyList())

    final override fun exchangeRate(): Single<ExchangeRate> =
        exchangeRates.exchangeRateToUserFiat(assetInfo).firstOrError()

    final override fun getPricesWith24hDelta(): Single<Prices24HrWithDelta> =
        exchangeRates.getPricesWith24hDelta(assetInfo).firstOrError()

    final override fun historicRate(epochWhen: Long): Single<ExchangeRate> =
        exchangeRates.getHistoricRate(assetInfo, epochWhen)

    override fun historicRateSeries(period: HistoricalTimeSpan): Single<HistoricalRateList> =
        assetInfo.startDate?.let {
            exchangeRates.getHistoricPriceSeries(assetInfo, period)
        } ?: Single.just(emptyList())

    override fun lastDayTrend(): Single<List<HistoricalRate>> {
        return assetInfo.startDate?.let {
            exchangeRates.get24hPriceSeries(assetInfo)
        } ?: Single.just(emptyList())
    }

    private fun getPitLinkingTargets(): Maybe<SingleAccountList> =
        exchangeLinking.isExchangeLinked().filter { it }
            .flatMap { custodialManager.getExchangeSendAddressFor(assetInfo) }
            .map { address ->
                listOf(
                    CryptoExchangeAccount(
                        currency = assetInfo,
                        label = labels.getDefaultExchangeWalletLabel(),
                        address = address,
                        exchangeRates = exchangeRates
                    )
                )
            }

    private fun getInterestTargets(): Maybe<SingleAccountList> =
        custodialManager.getInterestEligibilityForAsset(assetInfo).flatMapMaybe { eligibility ->
            if (eligibility.eligible) {
                accounts.flatMapMaybe {
                    Maybe.just(it.filterIsInstance<CryptoInterestAccount>())
                }
            } else {
                Maybe.empty()
            }
        }

    private fun getCustodialTargets(): Maybe<SingleAccountList> =
        accountGroup(AssetFilter.Custodial)
            .map { it.accounts }
            .onErrorComplete()

    private fun getNonCustodialTargets(exclude: SingleAccount? = null): Maybe<SingleAccountList> =
        getNonCustodialAccountList()
            .map { ll ->
                ll.filter { a -> a !== exclude }
            }.flattenAsObservable {
                it
            }.flatMapMaybe { account ->
                account.actions.flatMapMaybe {
                    if (it.contains(AssetAction.Receive)) {
                        Maybe.just(account)
                    } else Maybe.empty()
                }
            }.toList().toMaybe()

    final override fun transactionTargets(account: SingleAccount): Single<SingleAccountList> {
        require(account is CryptoAccount)
        require(account.currency == assetInfo)

        return when (account) {
            is TradingAccount -> Maybe.concat(
                listOf(
                    getNonCustodialTargets(),
                    getInterestTargets()
                )
            ).toList()
                .map { ll -> ll.flatten() }
                .onErrorReturnItem(emptyList())
            is NonCustodialAccount ->
                Maybe.concat(
                    listOf(
                        getPitLinkingTargets(),
                        getInterestTargets(),
                        getCustodialTargets(),
                        getNonCustodialTargets(exclude = account)
                    )
                ).toList()
                    .map { ll -> ll.flatten() }
                    .onErrorReturnItem(emptyList())
            is InterestAccount -> {
                Maybe.concat(
                    getCustodialTargets(),
                    getNonCustodialTargets()
                ).toList()
                    .map { ll -> ll.flatten() }
                    .onErrorReturnItem(emptyList())
            }
            else -> Single.just(emptyList())
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal class ActiveAccountList(
    private val asset: AssetInfo,
    private val custodialManager: CustodialWalletManager
) {
    private val activeList = mutableSetOf<CryptoAccount>()

    private var interestEnabled = false
    private val forceRefreshOnNext = AtomicBoolean(true)

    fun setForceRefresh() {
        forceRefreshOnNext.set(true)
    }

    fun fetchAccountList(
        loader: () -> Single<SingleAccountList>
    ): Single<SingleAccountList> =
        shouldRefresh().flatMap { refresh ->
            if (refresh) {
                loader().map { updateWith(it) }
            } else {
                Single.just(activeList.toList())
            }
        }

    private fun shouldRefresh() =
        Singles.zip(
            Single.just(interestEnabled),
            custodialManager.getInterestAvailabilityForAsset(asset),
            Single.just(forceRefreshOnNext.getAndSet(false))
        ) { wasEnabled, isEnabled, force ->
            interestEnabled = isEnabled
            wasEnabled != isEnabled || force
        }.onErrorReturn { false }

    @Synchronized
    private fun updateWith(
        accounts: List<SingleAccount>
    ): List<CryptoAccount> {
        val newActives = mutableSetOf<CryptoAccount>()
        accounts.filterIsInstance<CryptoAccount>()
            .forEach { a -> newActives.add(a) }
        activeList.clear()
        activeList.addAll(newActives)

        return activeList.toList()
    }
}

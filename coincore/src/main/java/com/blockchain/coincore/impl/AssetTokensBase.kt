package com.blockchain.coincore.impl

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TradingAccount
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestEligibility
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.koin.scopedInject
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletModeService
import exchange.ExchangeLinking
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isCustodial
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

interface AccountRefreshTrigger {
    fun forceAccountsRefresh()
}

internal abstract class CryptoAssetBase : CryptoAsset, AccountRefreshTrigger, KoinComponent {

    protected val exchangeRates: ExchangeRatesDataManager by inject()
    private val labels: DefaultLabels by inject()
    protected val custodialManager: CustodialWalletManager by scopedInject()
    private val interestService: InterestService by scopedInject()
    private val tradingService: TradingService by scopedInject()
    private val exchangeLinking: ExchangeLinking by scopedInject()
    private val remoteLogger: RemoteLogger by inject()
    private val walletModeService: WalletModeService by inject()
    protected val identity: UserIdentity by scopedInject()

    private val activeAccounts: ActiveAccountList by unsafeLazy {
        ActiveAccountList(currency, interestService)
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
                            labels.getOldDefaultNonCustodialWalletLabel(currency),
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

    private fun loadAccounts(): Single<SingleAccountList> {
        return Single.zip(
            loadNonCustodialAccounts(
                labels
            ),
            loadCustodialAccounts(),
            loadInterestAccounts()
        ) { nc, c, i ->
            nc + c + i
        }.doOnError {
            val errorMsg = "Error loading accounts for ${currency.networkTicker}"
            Timber.e("$errorMsg: $it")
            remoteLogger.logException(it, errorMsg)
        }
    }

    private fun CryptoNonCustodialAccount.labelNeedsUpdate(): Boolean {
        val regex =
            """${labels.getOldDefaultNonCustodialWalletLabel(this@CryptoAssetBase.currency)}(\s?)([\d]*)""".toRegex()
        return label.matches(regex)
    }

    final override fun forceAccountsRefresh() {
        activeAccounts.setForceRefresh()
    }

    private fun loadCustodialAccounts(): Single<SingleAccountList> =
        if (currency.isCustodial) Single.just(
            listOf(
                CustodialTradingAccount(
                    currency = currency,
                    label = labels.getDefaultCustodialWalletLabel(),
                    exchangeRates = exchangeRates,
                    custodialWalletManager = custodialManager,
                    tradingService = tradingService,
                    identity = identity,
                    walletModeService = walletModeService
                )
            )
        )
        else
            Single.just(emptyList())

    abstract fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList>

    private fun loadInterestAccounts(): Single<SingleAccountList> =
        interestService.isAssetAvailableForInterest(currency)
            .map {
                if (it) {
                    listOf(
                        CryptoInterestAccount(
                            currency = currency,
                            label = labels.getDefaultInterestWalletLabel(),
                            interestService = interestService,
                            custodialWalletManager = custodialManager,
                            exchangeRates = exchangeRates,
                            identity = identity,
                            internalAccountLabel = labels.getDefaultCustodialWalletLabel()
                        )
                    )
                } else {
                    emptyList()
                }
            }

    final override fun interestRate(): Single<Double> =
        interestService.isAssetAvailableForInterest(currency)
            .flatMap { isAvailable ->
                if (isAvailable) {
                    interestService.getInterestRate(currency)
                } else {
                    Single.just(0.0)
                }
            }

    final override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        accounts.flatMapMaybe {
            Maybe.fromCallable {
                it.makeAccountGroup(currency, labels, filter)
            }
        }

    final override fun defaultAccount(): Single<SingleAccount> {
        return accounts.map {
            // todo(othman): remove when it.first { a -> a.isDefault } crash is fixed
            remoteLogger.logEvent("defaultAccount, accounts: ${it.size}, hasDefault: ${it.any { it.isDefault }}")
            it.forEach {
                remoteLogger.logEvent("defaultAccount, account: ${it.label}")
            }
            it.first { a -> a.isDefault }
        }
    }

    private fun getNonCustodialAccountList(): Single<SingleAccountList> =
        accountGroup(filter = AssetFilter.NonCustodial)
            .map { group -> group.accounts }
            .defaultIfEmpty(emptyList())

    final override fun exchangeRate(): Single<ExchangeRate> =
        exchangeRates.exchangeRateToUserFiat(currency).firstOrError()

    final override fun getPricesWith24hDeltaLegacy(): Single<Prices24HrWithDelta> =
        exchangeRates.getPricesWith24hDeltaLegacy(currency).firstOrError()

    final override fun getPricesWith24hDelta(): Flow<DataResource<Prices24HrWithDelta>> {
        return exchangeRates.getPricesWith24hDelta(currency)
    }

    final override fun historicRate(epochWhen: Long): Single<ExchangeRate> =
        exchangeRates.getHistoricRate(currency, epochWhen)

    final override fun historicRateSeries(period: HistoricalTimeSpan): Flow<DataResource<HistoricalRateList>> =
        currency.startDate?.let {
            exchangeRates.getHistoricPriceSeries(currency, period)
        } ?: flowOf(DataResource.Data(emptyList()))

    final override fun lastDayTrend(): Flow<DataResource<HistoricalRateList>> {
        return currency.startDate?.let {
            exchangeRates.get24hPriceSeries(currency)
        } ?: flowOf(DataResource.Data(emptyList()))
    }

    private fun getPitLinkingTargets(): Maybe<SingleAccountList> =
        exchangeLinking.isExchangeLinked().filter { it }
            .flatMap { custodialManager.getExchangeSendAddressFor(currency) }
            .map { address ->
                listOf(
                    CryptoExchangeAccount(
                        currency = currency,
                        label = labels.getDefaultExchangeWalletLabel(),
                        address = address,
                        exchangeRates = exchangeRates
                    )
                )
            }

    private fun getInterestTargets(): Maybe<SingleAccountList> =
        interestService.getEligibilityForAsset(currency).flatMapMaybe { eligibility ->
            if (eligibility == InterestEligibility.Eligible) {
                accounts.flatMapMaybe {
                    Maybe.just(it.filterIsInstance<CryptoInterestAccount>())
                }
            } else {
                Maybe.empty()
            }
        }

    private fun getCustodialTargets(): Maybe<SingleAccountList> =
        accountGroup(AssetFilter.Trading)
            .map { it.accounts }
            .onErrorComplete()

    private fun getNonCustodialTargets(exclude: SingleAccount? = null): Maybe<SingleAccountList> =
        getNonCustodialAccountList()
            .map { ll ->
                ll.filter { a -> a !== exclude }
            }.flattenAsObservable {
                it
            }.flatMapMaybe { account ->
                account.stateAwareActions.flatMapMaybe { set ->
                    if (set.find { it.action == AssetAction.Receive && it.state == ActionState.Available } != null) {
                        Maybe.just(account)
                    } else Maybe.empty()
                }
            }.toList().toMaybe()

    final override fun transactionTargets(account: SingleAccount): Single<SingleAccountList> {
        require(account is CryptoAccount)
        require(account.currency == currency)

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
                getCustodialTargets()
                    .onErrorReturnItem(emptyList())
                    .defaultIfEmpty(emptyList())
            }
            else -> Single.just(emptyList())
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal class ActiveAccountList(
    private val asset: AssetInfo,
    private val interestService: InterestService
) {
    private val activeList = mutableSetOf<CryptoAccount>()

    private var interestEnabled = false
    private val forceRefreshOnNext = AtomicBoolean(true)

    fun setForceRefresh() {
        forceRefreshOnNext.set(true)
    }

    fun fetchAccountList(
        loader: () -> Single<SingleAccountList>,
    ): Single<SingleAccountList> =
        shouldRefresh().flatMap { refresh ->
            if (refresh || activeList.isEmpty()) {
                loader().map { updateWith(it) }
            } else {
                Single.just(activeList.toList())
            }
        }

    private fun shouldRefresh() =
        Singles.zip(
            Single.just(interestEnabled),
            interestService.isAssetAvailableForInterest(asset),
            Single.just(forceRefreshOnNext.getAndSet(false))
        ) { wasEnabled, isEnabled, force ->
            interestEnabled = isEnabled
            wasEnabled != isEnabled || force
        }.onErrorReturn { false }

    @Synchronized
    private fun updateWith(
        accounts: List<SingleAccount>,
    ): List<CryptoAccount> {
        val newActives = mutableSetOf<CryptoAccount>()
        accounts.filterIsInstance<CryptoAccount>()
            .forEach { a -> newActives.add(a) }
        activeList.clear()
        activeList.addAll(newActives)

        return activeList.toList()
    }
}

/**
 * This interface is implemented by all the Local Standard L1s that our app contains their logic+sdk
 */
interface StandardL1Asset

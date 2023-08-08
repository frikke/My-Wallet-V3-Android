package com.blockchain.coincore.impl

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TradingAccount
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asSingle
import com.blockchain.data.onErrorReturn
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.koin.scopedInject
import com.blockchain.logging.Logger
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.Feature
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.unsafeLazy
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletModeService
import exchange.ExchangeLinking
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.isCustodial
import info.blockchain.wallet.LabeledAccount
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface AccountRefreshTrigger {
    fun forceAccountsRefresh()
}

internal abstract class CryptoAssetBase : CryptoAsset, AccountRefreshTrigger, KoinComponent {

    protected val exchangeRates: ExchangeRatesDataManager by inject()
    private val labels: DefaultLabels by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    protected val custodialManager: CustodialWalletManager by scopedInject()
    private val interestService: InterestService by scopedInject()
    private val tradingService: TradingService by scopedInject()
    private val exchangeLinking: ExchangeLinking by scopedInject()
    private val remoteLogger: RemoteLogger by inject()
    private val walletModeService: WalletModeService by scopedInject()
    protected val identity: UserIdentity by scopedInject()
    private val userFeaturePermissionService: UserFeaturePermissionService by scopedInject()
    private val kycService: KycService by scopedInject()
    private val stakingService: StakingService by scopedInject()
    private val activeRewardsService: ActiveRewardsService by scopedInject()
    private val activeAccounts: ActiveAccountList by unsafeLazy {
        ActiveAccountList()
    }

    protected fun accounts(filter: AssetFilter): Single<SingleAccountList> {
        return activeAccounts.fetchAccountList(assetFilter = filter, loader = { f ->
            loadAccounts(f)
        })
    }

    protected fun LabeledAccount.labelNeedsUpdate(): Boolean {
        val regexV0 =
            """${labels.getV0DefaultNonCustodialWalletLabel(currency)}(\s?)(\d*)""".toRegex()
        val regexV1 =
            """${labels.getV1DefaultNonCustodialWalletLabel(currency)}(\s?)(\d*)""".toRegex()

        return label.matches(regexV0) || label.matches(regexV1)
    }

    protected fun String.updatedLabel(): String {
        if (contains(labels.getV0DefaultNonCustodialWalletLabel(currency), true)) {
            return this.replace(
                labels.getV0DefaultNonCustodialWalletLabel(currency),
                labels.getDefaultNonCustodialWalletLabel(),
                true
            )
        }
        if (contains(labels.getV1DefaultNonCustodialWalletLabel(currency), true)) {
            return this.replace(
                labels.getV1DefaultNonCustodialWalletLabel(currency),
                labels.getDefaultNonCustodialWalletLabel(),
                true
            )
        }
        return this
    }

    private fun loadAccounts(filter: AssetFilter): Single<SingleAccountList> {
        return when (filter) {
            AssetFilter.All -> Single.zip(
                loadNonCustodialAccounts(
                    labels
                ),
                loadCustodialAccounts(),
                loadInterestAccounts(),
                loadStakingAccounts(),
                loadActiveRewardsAccounts()
            ) { nonCustodial, trading, interest, staking, activeRewards ->
                nonCustodial + trading + interest + staking + activeRewards
            }.doOnError {
                val errorMsg = "Error loading accounts for ${currency.networkTicker}"
                Logger.e("$errorMsg: $it")
                remoteLogger.logException(it, errorMsg)
            }

            AssetFilter.NonCustodial -> loadNonCustodialAccounts(
                labels
            )

            AssetFilter.Custodial,
            AssetFilter.Trading -> {
                if (currency.isCustodial) {
                    Single.zip(
                        loadCustodialAccounts(),
                        loadInterestAccounts(),
                        loadStakingAccounts(),
                        loadActiveRewardsAccounts()
                    ) { trading, interest, staking, activeRewards ->
                        trading + interest + staking + activeRewards
                    }.doOnError {
                        val errorMsg = "Error loading accounts for ${currency.networkTicker}"
                        Logger.e("$errorMsg: $it")
                        remoteLogger.logException(it, errorMsg)
                    }
                } else Single.just(emptyList())
            }

            AssetFilter.Interest -> loadInterestAccounts()
            AssetFilter.Staking -> loadStakingAccounts()
            AssetFilter.ActiveRewards -> loadActiveRewardsAccounts()
        }
    }

    final override fun forceAccountsRefresh() {
        activeAccounts.setForceRefresh()
    }

    private val custodialAccountsAccess =
        userFeaturePermissionService.isEligibleFor(
            Feature.CustodialAccounts,
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ).asSingle().onErrorReturn { false }

    private fun loadCustodialAccounts(): Single<SingleAccountList> {
        if (AssetCategory.TRADING !in currency.categories) {
            return Single.just(emptyList())
        }
        return custodialAccountsAccess.map {
            if (it) {
                listOf(
                    CustodialTradingAccount(
                        currency = currency,
                        label = labels.getDefaultTradingWalletLabel(),
                        exchangeRates = exchangeRates,
                        custodialWalletManager = custodialManager,
                        tradingService = tradingService,
                        identity = identity,
                        kycService = kycService,
                        walletModeService = walletModeService
                    )
                )
            } else {
                emptyList()
            }
        }
    }

    abstract fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList>

    private fun loadInterestAccounts(): Single<SingleAccountList> {
        return custodialAccountsAccess.flatMap { hasCustodialAccess ->
            if (!hasCustodialAccess) return@flatMap Single.just(emptyList())
            interestService.isAssetAvailableForInterest(currency).onErrorReturn { false }.map {
                if (it) {
                    listOf(
                        CustodialInterestAccount(
                            currency = currency,
                            label = labels.getDefaultInterestWalletLabel(),
                            interestService = interestService,
                            custodialWalletManager = custodialManager,
                            exchangeRates = exchangeRates,
                            identity = identity,
                            kycService = kycService,
                            internalAccountLabel = labels.getDefaultTradingWalletLabel()
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun loadStakingAccounts(): Single<SingleAccountList> {
        return custodialAccountsAccess.flatMap { hasCustodialAccess ->
            if (!hasCustodialAccess) return@flatMap Single.just(emptyList())
            stakingService.getAvailabilityForAsset(currency).onErrorReturn { false }.asSingle().map {
                if (it) {
                    listOf(
                        CustodialStakingAccount(
                            currency = currency,
                            label = labels.getDefaultStakingWalletLabel(),
                            stakingService = stakingService,
                            exchangeRates = exchangeRates,
                            internalAccountLabel = labels.getDefaultTradingWalletLabel(),
                            identity = identity,
                            kycService = kycService,
                            custodialWalletManager = custodialManager
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun loadActiveRewardsAccounts(): Single<SingleAccountList> {
        return custodialAccountsAccess.flatMap { hasCustodialAccess ->
            if (!hasCustodialAccess) {
                Single.just(emptyList())
            } else
                activeRewardsService.getAvailabilityForAsset(currency).onErrorReturn { false }.asSingle()
                    .map { avaialble ->
                        if (avaialble) {
                            listOf(
                                CustodialActiveRewardsAccount(
                                    currency = currency,
                                    label = labels.getDefaultActiveRewardsWalletLabel(),
                                    activeRewardsService = activeRewardsService,
                                    exchangeRates = exchangeRates,
                                    internalAccountLabel = labels.getDefaultTradingWalletLabel(),
                                    identity = identity,
                                    kycService = kycService,
                                    custodialWalletManager = custodialManager
                                )
                            )
                        } else {
                            emptyList()
                        }
                    }
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

    final override fun stakingRate(): Single<Double> =
        stakingService.getEligibilityForAsset(currency).asSingle().flatMap {
            if (it is EarnRewardsEligibility.Eligible) {
                stakingService.getRatesForAsset(currency).asSingle().map { it.rate }
            } else {
                Single.just(0.0)
            }
        }

    final override fun activeRewardsRate(): Single<Double> =
        activeRewardsService.getEligibilityForAsset(currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .asSingle().flatMap {
                if (it is EarnRewardsEligibility.Eligible) {
                    activeRewardsService.getRatesForAsset(
                        currency, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                    ).asSingle().map { it.rate }
                } else {
                    Single.just(0.0)
                }
            }

    final override fun accountGroup(filter: AssetFilter): Maybe<AccountGroup> =
        accounts(filter).flatMapMaybe {
            Maybe.fromCallable {
                it.makeAccountGroup(currency, labels, filter)
            }
        }

    final override fun defaultAccount(filter: AssetFilter): Single<SingleAccount> {
        return accounts(filter).map {
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
        return exchangeRates.getPricesWith24hDelta(fromAsset = currency)
    }

    final override fun historicRate(epochWhen: Long): Single<ExchangeRate> =
        exchangeRates.getHistoricRate(currency, epochWhen)

    final override fun historicRateSeries(
        period: HistoricalTimeSpan
    ): Flow<DataResource<HistoricalRateList>> =
        currency.startDate?.let {
            exchangeRates.getHistoricPriceSeries(asset = currency, span = period)
        } ?: flowOf(DataResource.Data(emptyList()))

    final override fun lastDayTrend(): Flow<DataResource<HistoricalRateList>> {
        return currency.startDate?.let {
            exchangeRates.get24hPriceSeries(currency)
        } ?: flowOf(DataResource.Data(emptyList()))
    }

    private fun getExchangeTargets(): Maybe<SingleAccountList> =
        exchangeLinking.isExchangeLinked().filter { it }
            .flatMap { custodialManager.getExchangeSendAddressFor(currency) }
            .map { address ->
                listOf(
                    CryptoExchangeAccount(
                        currency = currency,
                        label = labels.getDefaultExchangeWalletLabel(),
                        address = address,
                        currencyPrefs = currencyPrefs,
                        exchangeRates = exchangeRates
                    )
                )
            }

    private fun getInterestTargets(): Maybe<SingleAccountList> =
        interestService.getEligibilityForAsset(currency).flatMapMaybe { eligibility ->
            if (eligibility == EarnRewardsEligibility.Eligible) {
                accounts(AssetFilter.Interest).flatMapMaybe {
                    Maybe.just(it.filterIsInstance<CustodialInterestAccount>())
                }
            } else {
                Maybe.empty()
            }
        }

    private fun getStakingTargets(): Maybe<SingleAccountList> =
        stakingService.getEligibilityForAsset(currency).asSingle().flatMapMaybe { eligibility ->
            if (eligibility == EarnRewardsEligibility.Eligible) {
                accounts(AssetFilter.Staking).flatMapMaybe {
                    Maybe.just(it.filterIsInstance<CustodialStakingAccount>())
                }
            } else {
                Maybe.empty()
            }
        }

    private fun getTradingTargets(): Maybe<SingleAccountList> =
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
                    getInterestTargets(),
                    getStakingTargets()
                )
            ).toList()
                .map { ll -> ll.flatten() }
                .onErrorReturnItem(emptyList())

            is NonCustodialAccount ->
                Maybe.concat(
                    listOf(
                        getExchangeTargets().onErrorReturnItem(emptyList()),
                        getInterestTargets().onErrorReturnItem(emptyList()),
                        getTradingTargets().onErrorReturnItem(emptyList()),
                        getNonCustodialTargets(exclude = account).onErrorReturnItem(emptyList()),
                        getStakingTargets().onErrorReturnItem(emptyList())
                    )
                ).toList()
                    .map { ll -> ll.flatten() }
                    .onErrorReturnItem(emptyList())

            is EarnRewardsAccount -> {
                getTradingTargets()
                    .onErrorReturnItem(emptyList())
                    .defaultIfEmpty(emptyList())
            }

            else -> Single.just(emptyList())
        }
    }
}

internal class ActiveAccountList {
    private val activeList = mutableMapOf<AssetFilter, Set<CryptoAccount>>()

    private val forceRefreshOnNext = AtomicBoolean(true)

    fun setForceRefresh() {
        forceRefreshOnNext.set(true)
    }

    fun fetchAccountList(
        assetFilter: AssetFilter,
        loader: (AssetFilter) -> Single<SingleAccountList>
    ): Single<SingleAccountList> =
        shouldRefresh(assetFilter).flatMap { refresh ->
            if (refresh) {
                loader(assetFilter).map { updateWith(assetFilter, it) }
            } else {
                Single.just(activeList[assetFilter]?.toList() ?: emptyList())
            }
        }

    private fun shouldRefresh(filter: AssetFilter): Single<Boolean> {
        return if (activeList[filter] == null || forceRefreshOnNext.get()) {
            Single.just(true).doOnSuccess {
                forceRefreshOnNext.set(false)
            }
        } else Single.just(false)
    }

    @Synchronized
    private fun updateWith(
        assetFilter: AssetFilter,
        accounts: List<SingleAccount>
    ): List<CryptoAccount> {
        val newActives = mutableSetOf<CryptoAccount>()
        accounts.filterIsInstance<CryptoAccount>()
            .forEach { a -> newActives.add(a) }
        activeList[assetFilter] = newActives

        return activeList[assetFilter]!!.toList()
    }
}

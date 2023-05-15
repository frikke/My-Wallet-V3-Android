package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.data.doOnData
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.active.ActiveRewardsAccountBalance
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.store.filterNotLoading
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import java.math.BigDecimal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitSingle
import kotlinx.coroutines.rx3.awaitSingleOrNull
import timber.log.Timber

class EarnDashboardViewModel(
    private val coincore: Coincore,
    private val stakingService: StakingService,
    private val interestService: InterestService,
    private val activeRewardsService: ActiveRewardsService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val userIdentity: UserIdentity,
    private val assetCatalogue: AssetCatalogue,
    private val custodialWalletManager: CustodialWalletManager,
    private val walletStatusPrefs: WalletStatusPrefs,
    private val currencyPrefs: CurrencyPrefs,
    private val activeRewardsFeatureFlag: FeatureFlag
) : MviViewModel<
    EarnDashboardIntent,
    EarnDashboardViewState,
    EarnDashboardModelState,
    EarnDashboardNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    EarnDashboardModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) { }

    override fun reduce(state: EarnDashboardModelState): EarnDashboardViewState = state.run {
        EarnDashboardViewState(
            dashboardState = reduceDashboardState(
                isLoading = isLoading,
                error = error,
                earnData = earnData,
                earningTabFilterBy = earningTabFilterBy,
                earningTabQueryBy = earningTabQueryBy,
                discoverTabFilterBy = discoverTabFilterBy,
                discoverTabQueryBy = discoverTabQueryBy,
                hasSeenEarnIntro = hasSeenEarnIntro,
                filterList = filterList
            ),
            earningTabFilterBy = earningTabFilterBy,
            earningTabQueryBy = earningTabQueryBy,
            discoverTabFilterBy = discoverTabFilterBy,
            discoverTabQueryBy = discoverTabQueryBy
        )
    }

    override suspend fun handleIntent(modelState: EarnDashboardModelState, intent: EarnDashboardIntent) =
        when (intent) {
            is EarnDashboardIntent.LoadEarn -> loadEarn()
            is EarnDashboardIntent.LoadSilently -> collectEarnData(false)
            is EarnDashboardIntent.UpdateEarningTabListFilter -> updateState {
                it.copy(
                    earningTabFilterBy = intent.filter
                )
            }
            is EarnDashboardIntent.UpdateEarningTabSearchQuery -> updateState {
                it.copy(
                    earningTabQueryBy = intent.searchTerm
                )
            }
            is EarnDashboardIntent.UpdateDiscoverTabListFilter -> updateState {
                it.copy(
                    discoverTabFilterBy = intent.filter
                )
            }
            is EarnDashboardIntent.UpdateDiscoverTabSearchQuery -> updateState {
                it.copy(
                    discoverTabQueryBy = intent.searchTerm
                )
            }
            is EarnDashboardIntent.DiscoverItemSelected -> {
                when (intent.earnAsset.eligibility) {
                    EarnRewardsEligibility.Eligible -> showAcquireOrSummaryForEarnType(
                        earnType = intent.earnAsset.type,
                        assetTicker = intent.earnAsset.assetTicker
                    )
                    is EarnRewardsEligibility.Ineligible -> when (intent.earnAsset.eligibility) {
                        EarnRewardsEligibility.Ineligible.KYC_TIER ->
                            navigate(EarnDashboardNavigationEvent.OpenKycUpgradeNowSheet)
                        EarnRewardsEligibility.Ineligible.REGION,
                        EarnRewardsEligibility.Ineligible.OTHER ->
                            navigate(EarnDashboardNavigationEvent.OpenBlockedForRegionSheet(intent.earnAsset.type))
                    }
                }
            }
            is EarnDashboardIntent.EarningItemSelected ->
                showAcquireOrSummaryForEarnType(
                    earnType = intent.earnAsset.type,
                    assetTicker = intent.earnAsset.assetTicker
                )
            is EarnDashboardIntent.LaunchProductComparator -> {
                modelState.earnData?.let {
                    val earnProducts = mutableListOf<EarnType>().apply {
                        if (it.interestEligibility.any { it.value == EarnRewardsEligibility.Eligible }) {
                            add(EarnType.Passive)
                        }
                        if (it.stakingEligibility.any { it.value == EarnRewardsEligibility.Eligible }) {
                            add(EarnType.Staking)
                        }
                        if (it.activeRewardsEligibility.any { it.value == EarnRewardsEligibility.Eligible }) {
                            add(EarnType.Active)
                        }
                    }

                    if (earnProducts.isNotEmpty()) {
                        navigate(EarnDashboardNavigationEvent.OpenProductComparator(earnProducts = earnProducts))
                    }
                } ?: Timber.w("Unable to launch Earn Product Comparator. Earn data is null")
            }

            EarnDashboardIntent.StartKycClicked -> navigate(EarnDashboardNavigationEvent.OpenKyc)
            is EarnDashboardIntent.OnNavigateToAction -> {
                when (intent.action) {
                    AssetAction.Buy -> navigate(EarnDashboardNavigationEvent.OpenBuy(intent.assetInfo))
                    AssetAction.Receive -> navigate(
                        EarnDashboardNavigationEvent.OpenReceive(intent.assetInfo.networkTicker)
                    )
                    else -> throw IllegalStateException("Earn dashboard: ${intent.action} not valid for navigation")
                }
            }

            is EarnDashboardIntent.FinishOnboarding -> {
                walletStatusPrefs.hasSeenEarnProductIntro = true
                updateState {
                    it.copy(
                        hasSeenEarnIntro = true
                    )
                }
            }
        }

    private fun reduceDashboardState(
        isLoading: Boolean,
        error: EarnDashboardError,
        earnData: CombinedEarnData?,
        earningTabFilterBy: EarnDashboardListFilter,
        earningTabQueryBy: String,
        discoverTabFilterBy: EarnDashboardListFilter,
        discoverTabQueryBy: String,
        filterList: List<EarnDashboardListFilter>,
        hasSeenEarnIntro: Boolean
    ): DashboardState =
        when {
            isLoading && earnData != null -> earnData.loadEarn(
                earningTabFilterBy,
                earningTabQueryBy,
                discoverTabFilterBy,
                discoverTabQueryBy,
                filterList,
                hasSeenEarnIntro
            )
            isLoading -> DashboardState.Loading
            error != EarnDashboardError.None -> DashboardState.ShowError(error)
            else -> earnData?.loadEarn(
                earningTabFilterBy,
                earningTabQueryBy,
                discoverTabFilterBy,
                discoverTabQueryBy,
                filterList,
                hasSeenEarnIntro
            ) ?: DashboardState.Loading
        }

    private fun CombinedEarnData.loadEarn(
        earningTabFilterBy: EarnDashboardListFilter,
        earningTabQueryBy: String,
        discoverTabFilterBy: EarnDashboardListFilter,
        discoverTabQueryBy: String,
        filterList: List<EarnDashboardListFilter>,
        hasSeenEarnIntro: Boolean
    ): DashboardState {
        val hasStakingBalance =
            stakingBalancesWithFiat.values.any { it.stakingCryptoBalances.totalBalance.isPositive }
        val hasInterestBalance =
            interestBalancesWithFiat.values.any { it.interestCryptoBalances.totalBalance.isPositive }
        val hasActiveRewardsBalance =
            activeRewardsBalancesWithFiat.values.any { it.activeRewardsCryptoBalances.totalBalance.isPositive }

        if (interestFeatureAccess !is FeatureAccess.Granted && !hasInterestBalance &&
            stakingFeatureAccess !is FeatureAccess.Granted && !hasStakingBalance &&
            activeRewardsFeatureAccess !is FeatureAccess.Granted && !hasActiveRewardsBalance
        ) {
            return DashboardState.ShowKyc
        }

        return if (!hasInterestBalance && !hasStakingBalance && !hasActiveRewardsBalance) {
            if (!hasSeenEarnIntro) {
                DashboardState.ShowIntro(
                    mutableListOf<EarnType>().apply {
                        if (interestFeatureAccess is FeatureAccess.Granted) {
                            add(EarnType.Passive)
                        }
                        if (stakingFeatureAccess is FeatureAccess.Granted) {
                            add(EarnType.Staking)
                        }
                        if (activeRewardsFeatureAccess is FeatureAccess.Granted) {
                            add(EarnType.Active)
                        }
                    }
                )
            } else {
                buildDiscoverList(
                    data = this,
                    discoverTabFilterBy = discoverTabFilterBy,
                    discoverTabQueryBy = discoverTabQueryBy,
                    filterList = filterList
                )
            }
        } else {
            splitEarningAndDiscoverData(
                data = this,
                earningTabFilterBy = earningTabFilterBy,
                earningTabQueryBy = earningTabQueryBy,
                discoverTabFilterBy = discoverTabFilterBy,
                discoverTabQueryBy = discoverTabQueryBy,
                filterList = filterList
            )
        }
    }

    private suspend fun showAcquireOrSummaryForEarnType(earnType: EarnType, assetTicker: String) {
        assetCatalogue.fromNetworkTicker(assetTicker)?.let { currency ->
            val tradingAccount = coincore[currency].accountGroup(AssetFilter.Trading).awaitSingle().accounts.first()
            val pkwAccountsBalance =
                coincore[currency].accountGroup(AssetFilter.NonCustodial).awaitSingleOrNull()?.accounts?.map {
                    it.balance().firstOrNull()
                }?.toList()?.sumOf { it?.total?.toBigDecimal() ?: BigDecimal.ZERO } ?: Money.zero(currency)
                    .toBigDecimal()

            if (tradingAccount.balance().firstOrNull()?.total?.isPositive == true ||
                pkwAccountsBalance > BigDecimal.ZERO
            ) {
                when (earnType) {
                    EarnType.Passive -> navigate(
                        EarnDashboardNavigationEvent.OpenInterestSummarySheet(currency.networkTicker)
                    )
                    EarnType.Staking -> navigate(
                        EarnDashboardNavigationEvent.OpenStakingSummarySheet(currency.networkTicker)
                    )
                    EarnType.Active -> navigate(
                        EarnDashboardNavigationEvent.OpenActiveRewardsSummarySheet(currency.networkTicker)
                    )
                }
            } else {
                custodialWalletManager.isCurrencyAvailableForTrading(
                    assetInfo = currency as AssetInfo,
                    freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                ).filterNotLoading()
                    .doOnData { availableToBuy ->
                        navigate(
                            EarnDashboardNavigationEvent.OpenBuyOrReceiveSheet(
                                when (earnType) {
                                    EarnType.Passive -> AssetAction.InterestDeposit
                                    EarnType.Staking -> AssetAction.StakingDeposit
                                    EarnType.Active -> AssetAction.ActiveRewardsDeposit
                                },
                                availableToBuy,
                                tradingAccount
                            )
                        )
                    }.firstOrNull()
            }
        } ?: Timber.e("Earn Dashboard - unknown asset $assetTicker")
    }

    private fun buildDiscoverList(
        data: CombinedEarnData,
        discoverTabFilterBy: EarnDashboardListFilter,
        filterList: List<EarnDashboardListFilter>,
        discoverTabQueryBy: String
    ): DashboardState.OnlyDiscover {
        val discoverList = mutableListOf<EarnAsset>()
        data.stakingEligibility.map { (asset, eligibility) ->
            discoverList.add(
                asset.createStakingAsset(
                    stakingBalancesWithFiat = StakingBalancesWithFiat(
                        asset,
                        StakingAccountBalance.zeroBalance(asset),
                        Money.zero(asset)
                    ),
                    stakingRate = data.stakingRates[asset],
                    eligibility = eligibility
                )
            )
        }

        data.interestEligibility.map { (asset, eligibility) ->
            discoverList.add(
                asset.createPassiveAsset(
                    interestBalancesWithFiat = InterestBalancesWithFiat(
                        asset,
                        InterestAccountBalance.zeroBalance(asset),
                        Money.zero(asset)
                    ),
                    passiveRate = data.interestRates[asset],
                    eligibility = eligibility
                )
            )
        }

        data.activeRewardsEligibility.map { (asset, eligibility) ->
            discoverList.add(
                asset.createActiveRewardsAsset(
                    activeRewardsBalancesWithFiat = ActiveRewardsBalancesWithFiat(
                        asset,
                        ActiveRewardsAccountBalance.zeroBalance(asset),
                        Money.zero(asset)
                    ),
                    activeRewardsRate = data.activeRewardsRates[asset],
                    eligibility = eligibility
                )
            )
        }

        return DashboardState.OnlyDiscover(
            discoverList.sortListByFilterAndQuery(discoverTabFilterBy, discoverTabQueryBy).sortByRate(),
            filterList = filterList
        )
    }

    private fun splitEarningAndDiscoverData(
        data: CombinedEarnData,
        earningTabFilterBy: EarnDashboardListFilter,
        earningTabQueryBy: String,
        discoverTabFilterBy: EarnDashboardListFilter,
        discoverTabQueryBy: String,
        filterList: List<EarnDashboardListFilter>
    ): DashboardState.EarningAndDiscover {
        val earningList = mutableListOf<EarnAsset>()
        val discoverList = mutableListOf<EarnAsset>()

        var totalEarningBalanceFiat = Money.zero(currencyPrefs.selectedFiatCurrency)

        data.stakingBalancesWithFiat.map { (asset, balances) ->

            totalEarningBalanceFiat += balances.stakingTotalFiat

            val totalBalance = balances.stakingCryptoBalances.totalBalance

            if (totalBalance.isPositive) {
                val eligibility =
                    if (data.stakingEligibility.isNotEmpty()) {
                        data.stakingEligibility[asset] ?: EarnRewardsEligibility.Ineligible.OTHER
                    } else {
                        EarnRewardsEligibility.Ineligible.OTHER
                    }

                val stakingAsset = asset.createStakingAsset(
                    stakingBalancesWithFiat = balances,
                    stakingRate = data.stakingRates[asset],
                    eligibility = eligibility
                )

                earningList.add(stakingAsset)
                discoverList.add(stakingAsset)
            }
        }

        if (data.stakingEligibility.isNotEmpty()) {
            data.stakingEligibility.map { (asset, eligibility) ->
                // if discoverList doesn't contain the asset, add it
                if (discoverList.none { it.type is EarnType.Staking && it.assetTicker == asset.networkTicker }) {
                    discoverList.add(
                        asset.createStakingAsset(
                            stakingBalancesWithFiat = StakingBalancesWithFiat(
                                asset,
                                StakingAccountBalance.zeroBalance(asset),
                                Money.zero(asset)
                            ),
                            stakingRate = data.stakingRates[asset],
                            eligibility = eligibility
                        )
                    )
                }
            }
        }

        data.interestBalancesWithFiat.map { (asset, balances) ->

            totalEarningBalanceFiat += balances.interestTotalFiat

            val totalBalance = balances.interestCryptoBalances.totalBalance

            if (totalBalance.isPositive) {
                val eligibility =
                    if (data.interestEligibility.isNotEmpty()) {
                        data.interestEligibility[asset] ?: EarnRewardsEligibility.Ineligible.OTHER
                    } else {
                        EarnRewardsEligibility.Ineligible.OTHER
                    }

                val passiveAsset = asset.createPassiveAsset(
                    interestBalancesWithFiat = balances,
                    passiveRate = data.interestRates[asset],
                    eligibility = eligibility
                )

                earningList.add(passiveAsset)
                discoverList.add(passiveAsset)
            }
        }

        if (data.interestEligibility.isNotEmpty()) {
            data.interestEligibility.map { (asset, eligibility) ->
                // if discoverList doesn't contain the asset, add it
                if (discoverList.none { it.type is EarnType.Passive && it.assetTicker == asset.networkTicker }) {
                    data.interestRates[asset]?.let { rate ->
                        discoverList.add(
                            asset.createPassiveAsset(
                                interestBalancesWithFiat = InterestBalancesWithFiat(
                                    asset,
                                    InterestAccountBalance.zeroBalance(asset),
                                    Money.zero(asset)
                                ),
                                passiveRate = rate,
                                eligibility = eligibility
                            )
                        )
                    }
                }
            }
        }

        data.activeRewardsBalancesWithFiat.map { (asset, balances) ->

            totalEarningBalanceFiat += balances.activeRewardsTotalFiat

            val totalBalance = balances.activeRewardsCryptoBalances.totalBalance

            if (totalBalance.isPositive) {
                val eligibility =
                    if (data.activeRewardsEligibility.isNotEmpty()) {
                        data.activeRewardsEligibility[asset] ?: EarnRewardsEligibility.Ineligible.OTHER
                    } else {
                        EarnRewardsEligibility.Ineligible.OTHER
                    }

                val activeRewardsAsset = asset.createActiveRewardsAsset(
                    activeRewardsBalancesWithFiat = balances,
                    activeRewardsRate = data.activeRewardsRates[asset],
                    eligibility = eligibility
                )

                earningList.add(activeRewardsAsset)
                discoverList.add(activeRewardsAsset)
            }
        }

        if (data.activeRewardsEligibility.isNotEmpty()) {
            data.activeRewardsEligibility.map { (asset, eligibility) ->
                // if discoverList doesn't contain the asset, add it
                if (discoverList.none { it.type is EarnType.Active && it.assetTicker == asset.networkTicker }) {
                    discoverList.add(
                        asset.createActiveRewardsAsset(
                            activeRewardsBalancesWithFiat = ActiveRewardsBalancesWithFiat(
                                asset,
                                ActiveRewardsAccountBalance.zeroBalance(asset),
                                Money.zero(asset)
                            ),
                            activeRewardsRate = data.activeRewardsRates[asset],
                            eligibility = eligibility
                        )
                    )
                }
            }
        }

        return DashboardState.EarningAndDiscover(
            earning = earningList.sortListByFilterAndQuery(earningTabFilterBy, earningTabQueryBy).sortByBalance(),
            totalEarningBalanceFiat = totalEarningBalanceFiat.toStringWithSymbol(),
            discover = discoverList.sortListByFilterAndQuery(discoverTabFilterBy, discoverTabQueryBy).sortByRate(),
            filterList = filterList
        )
    }

    private fun AssetInfo.createStakingAsset(
        stakingBalancesWithFiat: StakingBalancesWithFiat,
        stakingRate: Double?,
        eligibility: EarnRewardsEligibility
    ) = EarnAsset(
        assetTicker = networkTicker,
        assetName = name,
        iconUrl = logo,
        rate = stakingRate ?: 0.0,
        eligibility = eligibility,
        balanceCrypto = stakingBalancesWithFiat.stakingCryptoBalances.totalBalance,
        balanceFiat = stakingBalancesWithFiat.stakingTotalFiat,
        type = EarnType.Staking
    )

    private fun AssetInfo.createPassiveAsset(
        interestBalancesWithFiat: InterestBalancesWithFiat,
        passiveRate: Double?,
        eligibility: EarnRewardsEligibility
    ) = EarnAsset(
        assetTicker = networkTicker,
        assetName = name,
        iconUrl = logo,
        rate = passiveRate ?: 0.0,
        eligibility = eligibility,
        balanceCrypto = interestBalancesWithFiat.interestCryptoBalances.totalBalance,
        balanceFiat = interestBalancesWithFiat.interestTotalFiat,
        type = EarnType.Passive
    )

    private fun AssetInfo.createActiveRewardsAsset(
        activeRewardsBalancesWithFiat: ActiveRewardsBalancesWithFiat,
        activeRewardsRate: Double?,
        eligibility: EarnRewardsEligibility
    ) = EarnAsset(
        assetTicker = networkTicker,
        assetName = name,
        iconUrl = logo,
        rate = activeRewardsRate ?: 0.0,
        eligibility = eligibility,
        balanceCrypto = activeRewardsBalancesWithFiat.activeRewardsCryptoBalances.totalBalance,
        balanceFiat = activeRewardsBalancesWithFiat.activeRewardsTotalFiat,
        type = EarnType.Active
    )

    private fun List<EarnAsset>.sortListByFilterAndQuery(
        filter: EarnDashboardListFilter,
        query: String
    ): List<EarnAsset> =
        when (filter) {
            EarnDashboardListFilter.All -> this
            EarnDashboardListFilter.Staking -> this.filter { it.type == EarnType.Staking }
            EarnDashboardListFilter.Interest -> this.filter { it.type == EarnType.Passive }
            EarnDashboardListFilter.Active -> this.filter { it.type == EarnType.Active }
        }.filter {
            query.isEmpty() || it.assetName.contains(query, true) ||
                it.assetTicker.contains(query, true)
        }

    private fun List<EarnAsset>.sortByRate(): List<EarnAsset> =
        this.sortedWith(
            compareByDescending<EarnAsset> { it.eligibility is EarnRewardsEligibility.Eligible }
                .thenByDescending { it.rate }
        )

    private fun List<EarnAsset>.sortByBalance(): List<EarnAsset> =
        this.sortedByDescending { it.balanceFiat }

    private suspend fun loadEarn() {
        val activeRewardsEnabled = activeRewardsFeatureFlag.coEnabled()

        updateState {
            it.copy(
                isLoading = true,
                hasSeenEarnIntro = walletStatusPrefs.hasSeenEarnProductIntro,
                filterList = listOf(
                    EarnDashboardListFilter.All,
                    EarnDashboardListFilter.Interest,
                    EarnDashboardListFilter.Staking
                ) + if (activeRewardsEnabled) {
                    listOf(EarnDashboardListFilter.Active)
                } else {
                    emptyList()
                }
            )
        }

        collectEarnData(true)
    }

    private suspend fun collectEarnData(showLoading: Boolean) {
        val accessMap = try {
            userIdentity.userAccessForFeatures(
                listOf(Feature.DepositStaking, Feature.DepositInterest, Feature.DepositActiveRewards)
            ).await()
        } catch (e: Exception) {
            mapOf(
                Feature.DepositStaking to FeatureAccess.Blocked(BlockedReason.NotEligible("")),
                Feature.DepositInterest to FeatureAccess.Blocked(BlockedReason.NotEligible("")),
                Feature.DepositActiveRewards to FeatureAccess.Blocked(BlockedReason.NotEligible(""))
            )
        }

        val stakingBalanceWithFiatFlow =
            stakingService.getBalanceForAllAssets().flatMapData { balancesMap ->
                if (balancesMap.isEmpty()) {
                    return@flatMapData flowOf(DataResource.Data(emptyMap()))
                }
                val balancesWithFiatRates = balancesMap.map { (asset, balances) ->
                    exchangeRatesDataManager.exchangeRateToUserFiatFlow(
                        fromAsset = asset
                    ).mapData { exchangeRate ->
                        StakingBalancesWithFiat(
                            asset,
                            balances,
                            exchangeRate.convert(balances.totalBalance)
                        )
                    }
                }

                combine(balancesWithFiatRates) { balancesResource ->
                    combineDataResources(balancesResource.toList()) { balancesList ->
                        balancesList.associateBy { balanceWithFiat ->
                            balanceWithFiat.asset
                        }
                    }
                }
            }

        val interestBalanceWithFiatFlow =
            interestService.getBalancesFlow().flatMapData { balancesMap ->
                if (balancesMap.isEmpty()) {
                    return@flatMapData flowOf(DataResource.Data(emptyMap()))
                }
                val balancesWithFiatRates = balancesMap.map { (asset, balances) ->
                    exchangeRatesDataManager.exchangeRateToUserFiatFlow(
                        fromAsset = asset
                    ).mapData { exchangeRate ->
                        InterestBalancesWithFiat(
                            asset,
                            balances,
                            exchangeRate.convert(balances.totalBalance)
                        )
                    }
                }

                combine(balancesWithFiatRates) { balancesResource ->
                    combineDataResources(balancesResource.toList()) { balancesList ->
                        balancesList.associateBy { balanceWithFiat ->
                            balanceWithFiat.asset
                        }
                    }
                }
            }

        val activeRewardsBalanceWithFiatFlow =
            if (activeRewardsFeatureFlag.coEnabled()) {
                activeRewardsService.getBalanceForAllAssets().flatMapData { balancesMap ->
                    if (balancesMap.isEmpty()) {
                        return@flatMapData flowOf(DataResource.Data(emptyMap()))
                    }
                    val balancesWithFiatRates = balancesMap.map { (asset, balances) ->
                        exchangeRatesDataManager.exchangeRateToUserFiatFlow(
                            fromAsset = asset
                        ).mapData { exchangeRate ->
                            ActiveRewardsBalancesWithFiat(
                                asset,
                                balances,
                                exchangeRate.convert(balances.totalBalance)
                            )
                        }
                    }

                    combine(balancesWithFiatRates) { balancesResource ->
                        combineDataResources(balancesResource.toList()) { balancesList ->
                            balancesList.associateBy { balanceWithFiat ->
                                balanceWithFiat.asset
                            }
                        }
                    }
                }
            } else {
                flowOf(DataResource.Data(emptyMap()))
            }

        val activeRewardsEligibilityFlow =
            if (activeRewardsFeatureFlag.coEnabled()) {
                activeRewardsService.getEligibilityForAssets()
            } else {
                flowOf(DataResource.Data(emptyMap()))
            }

        val activeRewardsRatesFlow =
            if (activeRewardsFeatureFlag.coEnabled()) {
                activeRewardsService.getRatesForAllAssets()
            } else {
                flowOf(DataResource.Data(emptyMap()))
            }

        combine(
            stakingBalanceWithFiatFlow,
            stakingService.getEligibilityForAssets(),
            stakingService.getRatesForAllAssets(),
            interestBalanceWithFiatFlow,
            interestService.getEligibilityForAssets(),
            interestService.getAllInterestRates(),
            activeRewardsBalanceWithFiatFlow,
            activeRewardsEligibilityFlow,
            activeRewardsRatesFlow
        ) { listOfData ->
            require(listOfData.size == 9)
            combineDataResources(
                listOfData.toList()
            ) { data ->
                CombinedEarnData(
                    stakingBalancesWithFiat = data[0] as Map<AssetInfo, StakingBalancesWithFiat>,
                    stakingEligibility = data[1] as Map<AssetInfo, EarnRewardsEligibility>,
                    stakingRates = data[2] as Map<AssetInfo, Double>,
                    interestBalancesWithFiat = data[3] as Map<AssetInfo, InterestBalancesWithFiat>,
                    interestEligibility = data[4] as Map<AssetInfo, EarnRewardsEligibility>,
                    interestRates = data[5] as Map<AssetInfo, Double>,
                    activeRewardsBalancesWithFiat = data[6] as Map<AssetInfo, ActiveRewardsBalancesWithFiat>,
                    activeRewardsEligibility = data[7] as Map<AssetInfo, EarnRewardsEligibility>,
                    activeRewardsRates = data[8] as Map<AssetInfo, Double>,
                    interestFeatureAccess = accessMap[Feature.DepositInterest]!!,
                    stakingFeatureAccess = accessMap[Feature.DepositStaking]!!,
                    activeRewardsFeatureAccess = accessMap[Feature.DepositActiveRewards]!!
                )
            }
        }.collectLatest { data ->
            when (data) {
                is DataResource.Data -> {
                    updateState {
                        it.copy(
                            isLoading = false,
                            earnData = data.data
                        )
                    }
                }
                is DataResource.Error -> {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = EarnDashboardError.DataFetchFailed
                        )
                    }
                }
                DataResource.Loading -> {
                    updateState {
                        it.copy(isLoading = showLoading)
                    }
                }
            }
        }
    }
}

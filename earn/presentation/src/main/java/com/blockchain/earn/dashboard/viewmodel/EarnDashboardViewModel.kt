package com.blockchain.earn.dashboard.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.combineDataResources
import com.blockchain.data.doOnData
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitSingle
import kotlinx.coroutines.rx3.awaitSingleOrNull
import timber.log.Timber

class EarnDashboardViewModel(
    private val coincore: Coincore,
    private val stakingService: StakingService,
    private val interestService: InterestService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val userIdentity: UserIdentity,
    private val assetCatalogue: AssetCatalogue,
    private val custodialWalletManager: CustodialWalletManager
) : MviViewModel<EarnDashboardIntent,
    EarnDashboardViewState,
    EarnDashboardModelState,
    EarnDashboardNavigationEvent,
    ModelConfigArgs.NoArgs
    >(initialState = EarnDashboardModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        viewModelScope.launch {
            onIntent(EarnDashboardIntent.LoadEarn)
        }
    }

    override fun reduce(state: EarnDashboardModelState): EarnDashboardViewState = state.run {
        EarnDashboardViewState(
            dashboardState = reduceDashboardState(
                isLoading = isLoading,
                error = error,
                earnData = earnData,
                earningTabFilterBy = earningTabFilterBy,
                earningTabQueryBy = earningTabQueryBy,
                discoverTabFilterBy = discoverTabFilterBy,
                discoverTabQueryBy = discoverTabQueryBy
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
                    is EarnRewardsEligibility.Ineligible -> navigate(
                        EarnDashboardNavigationEvent.OpenBlockedForRegionSheet(intent.earnAsset.type)
                    )
                }
            }
            is EarnDashboardIntent.EarningItemSelected ->
                showAcquireOrSummaryForEarnType(
                    earnType = intent.earnAsset.type,
                    assetTicker = intent.earnAsset.assetTicker
                )
            is EarnDashboardIntent.CarouselLearnMoreSelected ->
                navigate(EarnDashboardNavigationEvent.OpenUrl(intent.url))
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
        }

    private fun reduceDashboardState(
        isLoading: Boolean,
        error: EarnDashboardError,
        earnData: CombinedEarnData?,
        earningTabFilterBy: EarnDashboardListFilter,
        earningTabQueryBy: String,
        discoverTabFilterBy: EarnDashboardListFilter,
        discoverTabQueryBy: String
    ): DashboardState =
        when {
            isLoading && earnData != null -> earnData.loadEarn(
                earningTabFilterBy,
                earningTabQueryBy,
                discoverTabFilterBy,
                discoverTabQueryBy
            )
            isLoading -> DashboardState.Loading
            error != EarnDashboardError.None -> DashboardState.ShowError(error)
            else -> earnData?.loadEarn(
                earningTabFilterBy,
                earningTabQueryBy,
                discoverTabFilterBy,
                discoverTabQueryBy
            ) ?: DashboardState.Loading
        }

    private fun CombinedEarnData.loadEarn(
        earningTabFilterBy: EarnDashboardListFilter,
        earningTabQueryBy: String,
        discoverTabFilterBy: EarnDashboardListFilter,
        discoverTabQueryBy: String
    ): DashboardState {
        val hasStakingBalance =
            stakingBalancesWithFiat.values.any { it.stakingCryptoBalances.totalBalance.isPositive }
        val hasInterestBalance =
            interestBalancesWithFiat.values.any { it.interestCryptoBalances.totalBalance.isPositive }

        if (interestFeatureAccess !is FeatureAccess.Granted && !hasInterestBalance &&
            stakingFeatureAccess !is FeatureAccess.Granted && !hasStakingBalance
        ) {
            return DashboardState.ShowKyc
        }

        return if (hasStakingBalance || hasInterestBalance) {
            splitEarningAndDiscoverData(
                data = this,
                earningTabFilterBy = earningTabFilterBy,
                earningTabQueryBy = earningTabQueryBy,
                discoverTabFilterBy = discoverTabFilterBy,
                discoverTabQueryBy = discoverTabQueryBy
            )
        } else {
            buildDiscoverList(
                data = this,
                discoverTabFilterBy = discoverTabFilterBy,
                discoverTabQueryBy = discoverTabQueryBy
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
                    EarnType.Passive -> {
                        getAccountForInterest(currency.networkTicker)
                    }
                    EarnType.Staking -> navigate(
                        EarnDashboardNavigationEvent.OpenStakingSummarySheet(currency.networkTicker)
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
                                },
                                availableToBuy, tradingAccount
                            )
                        )
                    }.firstOrNull()
            }
        } ?: Timber.e("Earn Dashboard - unknown asset $assetTicker")
    }

    private suspend fun getAccountForInterest(assetTicker: String) {
        assetCatalogue.fromNetworkTicker(assetTicker)?.let {
            navigate(
                EarnDashboardNavigationEvent.OpenRewardsSummarySheet(
                    coincore[it].accountGroup(AssetFilter.Interest).awaitSingle().accounts.first() as CryptoAccount
                )
            )
        }
    }

    private fun buildDiscoverList(
        data: CombinedEarnData,
        discoverTabFilterBy: EarnDashboardListFilter,
        discoverTabQueryBy: String
    ): DashboardState.OnlyDiscover {
        val discoverList = mutableListOf<EarnAsset>()
        data.stakingEligibility.map { (asset, eligibility) ->
            discoverList.add(
                asset.createStakingAsset(
                    stakingBalancesWithFiat = StakingBalancesWithFiat(
                        asset, StakingAccountBalance.zeroBalance(asset), Money.zero(asset)
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
                        asset, InterestAccountBalance.zeroBalance(asset), Money.zero(asset)
                    ),
                    passiveRate = data.interestRates[asset],
                    eligibility = eligibility
                )
            )
        }

        return DashboardState.OnlyDiscover(
            discoverList.sortListByFilterAndQuery(discoverTabFilterBy, discoverTabQueryBy).sortByRate()
        )
    }

    private fun splitEarningAndDiscoverData(
        data: CombinedEarnData,
        earningTabFilterBy: EarnDashboardListFilter,
        earningTabQueryBy: String,
        discoverTabFilterBy: EarnDashboardListFilter,
        discoverTabQueryBy: String
    ): DashboardState.EarningAndDiscover {
        val earningList = mutableListOf<EarnAsset>()
        val discoverList = mutableListOf<EarnAsset>()

        data.stakingEligibility.map { (asset, eligibility) ->
            data.stakingBalancesWithFiat[asset]?.let { balances ->
                val totalBalance = balances.stakingCryptoBalances.totalBalance

                val stakingAsset = asset.createStakingAsset(
                    stakingBalancesWithFiat = balances,
                    stakingRate = data.stakingRates[asset],
                    eligibility = eligibility
                )

                discoverList.add(stakingAsset)
                if (totalBalance.isPositive) {
                    earningList.add(stakingAsset)
                }
            } ?: discoverList.add(
                asset.createStakingAsset(
                    stakingBalancesWithFiat = StakingBalancesWithFiat(
                        asset, StakingAccountBalance.zeroBalance(asset), Money.zero(asset)
                    ),
                    stakingRate = data.stakingRates[asset],
                    eligibility = eligibility
                )
            )
        }

        data.interestEligibility.map { (asset, eligibility) ->
            data.interestBalancesWithFiat[asset]?.let { balances ->
                val totalBalance = balances.interestCryptoBalances.totalBalance

                val passiveAsset = asset.createPassiveAsset(
                    interestBalancesWithFiat = balances,
                    passiveRate = data.interestRates[asset],
                    eligibility = eligibility
                )

                discoverList.add(passiveAsset)
                if (totalBalance.isPositive) {
                    earningList.add(passiveAsset)
                }
            } ?: discoverList.add(
                asset.createPassiveAsset(
                    interestBalancesWithFiat = InterestBalancesWithFiat(
                        asset, InterestAccountBalance.zeroBalance(asset), Money.zero(asset)
                    ),
                    passiveRate = data.interestRates[asset],
                    eligibility = eligibility
                )
            )
        }

        return DashboardState.EarningAndDiscover(
            earningList.sortListByFilterAndQuery(earningTabFilterBy, earningTabQueryBy).sortByBalance(),
            discoverList.sortListByFilterAndQuery(discoverTabFilterBy, discoverTabQueryBy).sortByRate()
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

    private fun List<EarnAsset>.sortListByFilterAndQuery(
        filter: EarnDashboardListFilter,
        query: String
    ): List<EarnAsset> =
        when (filter) {
            EarnDashboardListFilter.All -> this
            EarnDashboardListFilter.Staking -> this.filter { it.type == EarnType.Staking }
            EarnDashboardListFilter.Rewards -> this.filter { it.type == EarnType.Passive }
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
        updateState {
            it.copy(
                isLoading = true
            )
        }

        collectEarnData(true)
    }

    private suspend fun collectEarnData(showLoading: Boolean) {
        val accessMap = try {
            userIdentity.userAccessForFeatures(listOf(Feature.DepositStaking, Feature.DepositInterest)).await()
        } catch (e: Exception) {
            mapOf(
                Feature.DepositStaking to FeatureAccess.Blocked(BlockedReason.NotEligible("")),
                Feature.DepositInterest to FeatureAccess.Blocked(BlockedReason.NotEligible(""))
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

        combine(
            stakingBalanceWithFiatFlow,
            stakingService.getEligibilityForAssets(),
            stakingService.getRatesForAllAssets(),
            interestBalanceWithFiatFlow,
            interestService.getEligibilityForAssets(),
            interestService.getAllInterestRates(),
        ) { listOfData ->
            require(listOfData.size == 6)
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
                    interestFeatureAccess = accessMap[Feature.DepositInterest]!!,
                    stakingFeatureAccess = accessMap[Feature.DepositStaking]!!
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

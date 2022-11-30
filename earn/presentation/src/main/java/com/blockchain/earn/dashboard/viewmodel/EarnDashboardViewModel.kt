package com.blockchain.earn.dashboard.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.domain.eligibility.model.StakingEligibility
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.interest.InterestEligibility
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitSingle

class EarnDashboardViewModel(
    private val coincore: Coincore,
    private val stakingService: StakingService,
    private val interestService: InterestService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val userIdentity: UserIdentity,
    private val assetCatalogue: AssetCatalogue
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
                when (val eligibility = intent.earnAsset.eligibility) {
                    EarnEligibility.Eligible -> showSummaryForEarnType(
                        earnType = intent.earnAsset.type,
                        assetTicker = intent.earnAsset.assetTicker
                    )
                    is EarnEligibility.NotEligible -> {
                        when (eligibility.reason) {
                            EarnIneligibleReason.REGION -> navigate(
                                EarnDashboardNavigationEvent.OpenBlockedForRegionSheet(intent.earnAsset.type)
                            )
                            EarnIneligibleReason.KYC_TIER -> navigate(
                                EarnDashboardNavigationEvent.OpenBlockedForKycSheet(intent.earnAsset.type)
                            )
                            EarnIneligibleReason.OTHER -> navigate(
                                EarnDashboardNavigationEvent.OpenBlockedForOtherSheet(intent.earnAsset.type)
                            )
                        }
                    }
                }
            }
            is EarnDashboardIntent.EarningItemSelected ->
                showSummaryForEarnType(
                    earnType = intent.earnAsset.type,
                    assetTicker = intent.earnAsset.assetTicker
                )
        }

    private suspend fun showSummaryForEarnType(earnType: EarnType, assetTicker: String) =
        when (earnType) {
            EarnType.Rewards -> {
                getAccountForInterest(assetTicker)
            }
            EarnType.Staking -> navigate(
                EarnDashboardNavigationEvent.OpenStakingSummarySheet(assetTicker)
            )
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
            isLoading -> DashboardState.Loading
            error != EarnDashboardError.None -> DashboardState.ShowError(error)
            else -> earnData?.let { data ->
                val hasStakingBalance = data.stakingBalances.values.any { it.totalBalance.isPositive }
                val hasInterestBalance = data.interestBalances.values.any { it.totalBalance.isPositive }

                if (data.interestFeatureAccess !is FeatureAccess.Granted && !hasInterestBalance &&
                    data.stakingFeatureAccess !is FeatureAccess.Granted && !hasStakingBalance
                ) {
                    return DashboardState.ShowKyc
                }

                return if (hasStakingBalance || hasInterestBalance) {
                    splitEarningAndDiscoverData(
                        data, earningTabFilterBy, earningTabQueryBy, discoverTabFilterBy, discoverTabQueryBy
                    )
                } else {
                    buildDiscoverList(data, discoverTabFilterBy, discoverTabQueryBy)
                }
            } ?: DashboardState.Loading
        }

    private fun buildDiscoverList(
        data: CombinedEarnData,
        discoverTabFilterBy: EarnDashboardListFilter,
        discoverTabQueryBy: String
    ): DashboardState.OnlyDiscover {
        val discoverList = mutableListOf<EarnAsset>()

        data.stakingEligibility.map { (asset, eligibility) ->
            val balance = data.stakingBalances[asset]?.totalBalance ?: Money.zero(asset)
            discoverList.add(
                asset.createStakingAsset(
                    balance = balance,
                    stakingRate = data.stakingRates[asset],
                    eligibility = eligibility
                )
            )
        }

        data.interestEligibility.map { (asset, eligibility) ->
            val balance = data.interestBalances[asset]?.totalBalance ?: Money.zero(asset)
            discoverList.add(
                asset.createRewardsAsset(
                    balance = balance,
                    rewardsRate = data.interestRates[asset],
                    eligibility = eligibility
                )
            )
        }

        return DashboardState.OnlyDiscover(
            discoverList.sortListByFilterAndQuery(discoverTabFilterBy, discoverTabQueryBy)
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
            val balance = data.stakingBalances[asset]?.totalBalance ?: Money.zero(asset)
            if (balance.isPositive) {
                earningList.add(
                    asset.createStakingAsset(
                        balance = balance,
                        stakingRate = data.stakingRates[asset],
                        eligibility = eligibility
                    )
                )
            } else {
                discoverList.add(
                    asset.createStakingAsset(
                        balance = balance,
                        stakingRate = data.stakingRates[asset],
                        eligibility = eligibility
                    )
                )
            }
        }

        data.interestEligibility.map { (asset, eligibility) ->
            val balance = data.interestBalances[asset]?.totalBalance ?: Money.zero(asset)

            if (balance.isPositive) {
                earningList.add(
                    asset.createRewardsAsset(
                        balance = balance,
                        rewardsRate = data.interestRates[asset],
                        eligibility = eligibility
                    )
                )
            } else {
                discoverList.add(
                    asset.createRewardsAsset(
                        balance = balance,
                        rewardsRate = data.interestRates[asset],
                        eligibility = eligibility
                    )
                )
            }
        }

        return DashboardState.EarningAndDiscover(
            earningList.sortListByFilterAndQuery(earningTabFilterBy, earningTabQueryBy),
            discoverList.sortListByFilterAndQuery(discoverTabFilterBy, discoverTabQueryBy)
        )
    }

    private fun AssetInfo.createStakingAsset(balance: Money, stakingRate: Double?, eligibility: StakingEligibility) =
        EarnAsset(
            assetTicker = networkTicker,
            assetName = name,
            iconUrl = logo,
            rate = stakingRate ?: 0.0,
            eligibility = eligibility.toEarnEligibility(),
            balanceCrypto = balance,
            balanceFiat = balance.toUserFiat(exchangeRatesDataManager),
            type = EarnType.Staking
        )

    private fun AssetInfo.createRewardsAsset(balance: Money, rewardsRate: Double?, eligibility: InterestEligibility) =
        EarnAsset(
            assetTicker = networkTicker,
            assetName = name,
            iconUrl = logo,
            rate = rewardsRate ?: 0.0,
            eligibility = eligibility.toEarnEligibility(),
            balanceCrypto = balance,
            balanceFiat = balance.toUserFiat(exchangeRatesDataManager),
            type = EarnType.Rewards
        )

    private fun List<EarnAsset>.sortListByFilterAndQuery(
        filter: EarnDashboardListFilter,
        query: String
    ): List<EarnAsset> =
        when (filter) {
            EarnDashboardListFilter.All -> this
            EarnDashboardListFilter.Staking -> this.filter { it.type == EarnType.Staking }
            EarnDashboardListFilter.Rewards -> this.filter { it.type == EarnType.Rewards }
        }.filter {
            query.isEmpty() || it.assetName.contains(query, true) ||
                it.assetTicker.contains(query, true)
        }

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
            userIdentity.userAccessForFeatures(
                listOf(Feature.DepositStaking, Feature.DepositInterest)
            ).await()
        } catch (e: Exception) {
            mapOf(
                Feature.DepositStaking to FeatureAccess.Blocked(BlockedReason.NotEligible("")),
                Feature.DepositInterest to FeatureAccess.Blocked(BlockedReason.NotEligible(""))
            )
        }

        combine(
            stakingService.getBalanceForAllAssets(),
            stakingService.getEligibilityForAssets(),
            stakingService.getRatesForAllAssets(),
            interestService.getBalancesFlow(),
            interestService.getEligibilityForAssets(),
            interestService.getAllInterestRates(),
        ) { listOfData ->
            require(listOfData.size == 6)
            combineDataResources(
                listOfData.toList()
            ) { data ->
                CombinedEarnData(
                    stakingBalances = data[0] as Map<AssetInfo, StakingAccountBalance>,
                    stakingEligibility = data[1] as Map<AssetInfo, StakingEligibility>,
                    stakingRates = data[2] as Map<AssetInfo, Double>,
                    interestBalances = data[3] as Map<AssetInfo, InterestAccountBalance>,
                    interestEligibility = data[4] as Map<AssetInfo, InterestEligibility>,
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

    private fun StakingEligibility.toEarnEligibility(): EarnEligibility =
        if (this is StakingEligibility.Eligible) {
            EarnEligibility.Eligible
        } else {
            when (this as StakingEligibility.Ineligible) {
                StakingEligibility.Ineligible.REGION -> EarnEligibility.NotEligible(EarnIneligibleReason.REGION)
                StakingEligibility.Ineligible.KYC_TIER -> EarnEligibility.NotEligible(EarnIneligibleReason.KYC_TIER)
                StakingEligibility.Ineligible.OTHER -> EarnEligibility.NotEligible(EarnIneligibleReason.OTHER)
                StakingEligibility.Ineligible.NONE -> EarnEligibility.NotEligible(EarnIneligibleReason.OTHER)
            }
        }

    private fun InterestEligibility.toEarnEligibility(): EarnEligibility =
        if (this is InterestEligibility.Eligible) {
            EarnEligibility.Eligible
        } else {
            when (this as InterestEligibility.Ineligible) {
                InterestEligibility.Ineligible.REGION -> EarnEligibility.NotEligible(EarnIneligibleReason.REGION)
                InterestEligibility.Ineligible.KYC_TIER -> EarnEligibility.NotEligible(EarnIneligibleReason.KYC_TIER)
                InterestEligibility.Ineligible.OTHER -> EarnEligibility.NotEligible(EarnIneligibleReason.OTHER)
                InterestEligibility.Ineligible.NONE -> EarnEligibility.NotEligible(EarnIneligibleReason.OTHER)
            }
        }
}

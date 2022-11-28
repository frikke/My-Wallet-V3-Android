package com.blockchain.earn.dashboard.viewmodel

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
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
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

class EarnDashboardViewModel(
    private val coincore: Coincore,
    private val stakingService: StakingService,
    private val interestService: InterestService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val userIdentity: UserIdentity
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
            dashboardState = reduceDashboardState(isLoading, error, earnData)
        )
    }

    override suspend fun handleIntent(modelState: EarnDashboardModelState, intent: EarnDashboardIntent) {
        when (intent) {
            is EarnDashboardIntent.LoadEarn -> loadEarn()
        }
    }

    private fun reduceDashboardState(
        isLoading: Boolean,
        error: EarnDashboardError,
        earnData: CombinedEarnData?
    ): DashboardState {
        if (isLoading) {
            return DashboardState.Loading
        }

        if (error != EarnDashboardError.None) {
            return DashboardState.ShowError(error)
        }

        require(earnData != null)

        val hasStakingBalance = earnData.stakingBalances.values.any { it.totalBalance.isPositive }
        val hasInterestBalance = earnData.interestBalances.values.any { it.totalBalance.isPositive }

        if (earnData.interestFeatureAccess !is FeatureAccess.Granted && !hasInterestBalance &&
            earnData.stakingFeatureAccess !is FeatureAccess.Granted && !hasStakingBalance
        ) {
            return DashboardState.ShowKyc
        }

        if (hasStakingBalance || hasInterestBalance) {
            val earningList = mutableListOf<EarnAsset>()
            val discoverList = mutableListOf<EarnAsset>()
            earnData.stakingEligibility.map { (asset, eligibility) ->

                val balance = earnData.stakingBalances[asset]?.totalBalance ?: Money.zero(asset)
                if (balance.isPositive) {
                    earningList.add(
                        EarnAsset(
                            asset,
                            earnData.stakingRates[asset] ?: 0.0,
                            eligibility.toEarnEligibility(),
                            balance,
                            balance.toUserFiat(exchangeRatesDataManager)
                        )
                    )
                } else {
                    discoverList.add(
                        EarnAsset(
                            asset,
                            earnData.stakingRates[asset] ?: 0.0,
                            eligibility.toEarnEligibility(),
                            balance,
                            balance.toUserFiat(exchangeRatesDataManager)
                        )
                    )
                }
            }

            earnData.interestEligibility.map { (asset, eligibility) ->

                val balance = earnData.interestBalances[asset]?.totalBalance ?: Money.zero(asset)

                if (balance.isPositive) {
                    earningList.add(
                        EarnAsset(
                            asset,
                            earnData.interestRates[asset] ?: 0.0,
                            eligibility.toEarnEligibility(),
                            balance,
                            balance.toUserFiat(exchangeRatesDataManager)
                        )
                    )
                } else {
                    discoverList.add(
                        EarnAsset(
                            asset,
                            earnData.interestRates[asset] ?: 0.0,
                            eligibility.toEarnEligibility(),
                            balance,
                            balance.toUserFiat(exchangeRatesDataManager)
                        )
                    )
                }
            }
            return DashboardState.EarningAndDiscover(
                earningList, discoverList
            )
        } else {
            val discoverList = mutableListOf<EarnAsset>()

            earnData.stakingEligibility.map { (asset, eligibility) ->

                val balance = earnData.stakingBalances[asset]?.totalBalance ?: Money.zero(asset)

                discoverList.add(
                    EarnAsset(
                        asset,
                        earnData.stakingRates[asset] ?: 0.0,
                        eligibility.toEarnEligibility(),
                        balance,
                        balance.toUserFiat(exchangeRatesDataManager)
                    )
                )
            }

            earnData.interestEligibility.map { (asset, eligibility) ->

                val balance = earnData.interestBalances[asset]?.totalBalance ?: Money.zero(asset)

                discoverList.add(
                    EarnAsset(
                        asset,
                        earnData.interestRates[asset] ?: 0.0,
                        eligibility.toEarnEligibility(),
                        balance,
                        balance.toUserFiat(exchangeRatesDataManager)
                    )
                )
            }
            return DashboardState.OnlyDiscover(discoverList)
        }
    }

    private suspend fun loadEarn() {
        updateState {
            it.copy(
                isLoading = true
            )
        }

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
                        it.copy(isLoading = true)
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

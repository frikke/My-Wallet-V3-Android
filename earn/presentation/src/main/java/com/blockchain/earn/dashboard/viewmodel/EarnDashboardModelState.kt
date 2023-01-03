package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.domain.eligibility.model.StakingEligibility
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.interest.InterestEligibility
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.nabu.FeatureAccess
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class EarnDashboardModelState(
    val isLoading: Boolean = true,
    val error: EarnDashboardError = EarnDashboardError.None,
    val earnData: CombinedEarnData? = null,
    val earningTabQueryBy: String = "",
    val earningTabFilterBy: EarnDashboardListFilter = EarnDashboardListFilter.All,
    val discoverTabQueryBy: String = "",
    val discoverTabFilterBy: EarnDashboardListFilter = EarnDashboardListFilter.All
) : ModelState

data class CombinedEarnData(
    val stakingBalancesWithFiat: Map<AssetInfo, StakingBalancesWithFiat>,
    val stakingEligibility: Map<AssetInfo, StakingEligibility>,
    val stakingRates: Map<AssetInfo, Double>,
    val interestBalancesWithFiat: Map<AssetInfo, InterestBalancesWithFiat>,
    val interestEligibility: Map<AssetInfo, InterestEligibility>,
    val interestRates: Map<AssetInfo, Double>,
    val interestFeatureAccess: FeatureAccess,
    val stakingFeatureAccess: FeatureAccess
)

data class StakingBalancesWithFiat(
    val asset: AssetInfo,
    val stakingCryptoBalances: StakingAccountBalance,
    val stakingTotalFiat: Money
)

data class InterestBalancesWithFiat(
    val asset: AssetInfo,
    val interestCryptoBalances: InterestAccountBalance,
    val interestTotalFiat: Money
)

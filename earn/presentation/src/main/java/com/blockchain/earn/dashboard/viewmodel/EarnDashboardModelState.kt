package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.domain.eligibility.model.EarnRewardsEligibility
import com.blockchain.earn.domain.models.EarnAccountBalance
import com.blockchain.earn.domain.models.active.ActiveRewardsAccountBalance
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
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
    val discoverTabFilterBy: EarnDashboardListFilter = EarnDashboardListFilter.All,
    val filterList: List<EarnDashboardListFilter> = emptyList(),
    val hasSeenEarnIntro: Boolean = false
) : ModelState

data class CombinedEarnData(
    val stakingBalancesWithFiat: Map<AssetInfo, EarnBalanceWithFiat.StakingBalanceWithFiat>,
    val stakingEligibility: Map<AssetInfo, EarnRewardsEligibility>,
    val stakingRates: Map<AssetInfo, Double>,
    val interestBalancesWithFiat: Map<AssetInfo, EarnBalanceWithFiat.InterestBalanceWithFiat>,
    val interestEligibility: Map<AssetInfo, EarnRewardsEligibility>,
    val interestRates: Map<AssetInfo, Double>,
    val activeRewardsBalancesWithFiat: Map<AssetInfo, EarnBalanceWithFiat.ActiveRewardsBalanceWithFiat>,
    val activeRewardsEligibility: Map<AssetInfo, EarnRewardsEligibility>,
    val activeRewardsRates: Map<AssetInfo, Double>,
    val interestFeatureAccess: FeatureAccess,
    val stakingFeatureAccess: FeatureAccess,
    val activeRewardsFeatureAccess: FeatureAccess
)

sealed interface EarnBalanceWithFiat {
    val asset: AssetInfo
    val cryptoBalance: EarnAccountBalance
    val totalFiat: Money?

    data class StakingBalanceWithFiat(
        override val asset: AssetInfo,
        override val cryptoBalance: StakingAccountBalance,
        override val totalFiat: Money?
    ) : EarnBalanceWithFiat

    data class InterestBalanceWithFiat(
        override val asset: AssetInfo,
        override val cryptoBalance: InterestAccountBalance,
        override val totalFiat: Money?
    ) : EarnBalanceWithFiat

    data class ActiveRewardsBalanceWithFiat(
        override val asset: AssetInfo,
        override val cryptoBalance: ActiveRewardsAccountBalance,
        override val totalFiat: Money?
    ) : EarnBalanceWithFiat
}

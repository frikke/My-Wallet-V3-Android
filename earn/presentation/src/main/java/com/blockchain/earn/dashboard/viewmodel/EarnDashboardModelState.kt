package com.blockchain.earn.dashboard.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.domain.eligibility.model.StakingEligibility
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.models.interest.InterestEligibility
import com.blockchain.earn.domain.models.staking.StakingAccountBalance
import com.blockchain.nabu.FeatureAccess
import info.blockchain.balance.AssetInfo

data class EarnDashboardModelState(
    val isLoading: Boolean = true,
    val hasAccessToFeature: Boolean = false,
    val error: EarnDashboardError = EarnDashboardError.None,
    val earnData: CombinedEarnData? = null
) : ModelState

data class CombinedEarnData(
    val stakingBalances: Map<AssetInfo, StakingAccountBalance>,
    val stakingEligibility: Map<AssetInfo, StakingEligibility>,
    val stakingRates: Map<AssetInfo, Double>,
    val interestBalances: Map<AssetInfo, InterestAccountBalance>,
    val interestEligibility: Map<AssetInfo, InterestEligibility>,
    val interestRates: Map<AssetInfo, Double>,
    val interestFeatureAccess: FeatureAccess,
    val stakingFeatureAccess: FeatureAccess
)

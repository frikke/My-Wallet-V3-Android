package com.blockchain.earn.staking.viewmodel

import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.Money

data class StakingSummaryModelState(
    val account: EarnRewardsAccount.Staking? = null,
    val errorState: StakingError = StakingError.None,
    val isLoading: Boolean = true,
    val balance: Money? = null,
    val staked: Money? = null,
    val bonding: Money? = null,
    val totalEarned: Money? = null,
    val stakingRate: Double = 0.0,
    val stakingCommission: Double = 0.0,
    val isWithdrawable: Boolean = true,
    val frequency: EarnRewardsFrequency = EarnRewardsFrequency.Unknown,
    val canDeposit: Boolean = false
) : ModelState

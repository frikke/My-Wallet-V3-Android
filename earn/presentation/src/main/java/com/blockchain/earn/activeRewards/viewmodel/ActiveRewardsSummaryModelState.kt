package com.blockchain.earn.activeRewards.viewmodel

import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.Money

data class ActiveRewardsSummaryModelState(
    val account: EarnRewardsAccount.Active? = null,
    val errorState: ActiveRewardsError = ActiveRewardsError.None,
    val isLoading: Boolean = true,
    val balance: Money? = null,
    val assetFiatPrice: Money? = null,
    val totalEarned: Money? = null,
    val totalSubscribed: Money? = null,
    val totalOnHold: Money? = null,
    val activeRewardsRate: Double = 0.0,
    val triggerPrice: Money? = null,
    val rewardsFrequency: EarnRewardsFrequency = EarnRewardsFrequency.Unknown,
    val isWithdrawable: Boolean = false,
    val canDeposit: Boolean = false
) : ModelState

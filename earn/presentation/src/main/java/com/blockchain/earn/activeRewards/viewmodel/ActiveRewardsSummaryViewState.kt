package com.blockchain.earn.activeRewards.viewmodel

import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.Money

data class ActiveRewardsSummaryViewState(
    val account: EarnRewardsAccount.Active?,
    val errorState: ActiveRewardsError,
    val isLoading: Boolean,
    val assetFiatPrice: Money?,
    val balanceCrypto: Money?,
    val balanceFiat: Money?,
    val totalEarnedCrypto: Money?,
    val totalEarnedFiat: Money?,
    val totalSubscribedCrypto: Money?,
    val totalSubscribedFiat: Money?,
    val totalOnHoldCrypto: Money?,
    val totalOnHoldFiat: Money?,
    val activeRewardsRate: Double,
    val triggerPrice: Money?,
    val rewardsFrequency: EarnRewardsFrequency,
    val isWithdrawable: Boolean,
    val canDeposit: Boolean
) : ViewState

sealed class ActiveRewardsError {
    class UnknownAsset(val assetTicker: String) : ActiveRewardsError()
    object Other : ActiveRewardsError()
    object None : ActiveRewardsError()
}

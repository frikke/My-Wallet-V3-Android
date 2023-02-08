package com.blockchain.earn.staking.viewmodel

import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.Money

data class StakingSummaryViewState(
    val account: EarnRewardsAccount.Staking?,
    val errorState: StakingError,
    val isLoading: Boolean,
    val balanceCrypto: Money?,
    val balanceFiat: Money?,
    val stakedCrypto: Money?,
    val stakedFiat: Money?,
    val bondingCrypto: Money?,
    val bondingFiat: Money?,
    val earnedCrypto: Money?,
    val earnedFiat: Money?,
    val stakingRate: Double,
    val commissionRate: Double,
    val isWithdrawable: Boolean,
    val rewardsFrequency: EarnRewardsFrequency,
    val canDeposit: Boolean
) : ViewState

sealed class StakingError {
    class UnknownAsset(val assetTicker: String) : StakingError()
    object Other : StakingError()
    object None : StakingError()
}

package com.blockchain.earn.staking.viewmodel

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.Money

data class StakingSummaryViewState(
    val account: BlockchainAccount?,
    val tradingAccount: CustodialTradingAccount?,
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
    val earnFrequency: EarnRewardsFrequency,
    val canDeposit: Boolean,
    val canWithdraw: Boolean,
    val pendingWithdrawals: List<EarnWithdrawalUiElement>
) : ViewState

data class EarnWithdrawalUiElement(
    val currency: String,
    val amountCrypto: String,
    val amountFiat: String,
    val unbondingStartDate: String,
    val unbondingExpiryDate: String,
)

sealed class StakingError {
    class UnknownAsset(val assetTicker: String) : StakingError()
    object Other : StakingError()
    object None : StakingError()
}

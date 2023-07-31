package com.blockchain.earn.interest.viewmodel

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.Money
import java.util.Date

data class InterestSummaryViewState(
    val account: BlockchainAccount?,
    val errorState: InterestError,
    val isLoading: Boolean,
    val balanceCrypto: Money?,
    val balanceFiat: Money?,
    val totalEarnedCrypto: Money?,
    val totalEarnedFiat: Money?,
    val pendingInterestCrypto: Money?,
    val pendingInterestFiat: Money?,
    val interestRate: Double,
    val interestCommission: Double,
    val earnFrequency: EarnRewardsFrequency,
    val nextPaymentDate: Date?,
    val initialHoldPeriod: Int,
    val canWithdraw: Boolean,
    val canDeposit: Boolean
) : ViewState

sealed class InterestError {
    class UnknownAsset(val assetTicker: String) : InterestError()
    object Other : InterestError()
    object None : InterestError()
}

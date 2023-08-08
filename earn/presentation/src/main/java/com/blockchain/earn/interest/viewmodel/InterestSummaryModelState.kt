package com.blockchain.earn.interest.viewmodel

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.util.Date

data class InterestSummaryModelState(
    val account: BlockchainAccount? = null,
    val errorState: InterestError = InterestError.None,
    val isLoading: Boolean = true,
    val exchangeRate: ExchangeRate? = null,
    val balance: Money? = null,
    val totalEarned: Money? = null,
    val pendingInterest: Money? = null,
    val interestRate: Double = 0.0,
    val interestCommission: Double = 0.0,
    val earnFrequency: EarnRewardsFrequency = EarnRewardsFrequency.Unknown,
    val nextPaymentDate: Date? = null,
    val initialHoldPeriod: Int = 0,
    val canWithdraw: Boolean = true,
    val canDeposit: Boolean = false
) : ModelState

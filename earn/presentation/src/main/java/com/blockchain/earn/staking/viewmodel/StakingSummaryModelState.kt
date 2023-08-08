package com.blockchain.earn.staking.viewmodel

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.earn.domain.models.staking.StakingActivity
import info.blockchain.balance.Money

data class StakingSummaryModelState(
    val account: BlockchainAccount? = null,
    val tradingAccount: CustodialTradingAccount? = null,
    val errorState: StakingError = StakingError.None,
    val isLoading: Boolean = true,
    val balance: Money? = null,
    val staked: Money? = null,
    val bonding: Money? = null,
    val totalEarned: Money? = null,
    val stakingRate: Double = 0.0,
    val stakingCommission: Double = 0.0,
    val frequency: EarnRewardsFrequency = EarnRewardsFrequency.Unknown,
    val canDeposit: Boolean = false,
    val canWithdraw: Boolean = false,
    val pendingActivity: List<StakingActivity> = emptyList(),
    val unbondingDays: Int = 2
) : ModelState

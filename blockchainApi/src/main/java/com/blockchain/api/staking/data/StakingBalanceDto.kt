package com.blockchain.api.staking.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StakingBalanceDto(
    @SerialName("balance")
    val totalBalance: String,
    // going through payments processing - after a deposit
    @SerialName("pendingDeposit")
    val pendingDeposit: String,
    // going through payments processing - after unbonding has happened
    @SerialName("pendingWithdrawal")
    val pendingWithdrawal: String,
    @SerialName("totalRewards")
    val totalRewards: String,
    @SerialName("pendingRewards")
    val pendingRewards: String,
    // going through staking holding period - once payments processing is done
    @SerialName("bondingDeposits")
    val bondingDeposits: String,
    // going through staking holding period - once withdrawal request is processed on-chain
    @SerialName("unbondingWithdrawals")
    val unbondingWithdrawals: String,
    @SerialName("locked")
    val lockedBalance: String,
)

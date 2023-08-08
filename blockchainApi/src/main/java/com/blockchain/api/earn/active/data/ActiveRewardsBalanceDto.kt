package com.blockchain.api.earn.active.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActiveRewardsBalanceDto(
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
    // going through holding period - once payments processing is done
    @SerialName("bondingDeposits")
    val bondingDeposits: String,
    // going through holding period - once withdrawal request is processed on-chain
    @SerialName("unbondingWithdrawals")
    val unbondingWithdrawals: String,
    // total subscribed balance that's earning rewards
    @SerialName("earningBalance")
    val earningBalance: String,
    @SerialName("locked")
    val lockedBalance: String
)

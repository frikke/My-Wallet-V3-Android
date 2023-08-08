package com.blockchain.api.earn.staking.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StakingActivityDto(
    @SerialName("bondingDeposits")
    val deposits: List<StakingDepositActivityDto>,
    @SerialName("unbondingWithdrawals")
    val withdrawals: List<StakingWithdrawalActivityDto>
)

@Serializable
data class StakingDepositActivityDto(
    val amount: String,
    val bondingDays: Int,
    val bondingExpiryDate: String,
    val bondingStartDate: String,
    val currency: String,
    val insertedAt: String,
    val isCustodialTransfer: Boolean,
    val paymentRef: String,
    val product: String,
    val userId: String
)

@Serializable
data class StakingWithdrawalActivityDto(
    val amount: String,
    val currency: String,
    val insertedAt: String,
    val paymentRef: String,
    val product: String,
    val unbondingDays: Int,
    val unbondingExpiryDate: String,
    val unbondingStartDate: String,
    val userId: String
)

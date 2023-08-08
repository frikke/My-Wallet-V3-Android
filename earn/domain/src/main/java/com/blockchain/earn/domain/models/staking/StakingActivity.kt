package com.blockchain.earn.domain.models.staking

import info.blockchain.balance.Money
import java.util.Date

enum class StakingActivityType {
    Bonding,
    Unbonding
}

data class StakingActivity(
    val product: String,
    val currency: String,
    val amountCrypto: Money?,
    val startDate: Date?,
    val expiryDate: Date?,
    val durationDays: Int,
    val type: StakingActivityType
)

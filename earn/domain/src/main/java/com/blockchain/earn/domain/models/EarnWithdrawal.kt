package com.blockchain.earn.domain.models

import info.blockchain.balance.Money
import java.util.Date

data class EarnWithdrawal(
    val product: String,
    val currency: String,
    val amountCrypto: Money?,
    val unbondingStartDate: Date?,
    val unbondingExpiryDate: Date?,
    val userId: String,
    val maxRequested: Boolean
)

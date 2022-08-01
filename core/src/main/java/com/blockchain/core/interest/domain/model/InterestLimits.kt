package com.blockchain.core.interest.domain.model

import info.blockchain.balance.Money
import java.util.Date

data class InterestLimits(
    val interestLockUpDuration: Int,
    val nextInterestPayment: Date,
    val minDepositFiatValue: Money,
    val maxWithdrawalFiatValue: Money
)

package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.Money
import java.io.Serializable
import java.time.ZonedDateTime

data class FundsLocks(
    val onHoldTotalAmount: Money,
    val locks: List<FundsLock>
) : Serializable

data class FundsLock(
    val amount: Money,
    val date: ZonedDateTime,
    val buyAmount: Money?,
) : Serializable

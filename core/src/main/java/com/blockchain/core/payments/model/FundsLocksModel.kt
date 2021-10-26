package com.blockchain.core.payments.model

import info.blockchain.balance.FiatValue
import java.io.Serializable
import java.time.ZonedDateTime

data class FundsLocks(
    val onHoldTotalAmount: FiatValue,
    val locks: List<FundsLock>
) : Serializable

data class FundsLock(
    val amount: FiatValue,
    val date: ZonedDateTime
) : Serializable
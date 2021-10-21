package com.blockchain.core.payments.model

import info.blockchain.balance.FiatValue
import java.io.Serializable
import java.time.ZonedDateTime

data class WithdrawalsLocks(
    val onHoldTotalAmount: FiatValue,
    val locks: List<WithdrawalLock>
) : Serializable

data class WithdrawalLock(
    val amount: FiatValue,
    val date: ZonedDateTime
) : Serializable
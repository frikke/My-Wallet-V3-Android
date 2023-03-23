package com.blockchain.core.recurringbuy.domain.model

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.RecurringBuyPaymentDetails
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import java.io.Serializable
import java.util.Date

data class RecurringBuy(
    val id: String,
    val state: RecurringBuyState,
    val recurringBuyFrequency: RecurringBuyFrequency,
    val nextPaymentDate: Date,
    val paymentMethodType: PaymentMethodType,
    val paymentMethodId: String?,
    val amount: Money,
    val asset: AssetInfo,
    val createDate: Date,
    val paymentDetails: RecurringBuyPaymentDetails? = null
) : Serializable

enum class RecurringBuyState {
    ACTIVE,
    INACTIVE,
    UNINITIALISED
}

fun RecurringBuy.isActive() = state == RecurringBuyState.ACTIVE

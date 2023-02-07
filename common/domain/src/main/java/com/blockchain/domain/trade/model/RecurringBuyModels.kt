package com.blockchain.domain.trade.model

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.RecurringBuyPaymentDetails
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import java.io.Serializable
import java.util.Date

enum class RecurringBuyState {
    ACTIVE,
    INACTIVE,
    UNINITIALISED
}

enum class RecurringBuyFrequency {
    // TODO(aromano): ONE_TIME 'recurring' buy doesn't make sense, if there's no recurring buy selected it should just be null
    //                On top of this, it's a concept used only by Buy flow UI, the API doesn't have this
    ONE_TIME,
    DAILY,
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    UNKNOWN
}

@kotlinx.serialization.Serializable
data class EligibleAndNextPaymentRecurringBuy(
    val frequency: RecurringBuyFrequency,
    val nextPaymentDate: String,
    val eligibleMethods: List<PaymentMethodType>
)

data class FundsAccount(val currency: String) : RecurringBuyPaymentDetails {
    override val paymentDetails: PaymentMethodType
        get() = PaymentMethodType.FUNDS
}

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

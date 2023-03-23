package com.blockchain.core.recurringbuy.domain.model

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import java.io.Serializable

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

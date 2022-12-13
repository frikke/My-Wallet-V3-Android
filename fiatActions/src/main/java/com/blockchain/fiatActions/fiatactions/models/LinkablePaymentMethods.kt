package com.blockchain.fiatActions.fiatactions.models

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import info.blockchain.balance.FiatCurrency
import java.io.Serializable

data class LinkablePaymentMethods(
    val currency: FiatCurrency,
    val linkMethods: List<PaymentMethodType>
) : Serializable

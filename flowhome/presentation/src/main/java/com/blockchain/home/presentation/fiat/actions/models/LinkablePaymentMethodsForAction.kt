package com.blockchain.home.presentation.fiat.actions.models

import java.io.Serializable

sealed class LinkablePaymentMethodsForAction(
    open val linkablePaymentMethods: LinkablePaymentMethods
) : Serializable {
    data class LinkablePaymentMethodsForSettings(
        override val linkablePaymentMethods: LinkablePaymentMethods
    ) : LinkablePaymentMethodsForAction(linkablePaymentMethods)

    data class LinkablePaymentMethodsForDeposit(
        override val linkablePaymentMethods: LinkablePaymentMethods
    ) : LinkablePaymentMethodsForAction(linkablePaymentMethods)

    data class LinkablePaymentMethodsForWithdraw(
        override val linkablePaymentMethods: LinkablePaymentMethods
    ) : LinkablePaymentMethodsForAction(linkablePaymentMethods)
}

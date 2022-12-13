package com.blockchain.fiatActions

import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import info.blockchain.balance.FiatCurrency

interface BankLinkingHost : SlidingModalBottomDialog.Host {
    fun onBankWireTransferSelected(currency: FiatCurrency)
    fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction)
}

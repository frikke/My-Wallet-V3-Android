package com.blockchain.tempsheetinterfaces

import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.tempsheetinterfaces.fiatactions.models.LinkablePaymentMethodsForAction
import info.blockchain.balance.FiatCurrency

interface BankLinkingHost : SlidingModalBottomDialog.Host {
    fun onBankWireTransferSelected(currency: FiatCurrency)
    fun onLinkBankSelected(paymentMethodForAction: LinkablePaymentMethodsForAction)
}

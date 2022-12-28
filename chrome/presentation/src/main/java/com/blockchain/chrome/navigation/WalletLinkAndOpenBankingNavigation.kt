package com.blockchain.chrome.navigation

import com.blockchain.domain.paymentmethods.model.BankLinkingInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

interface WalletLinkAndOpenBankingNavigation {
    fun walletLinkError(walletIdHint: String)
    fun depositComplete(amount: Money, estimationTime: String)
    fun depositInProgress(orderValue: Money)
    fun openBankingTimeout(currency: FiatCurrency)
    fun approvalError()
    fun openBankingError()
    fun openBankingError(currency: FiatCurrency)
    fun launchOpenBankingLinking(bankLinkingInfo: BankLinkingInfo)
    fun paymentForCancelledOrder(currency: FiatCurrency)
    fun launchSimpleBuyFromLinkApproval()
}

package com.blockchain.fiatActions.fiatactions.models

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullFiatAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.nabu.BlockedReason
import info.blockchain.balance.FiatCurrency

sealed interface FiatActionsResult : NavigationEvent {
    val account: FiatAccount
    val action: AssetAction

    data class TransactionFlow(
        override val account: FiatAccount = NullFiatAccount,
        override val action: AssetAction,
        val target: TransactionTarget = NullFiatAccount
    ) : FiatActionsResult

    data class WireTransferAccountDetails(
        override val account: FiatAccount,
        val accountIsFunded: Boolean,
        override val action: AssetAction
    ) : FiatActionsResult

    data class LaunchQuestionnaire(
        override val account: FiatAccount,
        override val action: AssetAction,
        val questionnaire: Questionnaire
    ) : FiatActionsResult

    data class BlockedDueToSanctions(
        override val account: FiatAccount,
        override val action: AssetAction,
        val reason: BlockedReason.Sanctions
    ) : FiatActionsResult

    data class LinkBankMethod(
        override val account: FiatAccount,
        override val action: AssetAction,
        val paymentMethodsForAction: LinkablePaymentMethodsForAction
    ) : FiatActionsResult

    /**
     * for argentinian withdraw
     */
    data class LinkBankWithAlias(
        override val account: FiatAccount,
        override val action: AssetAction
    ) : FiatActionsResult

    /**
     * opens external link bank cache
     */
    data class BankLinkFlow(
        override val account: FiatAccount,
        override val action: AssetAction,
        val linkBankTransfer: LinkBankTransfer
    ) : FiatActionsResult

    data class KycDepositCashBenefits(
        val currency: FiatCurrency
    ) : FiatActionsResult {
        override val account: FiatAccount
            get() = NullFiatAccount
        override val action: AssetAction
            get() = AssetAction.FiatDeposit
    }
}

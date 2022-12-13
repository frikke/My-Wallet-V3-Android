package com.blockchain.tempsheetinterfaces.fiatactions.models

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullFiatAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.nabu.BlockedReason

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
        override val action: AssetAction
    ) : FiatActionsResult

    data class DepositQuestionnaire(
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
     * opens external link bank
     */
    data class BankLinkFlow(
        override val account: FiatAccount,
        override val action: AssetAction,
        val linkBankTransfer: LinkBankTransfer,
    ) : FiatActionsResult
}

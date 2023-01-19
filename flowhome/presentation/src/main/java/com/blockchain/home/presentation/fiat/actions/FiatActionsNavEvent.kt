package com.blockchain.home.presentation.fiat.actions

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.nabu.BlockedReason
import info.blockchain.balance.FiatCurrency

sealed interface FiatActionsNavEvent {
    data class TransactionFlow(
        val account: FiatAccount,
        val target: TransactionTarget,
        val action: AssetAction
    ) : FiatActionsNavEvent

    data class WireTransferAccountDetails(
        val account: FiatAccount
    ) : FiatActionsNavEvent

    data class DepositQuestionnaire(
        val questionnaire: Questionnaire
    ) : FiatActionsNavEvent

    data class BlockedDueToSanctions(
        val reason: BlockedReason.Sanctions
    ) : FiatActionsNavEvent

    data class LinkBankMethod(
        val paymentMethodsForAction: LinkablePaymentMethodsForAction
    ) : FiatActionsNavEvent

    data class BankLinkFlow(
        val linkBankTransfer: LinkBankTransfer,
        val account: FiatAccount,
        val action: AssetAction
    ) : FiatActionsNavEvent

    data class LinkBankWithAlias(
        val account: FiatAccount,
        val action: AssetAction
    ) : FiatActionsNavEvent

    data class KycCashBenefits(
        val currency: FiatCurrency
    ) : FiatActionsNavEvent
}

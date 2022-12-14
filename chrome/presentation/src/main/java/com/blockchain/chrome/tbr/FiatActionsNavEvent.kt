package com.blockchain.chrome.tbr

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.nabu.BlockedReason

sealed interface FiatActionsNavEvent : NavigationEvent {
    data class TransactionFlow(
        val sourceAccount: BlockchainAccount = NullCryptoAccount(),
        val target: TransactionTarget = NullCryptoAccount(),
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
        val fiatAccount: FiatAccount,
        val assetAction: AssetAction
    ) : FiatActionsNavEvent
}

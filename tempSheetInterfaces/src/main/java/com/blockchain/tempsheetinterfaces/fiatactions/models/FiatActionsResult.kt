package com.blockchain.tempsheetinterfaces.fiatactions.models

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.nabu.BlockedReason

sealed interface FiatActionsResult : NavigationEvent {
    data class TransactionFlow(
        val sourceAccount: BlockchainAccount = NullCryptoAccount(),
        val target: TransactionTarget = NullCryptoAccount(),
        val action: AssetAction
    ) : FiatActionsResult

    data class WireTransferAccountDetails(
        val account: FiatAccount
    ) : FiatActionsResult

    data class DepositQuestionnaire(
        val questionnaire: Questionnaire
    ) : FiatActionsResult

    data class BlockedDueToSanctions(
        val reason: BlockedReason.Sanctions
    ) : FiatActionsResult

    data class LinkBankMethod(
        val paymentMethodsForAction: LinkablePaymentMethodsForAction
    ) : FiatActionsResult
}

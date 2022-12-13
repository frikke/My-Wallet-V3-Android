package com.blockchain.home.presentation.fiat.actions

import androidx.compose.runtime.Stable
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.home.presentation.fiat.actions.models.LinkablePaymentMethodsForAction
import com.blockchain.nabu.BlockedReason

@Stable
interface FiatActionsNavigation {
    fun wireTransferDetail(
        account: FiatAccount
    )

    fun depositQuestionnaire(
        questionnaire: Questionnaire
    )

    fun transactionFlow(
        sourceAccount: BlockchainAccount = NullCryptoAccount(),
        target: TransactionTarget = NullCryptoAccount(),
        action: AssetAction
    )

    fun blockedDueToSanctions(
        reason: BlockedReason.Sanctions
    )

    fun linkBankMethod(
        paymentMethodsForAction: LinkablePaymentMethodsForAction
    )
}

package com.blockchain.fiatActions.fiatactions

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Stable
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
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
        account: FiatAccount,
        target: TransactionTarget,
        action: AssetAction
    )

    fun blockedDueToSanctions(
        reason: BlockedReason.Sanctions
    )

    fun linkBankMethod(
        paymentMethodsForAction: LinkablePaymentMethodsForAction
    )

    fun bankLinkFlow(
        launcher: ActivityResultLauncher<Intent>,
        linkBankTransfer: LinkBankTransfer,
        fiatAccount: FiatAccount,
        assetAction: AssetAction
    )

    fun bankLinkWithAlias(
        launcher: ActivityResultLauncher<Intent>,
        fiatAccount: FiatAccount
    )
}

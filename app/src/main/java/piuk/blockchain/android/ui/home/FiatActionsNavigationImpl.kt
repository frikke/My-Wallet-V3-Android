package piuk.blockchain.android.ui.home

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.nabu.BlockedReason
import com.blockchain.tempsheetinterfaces.fiatactions.FiatActionsNavigation
import com.blockchain.tempsheetinterfaces.fiatactions.models.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankMethodChooserBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class FiatActionsNavigationImpl(private val activity: BlockchainActivity?) : FiatActionsNavigation {
    override fun wireTransferDetail(account: FiatAccount) {
        activity?.showBottomSheet(
            WireTransferAccountDetailsBottomSheet.newInstance(account)
        )
    }

    override fun depositQuestionnaire(questionnaire: Questionnaire) {
        activity?.showBottomSheet(
            QuestionnaireSheet.newInstance(questionnaire, true)
        )
    }

    override fun transactionFlow(
        sourceAccount: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ) {
        activity?.startActivity(
            TransactionFlowActivity.newIntent(
                context = activity,
                sourceAccount = sourceAccount,
                target = target,
                action = action
            )
        )
    }

    override fun blockedDueToSanctions(reason: BlockedReason.Sanctions) {
        activity?.showBottomSheet(
            BlockedDueToSanctionsSheet.newInstance(reason)
        )
    }

    override fun linkBankMethod(
        paymentMethodsForAction: LinkablePaymentMethodsForAction
    ) {
        activity?.showBottomSheet(
            LinkBankMethodChooserBottomSheet.newInstance(paymentMethodsForAction)
        )
    }
}

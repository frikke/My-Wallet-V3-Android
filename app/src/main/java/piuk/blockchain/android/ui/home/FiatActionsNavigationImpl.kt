package piuk.blockchain.android.ui.home

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.fiatActions.fiatactions.FiatActionsNavigation
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.nabu.BlockedReason
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankMethodChooserBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.alias.BankAliasLinkActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class FiatActionsNavigationImpl(
    private val activity: BlockchainActivity?,
) : FiatActionsNavigation {
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
        account: FiatAccount,
        target: TransactionTarget,
        action: AssetAction
    ) {
        activity?.startActivity(
            TransactionFlowActivity.newIntent(
                context = activity,
                sourceAccount = account,
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

    override fun bankLinkFlow(
        launcher: ActivityResultLauncher<Intent>,
        linkBankTransfer: LinkBankTransfer,
        fiatAccount: FiatAccount,
        assetAction: AssetAction
    ) {
        activity?.let {
            launcher.launch(
                BankAuthActivity.newInstance(
                    linkBankTransfer,
                    when (assetAction) {
                        AssetAction.FiatDeposit -> {
                            BankAuthSource.DEPOSIT
                        }
                        AssetAction.FiatWithdraw -> {
                            BankAuthSource.WITHDRAW
                        }
                        else -> {
                            throw IllegalStateException("Attempting to link from an unsupported action")
                        }
                    },
                    activity
                )
            )
        }
    }

    override fun bankLinkWithAlias(
        launcher: ActivityResultLauncher<Intent>,
        fiatAccount: FiatAccount
    ) {
        activity?.let {
            launcher.launch(
                BankAliasLinkActivity.newInstance(
                    currency = fiatAccount.currency.networkTicker,
                    context = activity
                )
            )
        }
    }
}

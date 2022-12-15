package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.nabu.BlockedReason

sealed class FiatTransactionRequestResult {
    class LaunchBankLink(
        val linkBankTransfer: LinkBankTransfer,
        val action: AssetAction
    ) : FiatTransactionRequestResult()
    class LaunchDepositFlow(
        val preselectedBankAccount: LinkedBankAccount,
        val action: AssetAction,
        val targetAccount: TransactionTarget
    ) : FiatTransactionRequestResult()
    class LaunchPaymentMethodChooser(val paymentMethodForAction: LinkablePaymentMethodsForAction) :
        FiatTransactionRequestResult()

    class LaunchDepositDetailsSheet(val targetAccount: FiatAccount) : FiatTransactionRequestResult()
    data class LaunchDepositFlowWithMultipleAccounts(
        val action: AssetAction,
        val targetAccount: TransactionTarget
    ) : FiatTransactionRequestResult()
    class LaunchWithdrawalFlow(
        val preselectedBankAccount: LinkedBankAccount,
        val action: AssetAction,
        val sourceAccount: FiatAccount
    ) : FiatTransactionRequestResult()
    data class LaunchWithdrawalFlowWithMultipleAccounts(
        val action: AssetAction,
        val sourceAccount: FiatAccount
    ) : FiatTransactionRequestResult()
    data class LaunchAliasWithdrawal(val targetAccount: FiatAccount) : FiatTransactionRequestResult()
    object NotSupportedPartner : FiatTransactionRequestResult()
    data class BlockedDueToSanctions(val reason: BlockedReason.Sanctions) : FiatTransactionRequestResult()
    data class LaunchQuestionnaire(
        val questionnaire: Questionnaire,
        val callbackIntent: DashboardIntent.LaunchBankTransferFlow
    ) : FiatTransactionRequestResult()
}

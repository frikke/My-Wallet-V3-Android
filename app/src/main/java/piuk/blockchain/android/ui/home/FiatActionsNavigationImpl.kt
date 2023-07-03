package piuk.blockchain.android.ui.home

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.blockchain.api.NabuApiException
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullFiatAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.fiatActions.fiatactions.FiatActionsNavigation
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethodsForAction
import com.blockchain.nabu.BlockedReason
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ACTION_DEPOSIT
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.customviews.BlockedDueToSanctionsSheet
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankMethodChooserBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.WireTransferAccountDetailsBottomSheet
import piuk.blockchain.android.ui.dataremediation.QuestionnaireSheet
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.alias.BankAliasLinkActivity
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity

class FiatActionsNavigationImpl(
    private val activity: BlockchainActivity?
) : FiatActionsNavigation {
    override fun wireTransferDetail(account: FiatAccount, accountIsFunded: Boolean) {
        activity?.showBottomSheet(
            WireTransferAccountDetailsBottomSheet.newInstance(account, accountIsFunded)
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
                sourceAccount = account.takeIf { it !is NullFiatAccount },
                target = target.takeIf { it !is NullFiatAccount },
                action = action,
                origin = "FiatActionsNav",
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

    override fun kycCashBenefits(currency: FiatCurrency) {
        activity?.let {
            with(it) {
                showBottomSheet(
                    KycBenefitsBottomSheet.newInstance(
                        KycBenefitsBottomSheet.BenefitsDetails(
                            title = getString(
                                com.blockchain.stringResources.R.string.fiat_funds_no_kyc_announcement_title
                            ),
                            description = getString(
                                com.blockchain.stringResources.R.string.fiat_funds_no_kyc_announcement_description
                            ),
                            listOfBenefits = listOf(
                                VerifyIdentityNumericBenefitItem(
                                    getString(com.blockchain.stringResources.R.string.fiat_funds_no_kyc_step_1_title),
                                    getString(
                                        com.blockchain.stringResources.R.string.fiat_funds_no_kyc_step_1_description
                                    )
                                ),
                                VerifyIdentityNumericBenefitItem(
                                    getString(com.blockchain.stringResources.R.string.fiat_funds_no_kyc_step_2_title),
                                    getString(
                                        com.blockchain.stringResources.R.string.fiat_funds_no_kyc_step_2_description
                                    )
                                ),
                                VerifyIdentityNumericBenefitItem(
                                    getString(com.blockchain.stringResources.R.string.fiat_funds_no_kyc_step_3_title),
                                    getString(
                                        com.blockchain.stringResources.R.string.fiat_funds_no_kyc_step_3_description
                                    )
                                )
                            ),
                            icon = currency.logo
                        )
                    )
                )
            }
        }
    }

    override fun failure(action: AssetAction, error: Exception) {
        val serverSideErrorInfo = (error as? NabuApiException)?.getServerSideErrorInfo()
        val sheet = if (serverSideErrorInfo != null) {
            ErrorSlidingBottomDialog.newInstance(serverSideErrorInfo)
        } else {
            ErrorSlidingBottomDialog.newInstance(
                ErrorDialogData(
                    title = activity?.getString(com.blockchain.stringResources.R.string.common_error).orEmpty(),
                    description = (error as? NabuApiException)?.getErrorDescription()
                        ?: error.localizedMessage.orEmpty(),
                    errorButtonCopies = ErrorButtonCopies(
                        primaryButtonText = activity
                            ?.getString(com.blockchain.stringResources.R.string.common_ok).orEmpty()
                    ),
                    nabuApiException = error as? NabuApiException,
                    errorDescription = (error as? NabuApiException)?.getErrorDescription(),
                    action = ACTION_DEPOSIT,
                    analyticsCategories = emptyList(),
                )
            )
        }

        activity?.showBottomSheet(sheet)
    }
}

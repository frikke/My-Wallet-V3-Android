package piuk.blockchain.android.ui.linkbank

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.domain.paymentmethods.model.BankAuthError
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.BankLinkingProcessState
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.RefreshBankInfo
import piuk.blockchain.android.simplebuy.SelectedPaymentMethod

sealed class BankAuthIntent : MviIntent<BankAuthState> {

    object ProviderAccountIdUpdateError : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState = oldState.copy(
            errorState = BankAuthError.BankLinkingUpdateFailed,
            bankLinkingProcessState = BankLinkingProcessState.NONE
        )
    }

    class LinkedBankStateSuccess(private val linkedBank: LinkedBank) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING_SUCCESS,
                linkedBank = linkedBank,
                errorState = null,
                selectedPaymentMethod = SelectedPaymentMethod(
                    id = linkedBank.id,
                    paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                    label = linkedBank.accountName,
                    isEligible = true
                )
            )

        override fun isValidFor(oldState: BankAuthState): Boolean = true
    }

    class BankAuthErrorState(val state: BankAuthError) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                errorState = state,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
    }

    class GetLinkedBankState(val linkingBankId: String, private val isFromDeepLink: Boolean = false) :
        BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                bankLinkingProcessState = if (isFromDeepLink) {
                    BankLinkingProcessState.ACTIVATING
                } else {
                    BankLinkingProcessState.IN_EXTERNAL_FLOW
                }
            )
    }

    class UpdateAccountProvider(
        val accountProviderId: String,
        val accountId: String,
        val linkingBankId: String,
        val linkBankTransfer: LinkBankTransfer,
        val authSource: BankAuthSource
    ) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState = oldState.copy(
            linkBankTransfer = linkBankTransfer,
            bankLinkingProcessState = BankLinkingProcessState.LINKING
        )
    }

    class LinkPlaidAccount(
        val accountId: String,
        val linkBankAccountId: String,
        val linkBankToken: String,
        val linkBankTransfer: LinkBankTransfer,
        val authSource: BankAuthSource
    ) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState = oldState.copy(
            linkBankTransfer = linkBankTransfer,
            id = accountId,
            linkBankToken = linkBankToken,
            linkBankAccountId = linkBankAccountId,
            bankLinkingProcessState = BankLinkingProcessState.LINKING
        )
    }

    class RefreshPlaidAccount(val refreshBankAccountId: String?) :
        BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                refreshBankAccountId = refreshBankAccountId,
                bankLinkingProcessState = BankLinkingProcessState.LINKING
            )
    }

    class PlaidAccountRefreshInfoReceived(private val refreshBankInfo: RefreshBankInfo) :
        BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                refreshBankInfo = refreshBankInfo,
                bankLinkingProcessState = BankLinkingProcessState.IN_REFRESH_FLOW
            )
    }

    object ClearBankLinkingUrl : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkBankUrl = null,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
    }

    object StartBankLinking : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                bankLinkingProcessState = BankLinkingProcessState.IN_EXTERNAL_FLOW
            )
    }

    class StartBankApproval(val callbackPath: String) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                bankLinkingProcessState = BankLinkingProcessState.APPROVAL_WAIT
            )
    }

    object ResetBankLinking : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkBankUrl = null,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
    }

    data class StartPollingForLinkStatus(val bankId: String) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(bankLinkingProcessState = BankLinkingProcessState.LINKING)
    }

    data class UpdateLinkingUrl(val authUrl: String) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkBankUrl = authUrl
            )
    }

    data class UpdateForApproval(val authorisationUrl: String, val callbackPath: String) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkBankUrl = authorisationUrl,
                callbackPathUrl = callbackPath,
                bankLinkingProcessState = BankLinkingProcessState.APPROVAL
            )
    }

    object ClearApprovalState : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkedBank = null,
                callbackPathUrl = ""
            )
    }

    object CancelOrder : BankAuthIntent() {
        override fun isValidFor(oldState: BankAuthState) = true
        override fun reduce(oldState: BankAuthState): BankAuthState = oldState
    }

    object CancelOrderAndResetAuthorisation : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                callbackPathUrl = "",
                linkedBank = null
            )
    }

    object OrderCanceled : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            BankAuthState(bankLinkingProcessState = BankLinkingProcessState.CANCELED)
    }

    class ErrorIntent(private val error: BankAuthError = BankAuthError.GenericError) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(errorState = error, bankLinkingProcessState = BankLinkingProcessState.NONE)

        override fun isValidFor(oldState: BankAuthState): Boolean = true
    }
}

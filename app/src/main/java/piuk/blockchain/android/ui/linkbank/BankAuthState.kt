package piuk.blockchain.android.ui.linkbank

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.payments.model.LinkBankTransfer
import com.blockchain.core.payments.model.LinkedBank
import com.google.gson.Gson
import java.io.Serializable
import piuk.blockchain.android.simplebuy.SelectedPaymentMethod

data class BankAuthState(
    val id: String? = null,
    val linkedBank: LinkedBank? = null,
    val linkBankTransfer: LinkBankTransfer? = null,
    val linkBankUrl: String? = null,
    val bankLinkingProcessState: BankLinkingProcessState = BankLinkingProcessState.NONE,
    val errorState: BankAuthError? = null,
    val selectedPaymentMethod: SelectedPaymentMethod? = null,
    val callbackPathUrl: String = ""
) : MviState

enum class BankLinkingProcessState {
    LINKING,
    IN_EXTERNAL_FLOW,
    APPROVAL,
    APPROVAL_WAIT,
    ACTIVATING,
    LINKING_SUCCESS,
    CANCELED,
    NONE
}

enum class BankAuthError {
    BankLinkingUpdateFailed,
    BankLinkingFailed,
    LinkedBankAlreadyLinked,
    BankLinkingTimeout,
    LinkedBankInfoNotFound,
    LinkedBankAccountUnsupported,
    GenericError,
    LinkedBankNamesMismatched,
    LinkedBankRejected,
    LinkedBankExpired,
    LinkedBankFailure,
    LinkedBankInternalFailure,
    LinkedBankInvalid,
    LinkedBankFraud
}

data class BankLinkingInfo(
    val linkingId: String,
    val bankAuthSource: BankAuthSource
) : Serializable

enum class BankAuthSource {
    SIMPLE_BUY,
    SETTINGS,
    DEPOSIT,
    WITHDRAW
}

data class BankAuthDeepLinkState(
    val bankAuthFlow: BankAuthFlowState = BankAuthFlowState.NONE,
    val bankPaymentData: BankPaymentApproval? = null,
    val bankLinkingInfo: BankLinkingInfo? = null
)

fun BankAuthDeepLinkState.toPreferencesValue(): String =
    Gson().toJson(this, BankAuthDeepLinkState::class.java)

internal fun String.fromPreferencesValue(): BankAuthDeepLinkState? =
    if (this.isNotEmpty()) {
        Gson().fromJson(this, BankAuthDeepLinkState::class.java)
    } else {
        null
    }

enum class BankAuthFlowState {
    BANK_LINK_PENDING,
    BANK_LINK_COMPLETE,
    BANK_APPROVAL_PENDING,
    BANK_APPROVAL_COMPLETE,
    NONE
}

enum class FiatTransactionState {
    SUCCESS,
    ERROR,
    PENDING
}

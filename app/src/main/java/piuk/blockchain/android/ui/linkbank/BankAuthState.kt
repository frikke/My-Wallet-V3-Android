package piuk.blockchain.android.ui.linkbank

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.google.gson.Gson
import java.io.Serializable
import piuk.blockchain.android.simplebuy.SelectedPaymentMethod

data class BankAuthState(
    val id: String? = null,
    val linkedBank: LinkedBank? = null,
    val linkBankTransfer: LinkBankTransfer? = null,
    val linkBankUrl: String? = null,
    val linkBankAccountId: String? = null,
    val linkBankToken: String? = null,
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

sealed class BankAuthError : Serializable {
    object BankLinkingUpdateFailed : BankAuthError()
    object BankLinkingFailed : BankAuthError()
    object LinkedBankAlreadyLinked : BankAuthError()
    object BankLinkingTimeout : BankAuthError()
    object LinkedBankInfoNotFound : BankAuthError()
    object LinkedBankAccountUnsupported : BankAuthError()
    object GenericError : BankAuthError()
    object LinkedBankNamesMismatched : BankAuthError()
    object LinkedBankRejected : BankAuthError()
    object LinkedBankExpired : BankAuthError()
    object LinkedBankFailure : BankAuthError()
    object LinkedBankInternalFailure : BankAuthError()
    object LinkedBankInvalid : BankAuthError()
    object LinkedBankFraud : BankAuthError()
    class ServerSideDrivenLinkedBankError(
        val title: String,
        val message: String,
        val iconUrl: String,
        val statusIconUrl: String
    ) : BankAuthError()
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

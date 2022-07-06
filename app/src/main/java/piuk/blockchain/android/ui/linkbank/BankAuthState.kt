package piuk.blockchain.android.ui.linkbank

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.RefreshBankInfo
import java.io.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
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
    val callbackPathUrl: String = "",
    val refreshBankAccountId: String? = null,
    val refreshBankInfo: RefreshBankInfo? = null
) : MviState

enum class BankLinkingProcessState {
    LINKING,
    IN_EXTERNAL_FLOW,
    IN_REFRESH_FLOW,
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

@kotlinx.serialization.Serializable
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

@kotlinx.serialization.Serializable
data class BankAuthDeepLinkState(
    val bankAuthFlow: BankAuthFlowState = BankAuthFlowState.NONE,
    val bankPaymentData: BankPaymentApproval? = null,
    val bankLinkingInfo: BankLinkingInfo? = null
)

fun BankAuthDeepLinkState.toPreferencesValue(): String {
    val koin = GlobalContext.get()
    val json by koin.inject<Json>()

    return json.encodeToString(this)
}

internal fun String.fromPreferencesValue(): BankAuthDeepLinkState? {
    val koin = GlobalContext.get()

    return if (this.isNotEmpty()) {
        val json by koin.inject<Json>()
        json.decodeFromString<BankAuthDeepLinkState>(this)
    } else {
        null
    }
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

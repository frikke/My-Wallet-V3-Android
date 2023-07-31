package com.blockchain.domain.paymentmethods.model

import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import java.io.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext

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

fun String.fromPreferencesValue(): BankAuthDeepLinkState? {
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

enum class BankTransferAction {
    LINK, PAY
}

interface BankPartnerCallbackProvider {
    fun callback(partner: BankPartner, action: BankTransferAction): String
}

@kotlinx.serialization.Serializable
data class BankPaymentApproval(
    val paymentId: String,
    val authorisationUrl: String,
    val linkedBank: LinkedBank,
    val orderValue: FiatValue
) : Serializable

sealed class BankBuyAuthStep {
    object BuyWithBankApproved : BankBuyAuthStep()
    object BuyWithBankError : BankBuyAuthStep()
    class BankAuthForCancelledOrder(val fiatCurrency: FiatCurrency) : BankBuyAuthStep()
}

interface BankBuyNavigation {
    fun step(): Single<BankBuyAuthStep>
}

const val LINKED_BANK_ID_KEY = "LINKED_BANK_ID"

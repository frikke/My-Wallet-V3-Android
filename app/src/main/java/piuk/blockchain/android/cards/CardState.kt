package piuk.blockchain.android.cards

import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.braintreepayments.cardform.utils.CardType
import info.blockchain.balance.FiatCurrency
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import piuk.blockchain.android.R

@Serializable
data class CardState(
    val fiatCurrency: FiatCurrency,
    val cardId: String? = null,
    val cardStatus: CardStatus? = null,
    val billingAddress: BillingAddress? = null,
    val addCard: Boolean = false,
    val linkedCards: List<@Contextual LinkedPaymentMethod.Card>? = null,
    @Transient
    val authoriseCard: CardAcquirerCredentials? = null,
    @Transient
    val cardRequestStatus: CardRequestStatus? = null,
    val cardRejectionState: @Contextual CardRejectionState? = null,
    val usStateList: List<@Contextual Region.State>? = null,
) : MviState

@Serializable
sealed class CardAcquirerCredentials {
    // This used to be EverypayAuthOptions
    data class Everypay(val paymentLink: String, val exitLink: String) : CardAcquirerCredentials()

    data class Stripe(val apiKey: String, val clientSecret: String) : CardAcquirerCredentials()

    data class Checkout(
        val apiKey: String,
        val paymentLink: String,
        val exitLink: String
    ) : CardAcquirerCredentials()
}

@Serializable
sealed class CardRequestStatus {
    class Error(val type: CardError) : CardRequestStatus()
    object Loading : CardRequestStatus()
    class Success(val card: PaymentMethod.Card) : CardRequestStatus()
}

fun CardType.icon() =
    when (this) {
        CardType.VISA -> R.drawable.stripe_ic_visa
        CardType.MASTERCARD -> R.drawable.stripe_ic_mastercard
        else -> this.frontResource
    }

sealed class CardError {
    object CreationFailed : CardError()
    object ActivationFailed : CardError()
    object PendingAfterPoll : CardError()
    object LinkFailed : CardError()
    object InsufficientCardBalance : CardError()
    object CardBankDeclined : CardError()
    object CardDuplicated : CardError()
    object CardBlockchainDeclined : CardError()
    object CardAcquirerDeclined : CardError()
    object CardPaymentNotSupportedDeclined : CardError()
    object CardCreatedFailed : CardError()
    object CardPaymentFailed : CardError()
    object CardCreatedAbandoned : CardError()
    object CardCreatedExpired : CardError()
    object CardCreateBankDeclined : CardError()
    object CardCreateDebitOnly : CardError()
    object CardPaymentDebitOnly : CardError()
    object CardCreateNoToken : CardError()
    object CardLimitReach : CardError()
    class ServerSideCardError(
        val title: String,
        val message: String,
        val iconUrl: String,
        val statusIconUrl: String,
        val actions: List<ServerErrorAction>,
        val categories: List<String>,
        val errorId: String?
    ) : CardError()
}

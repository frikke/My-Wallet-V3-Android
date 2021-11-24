package piuk.blockchain.android.cards

import com.blockchain.nabu.datamanagers.BillingAddress
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.braintreepayments.cardform.utils.CardType
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviState

data class CardState(
    val fiatCurrency: String,
    val cardId: String? = null,
    val cardStatus: CardStatus? = null,
    val billingAddress: BillingAddress? = null,
    val addCard: Boolean = false,
    @Transient val authoriseCard: CardAcquirerCredentials? = null,
    @Transient val cardRequestStatus: CardRequestStatus? = null
) : MviState

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

sealed class CardRequestStatus {
    class Error(val type: CardError) : CardRequestStatus()
    object Loading : CardRequestStatus()
    class Success(val card: PaymentMethod.Card) : CardRequestStatus()
}

fun CardType.icon() =
    when (this) {
        CardType.VISA -> R.drawable.ic_visa
        CardType.MASTERCARD -> R.drawable.ic_mastercard
        else -> this.frontResource
    }

enum class CardError {
    CREATION_FAILED, ACTIVATION_FAIL, PENDING_AFTER_POLL, LINK_FAILED
}

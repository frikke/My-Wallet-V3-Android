package piuk.blockchain.android.cards.partners

import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.cards.CardData
import piuk.blockchain.android.everypay.models.CcDetails

interface CardActivator {
    fun activateCard(cardData: CardData, cardId: String): Single<out CompleteCardActivation>
    fun paymentAttributes(): SimpleBuyConfirmationAttributes
    val unsupportedPartnerError: String
        get() = "Card partner not supported"
    val redirectUrl: String
        get() = "https://google.com"
}

sealed class CompleteCardActivation {
    data class EverypayCompleteCardActivationDetails(val paymentLink: String, val exitLink: String) :
        CompleteCardActivation()

    data class StripeCardActivationDetails(
        val apiKey: String,
        val clientSecret: String
    ) : CompleteCardActivation()

    data class CheckoutCardActivationDetails(
        val apiKey: String,
        val paymentLink: String,
        val exitLink: String
    ) : CompleteCardActivation()
}

fun CardData.toCcDetails() =
    CcDetails(
        number = number,
        month = month.toString(),
        year = year.toString(),
        cvc = cvv,
        holderName = fullName
    )

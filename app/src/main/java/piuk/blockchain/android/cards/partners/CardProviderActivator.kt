package piuk.blockchain.android.cards.partners

import com.blockchain.nabu.datamanagers.CardProvider
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.EveryPayCredentials
import com.blockchain.nabu.datamanagers.PartnerCredentials
import com.blockchain.nabu.models.responses.simplebuy.EveryPayAttrs
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyConfirmationAttributes
import com.blockchain.payments.core.CardAcquirer
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.cards.CardData
import piuk.blockchain.android.everypay.service.EveryPayCardService

class CardProviderActivator(
    private val custodialWalletManager: CustodialWalletManager,
    private val submitEveryPayCardService: EveryPayCardService
) : CardActivator {

    override fun activateCard(
        cardData: CardData,
        cardId: String
    ): Single<CompleteCardActivation> {
        return custodialWalletManager.activateCard(
            cardId = cardId,
            attributes = SimpleBuyConfirmationAttributes(
                everypay = EveryPayAttrs(redirectUrl),
                redirectURL = redirectUrl
            )
        ).flatMap { credentials ->
            when (credentials) {
                is PartnerCredentials.EverypayPartner -> getEveryPayActivationDetails(credentials.everyPay, cardData)
                is PartnerCredentials.CardProviderPartner ->
                    getCardActivationDetails(credentials.cardProvider, cardData)
                is PartnerCredentials.Unknown -> Single.error(Throwable(unsupportedPartnerError))
            }
        }
    }

    private fun getCardActivationDetails(
        cardProvider: CardProvider,
        cardData: CardData
    ): Single<CompleteCardActivation> =
        when (CardAcquirer.fromString(cardProvider.cardAcquirerName)) {
            CardAcquirer.CHECKOUT -> Single.just(
                CompleteCardActivation.CheckoutCardActivationDetails(
                    apiKey = cardProvider.publishableKey,
                    paymentLink = cardProvider.paymentLink,
                    exitLink = CHECKOUT_EXIT_LINK
                )
            )
            CardAcquirer.STRIPE -> Single.just(
                CompleteCardActivation.StripeCardActivationDetails(
                    apiKey = cardProvider.publishableKey,
                    clientSecret = cardProvider.clientSecret
                )
            )
            // The backend will start returning Everypay as a CardProvider instead of Everypay from the Partner enum
            CardAcquirer.EVERYPAY ->
                getEveryPayActivationDetails(
                    everyPay = EveryPayCredentials(
                        apiUsername = cardProvider.apiUserID,
                        mobileToken = cardProvider.apiToken,
                        paymentLink = cardProvider.paymentLink
                    ),
                    cardData = cardData
                )
            else -> Single.error(
                Throwable(unsupportedPartnerError)
            )
        }

    private fun getEveryPayActivationDetails(
        everyPay: EveryPayCredentials,
        cardData: CardData
    ): Single<CompleteCardActivation> =
        submitEveryPayCardService.submitCard(
            cardData.toCcDetails(),
            everyPay.apiUsername,
            everyPay.mobileToken
        ).map {
            CompleteCardActivation.EverypayCompleteCardActivationDetails(
                paymentLink = everyPay.paymentLink,
                exitLink = redirectUrl
            )
        }

    override fun paymentAttributes(): SimpleBuyConfirmationAttributes =
        SimpleBuyConfirmationAttributes(everypay = EveryPayAttrs(redirectUrl), redirectURL = redirectUrl)

    companion object {
        private const val CHECKOUT_EXIT_LINK = "http://example.com/payments"
    }
}

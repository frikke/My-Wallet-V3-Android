package com.blockchain.payments.stripe

import com.blockchain.outcome.Outcome
import com.blockchain.payments.core.CardBillingAddress
import com.blockchain.payments.core.CardAcquirer
import com.blockchain.payments.core.CardProcessor
import com.blockchain.payments.core.CardDetails
import com.blockchain.payments.core.CardProcessingFailure
import com.blockchain.payments.core.PaymentToken
import com.stripe.android.createPaymentMethod
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.Address
import com.stripe.android.model.CardParams
import com.stripe.android.model.PaymentMethodCreateParams

class StripeCardProcessor(
    private val stripeFactory: StripeFactory
) : CardProcessor {

    override val acquirer: CardAcquirer = CardAcquirer.STRIPE

    // Create a payment method and return the payment token for the given card.
    // The backend wants the clients to set the address on the token for Stripe.
    override suspend fun createPaymentMethod(
        cardDetails: CardDetails,
        billingAddress: CardBillingAddress?,
        apiKey: String
    ): Outcome<CardProcessingFailure, PaymentToken> {
        return try {
            Outcome.Success(
            stripeFactory.getOrCreate(apiKey).createPaymentMethod(
                    paymentMethodCreateParams = PaymentMethodCreateParams.createCard(
                        createStripeCardParams(
                            cardDetails = cardDetails,
                            billingAddress = billingAddress
                        )
                    ),
                    idempotencyKey = null,
                    stripeAccountId = null
                ).id ?: ""
            )
        } catch (ex: AuthenticationException) {
            // Failure to properly authenticate yourself (check your key
            Outcome.Failure(CardProcessingFailure.AuthError(ex))
        } catch (ex: InvalidRequestException) {
            // Your request has invalid parameters
            Outcome.Failure(CardProcessingFailure.InvalidRequestError(ex))
        } catch (ex: APIConnectionException) {
            // Failure to connect to Stripe's API
            Outcome.Failure(CardProcessingFailure.NetworkError(ex))
        } catch (ex: APIException) {
            // Any other type of problem (for instance, a temporary issue with Stripe's servers)
            Outcome.Failure(CardProcessingFailure.UnknownError(ex))
        }
    }

    private fun createStripeCardParams(cardDetails: CardDetails, billingAddress: CardBillingAddress?) =
        CardParams(
            number = cardDetails.number,
            expMonth = cardDetails.expMonth,
            expYear = cardDetails.expYear,
            cvc = cardDetails.cvc,
            name = cardDetails.fullName,
            address = billingAddress?.toStripeAddress()
        )

    private fun CardBillingAddress.toStripeAddress() =
        Address(
            city = city,
            country = country,
            line1 = addressLine1,
            line2 = addressLine2,
            postalCode = postalCode,
            state = state
        )
}
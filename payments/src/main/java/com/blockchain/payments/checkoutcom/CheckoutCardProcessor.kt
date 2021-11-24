package com.blockchain.payments.checkoutcom

import com.blockchain.outcome.Outcome
import com.blockchain.payments.core.CardAcquirer
import com.blockchain.payments.core.CardDetails
import com.blockchain.payments.core.CardProcessingFailure
import com.blockchain.payments.core.CardProcessor
import com.checkout.android_sdk.CheckoutAPIClient
import com.checkout.android_sdk.Request.CardTokenisationRequest
import com.checkout.android_sdk.Response.CardTokenisationFail
import com.checkout.android_sdk.Response.CardTokenisationResponse
import com.checkout.android_sdk.network.NetworkError
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CheckoutCardProcessor(
    private val checkoutFactory: CheckoutFactory
) : CardProcessor {

    override val acquirer: CardAcquirer = CardAcquirer.CHECKOUT

    override suspend fun createPaymentMethod(
        cardDetails: CardDetails,
        apiKey: String
    ): Outcome<CardProcessingFailure, String> {

        // CheckoutAPIClient only supports callbacks for obtaining results from its calls. To transform
        // callback-style into suspending functions we need to suspend the coroutine that's currently
        // running and obtain its continuation - this is done by wrapping the block into suspendCoroutine.
        // Then execute the methods which then return the result through callbacks. Finally resume the coroutine
        // using its continuation's resume function where we can pass the results.
        return suspendCoroutine { continuation ->

            val checkoutApiClient = checkoutFactory.getOrCreate(apiKey)

            checkoutApiClient.setTokenListener(
                object : CheckoutAPIClient.OnTokenGenerated {
                    override fun onTokenGenerated(response: CardTokenisationResponse) {
                        continuation.resume(
                            Outcome.Success(response.token)
                        )
                    }

                    override fun onError(error: CardTokenisationFail) {
                        continuation.resume(Outcome.Failure(error.toFailure()))
                    }

                    override fun onNetworkError(error: NetworkError) {
                        continuation.resume(Outcome.Failure(error.toFailure()))
                    }
                }
            )

            checkoutApiClient.generateToken(cardDetails.toCardTokenizationRequest())
        }
    }

    private fun CardDetails.toCardTokenizationRequest() =
        CardTokenisationRequest(
            number,
            fullName,
            expMonth.toString(),
            expYear.toString(),
            cvc
        )

    private fun CardTokenisationFail.toFailure(): CardProcessingFailure {
        return when (errorType) {
            INVALID_REQUEST_ERROR ->
                CardProcessingFailure.InvalidRequestError(
                    throwable = Throwable("$INVALID_REQUEST_ERROR: $errorCodes")
                )
            else -> CardProcessingFailure.UnknownError(
                throwable = Throwable("$errorType: $errorCodes")
            )
        }
    }

    private fun NetworkError.toFailure(): CardProcessingFailure {
        return when (message) {
            AUTH_REQUIRED_ERROR ->
                CardProcessingFailure.AuthError(
                    throwable = cause
                        ?: Throwable(message ?: UNKNOWN_NETWORK_ERROR)
                )
            else -> CardProcessingFailure.UnknownError(
                throwable = cause
                    ?: Throwable(message ?: UNKNOWN_NETWORK_ERROR)
            )
        }
    }

    companion object {
        // There isn't much mention of error codes in the official documentation
        private const val AUTH_REQUIRED_ERROR = "Unauthorised request (HttpStatus: 401)"
        private const val INVALID_REQUEST_ERROR = "request_invalid"
        private const val UNKNOWN_NETWORK_ERROR = "Unknown Network Error"
    }
}